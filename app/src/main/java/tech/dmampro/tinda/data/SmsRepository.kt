package tech.dmampro.tinda.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.data.local.ArchivedThread
import tech.dmampro.tinda.data.local.Conversation
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.data.local.ScheduledStatus
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.sms.MultipartStatus
import tech.dmampro.tinda.sms.PartOutcome
import tech.dmampro.tinda.sms.PartSentState
import tech.dmampro.tinda.ui.conversations.ConversationItem
import tech.dmampro.tinda.util.TindaLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accès aux SMS : le provider système est la source pour la liste,
 * Room est le miroir applicatif observé par l'UI (et indexé pour la
 * recherche plein texte du lot 2).
 *
 * Règle absolue : jamais de contenu ni de numéro dans les logs (AGENTS.md).
 * Les écritures dans le provider n'ont lieu que si Tinda est la default SMS app.
 */
@Singleton
class SmsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
    private val prefs: PrefsRepository,
) {
    private val refreshSignal = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    private val nameCache = mutableMapOf<String, String>()

    /**
     * Sérialise les transitions de statut liées aux accusés : lecture
     * des segments, agrégation et écriture doivent être atomiques
     * entre elles, sinon un calcul ancien (encore SENDING) peut
     * persister après un échec ou une livraison confirmée par un
     * callback concurrent. Le dépôt est un singleton : un seul verrou
     * global suffit. En complément, l'écriture DAO elle-même est
     * conditionnelle (jamais d'écrasement d'un statut terminal).
     */
    private val statusMutex = Mutex()

    /** Vrai si Tinda est l'application SMS par défaut du système. */
    fun isDefaultSmsApp(): Boolean =
        tech.dmampro.tinda.util.isDefaultSmsApp(context)

    /** Liste des conversations, rafraîchie à chaque [notifyChanged]. */
    fun observeConversations(): Flow<List<ConversationItem>> =
        refreshSignal
            .onStart { emit(Unit) }
            .map { loadConversations() }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    /** Messages d'un fil, observés depuis le miroir Room. */
    fun observeThreadMessages(threadId: Long): Flow<List<Message>> =
        db.messageDao().observeForThread(threadId)

    /** Signale un changement (réception, envoi, lecture) pour rafraîchir l'UI. */
    fun notifyChanged() {
        refreshSignal.tryEmit(Unit)
    }

    /**
     * Re-synchronise la liste des conversations depuis le provider, puis
     * remplit le miroir Room + l'index FTS (recherche plein texte, lot 2).
     * Le remplissage complet est limité (100 fils récents, 3000 messages par
     * fil) et relancé au plus une fois par jour, le reste étant tenu à jour
     * par les triggers FTS.
     */
    suspend fun syncConversations() {
        notifyChanged()
        backfillSearchIndex()
    }

    private suspend fun backfillSearchIndex() = withContext(Dispatchers.IO) {
        try {
            val last = prefs.getLastFtsBackfill()
            val fresh = System.currentTimeMillis() - last < BACKFILL_INTERVAL_MILLIS
            if (fresh && db.searchDao().ftsCount() > 0) return@withContext
            for (threadId in queryRecentThreadIds(BACKFILL_THREADS)) {
                db.messageDao().insertAllIgnore(queryThreadMessages(threadId, BACKFILL_PER_THREAD))
            }
            prefs.setLastFtsBackfill(System.currentTimeMillis())
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "remplissage FTS sans permission SMS")
        }
    }

    /** Archive ou désarchive un fil (F5). */
    suspend fun setThreadArchived(threadId: Long, archived: Boolean) =
        withContext(Dispatchers.IO) {
            if (archived) {
                val address = getThreadAddress(threadId)
                db.archiveDao().archive(
                    ArchivedThread(threadId, address, System.currentTimeMillis()),
                )
            } else {
                db.archiveDao().unarchive(threadId)
            }
            notifyChanged()
        }

    /** Fils archivés, pour l'écran des paramètres. */
    fun observeArchivedThreads(): Flow<List<ArchivedThread>> =
        db.archiveDao().observeArchived()

    /** Épingle/désépingle une conversation (maquette Stitch : "Pinned Threads"). */
    suspend fun setThreadPinned(threadId: Long, pinned: Boolean) =
        withContext(Dispatchers.IO) {
            if (pinned) {
                db.pinnedThreadDao().pin(
                    tech.dmampro.tinda.data.local.PinnedThread(
                        threadId,
                        System.currentTimeMillis(),
                    ),
                )
            } else {
                db.pinnedThreadDao().unpin(threadId)
            }
            notifyChanged()
        }

    /** Supprime un fil : provider système + miroir local (F5). */
    suspend fun deleteThread(threadId: Long) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
            )
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "deleteThread sans permission d'écriture SMS")
        }
        db.messageDao().deleteByThread(threadId)
        db.archiveDao().unarchive(threadId)
        notifyChanged()
    }

    /** Recharge le miroir Room d'un fil depuis le provider. */
    suspend fun syncThread(threadId: Long) = withContext(Dispatchers.IO) {
        try {
            val rows = queryThreadMessages(threadId)
            db.messageDao().insertAllIgnore(rows)
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "syncThread sans permission SMS")
        }
    }

    /** Adresse du premier message d'un fil (pour l'envoi). */
    suspend fun getThreadAddress(threadId: Long): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC LIMIT 1",
            )?.use { c ->
                if (c.moveToFirst()) return@withContext c.getString(0).orEmpty()
            }
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "getThreadAddress sans permission SMS")
        }
        ""
    }

    /** Nom d'affichage d'un numéro (contacts, avec cache mémoire). */
    suspend fun resolveDisplayName(address: String): String = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext address
        nameCache[address]?.let { return@withContext it }
        val name = try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address),
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: address
        } catch (e: SecurityException) {
            address
        }
        nameCache[address] = name
        name
    }

    /** Thread existant ou créé pour un numéro (pour une nouvelle conversation). */
    suspend fun getOrCreateThreadId(address: String): Long = withContext(Dispatchers.IO) {
        try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "getOrCreateThreadId sans permission SMS")
            -1L
        }
    }

    /**
     * Persiste un SMS reçu : écriture dans le provider (rôle default handler
     * requis) puis miroir Room. Retourne l'identifiant de fil, -1 si refusé.
     */
    suspend fun insertIncoming(
        address: String,
        body: String,
        dateMillis: Long,
        subscriptionId: Int,
    ): Long = withContext(Dispatchers.IO) {
        if (!isDefaultSmsApp()) {
            TindaLogger.w(TAG, "insertIncoming refusé : pas default SMS app")
            return@withContext -1L
        }
        val threadId = try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, dateMillis)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                if (subscriptionId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
            }
            val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            val providerId = uri?.lastPathSegment?.toLongOrNull()
            val tid = Telephony.Threads.getOrCreateThreadId(context, address)
            db.messageDao().insert(
                Message(
                    providerId = providerId,
                    threadId = tid,
                    address = address,
                    body = body,
                    date = dateMillis,
                    read = false,
                    incoming = true,
                    status = MessageStatus.RECEIVED,
                    subscriptionId = subscriptionId,
                ),
            )
            db.conversationDao().upsertAll(listOf(conversationEntity(tid, address, body, dateMillis, unread = 1)))
            // Un nouveau message désarchive le fil (comportement attendu).
            db.archiveDao().unarchive(tid)
            tid
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "insertIncoming sans permission d'écriture SMS")
            -1L
        }
        notifyChanged()
        threadId
    }

    /** Marque un fil comme lu (provider + miroir). */
    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI, values,
                "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
            )
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "markThreadRead sans permission d'écriture SMS")
        }
        db.messageDao().markThreadRead(threadId)
        notifyChanged()
    }

    /** Met à jour le statut d'un message sortant (accusés d'envoi / livraison). */
    suspend fun updateMessageStatus(token: String, status: MessageStatus) =
        updateMessageStatus(token, status, null)

    /**
     * Met à jour le statut d'un message sortant, avec le code d'échec
     * éventuel (diagnostic visible dans l'app, jamais de contenu ni numéro).
     */
    suspend fun updateMessageStatus(token: String, status: MessageStatus, failureCode: Int?) =
        withContext(Dispatchers.IO) {
            // Écriture conditionnelle : un statut terminal déjà persisté
            // (DELIVERED ou FAILED) n'est jamais écrasé par un écrivain
            // concurrent porteur d'un calcul plus ancien.
            db.messageDao().updateStatusAndFailureByTokenIfNotTerminal(token, status, failureCode)
            // Best effort : reflète aussi le statut dans le provider système.
            try {
                val row = db.messageDao().findByToken(token)
                val providerId = row?.providerId
                if (providerId != null) {
                    val values = ContentValues().apply {
                        put(
                            Telephony.Sms.STATUS,
                            when (status) {
                                MessageStatus.DELIVERED, MessageStatus.SENT ->
                                    Telephony.Sms.STATUS_COMPLETE
                                MessageStatus.FAILED -> Telephony.Sms.STATUS_FAILED
                                else -> Telephony.Sms.STATUS_NONE
                            },
                        )
                    }
                    context.contentResolver.update(
                        Telephony.Sms.CONTENT_URI, values,
                        "${Telephony.Sms._ID} = ?", arrayOf(providerId.toString()),
                    )
                }
            } catch (e: SecurityException) {
                TindaLogger.w(TAG, "updateMessageStatus sans permission d'écriture SMS")
            }
            notifyChanged()
        }

    /**
     * Enregistre le verdict d'émission d'un segment (L3) puis recalcule
     * le statut du message par agrégation des segments. Un message créé
     * avant le suivi par segment n'a aucune ligne de segment : repli
     * sur l'ancien comportement direct pour ne pas le laisser bloqué.
     */
    suspend fun recordPartSent(
        token: String,
        partIndex: Int,
        state: PartSentState,
        failureCode: Int?,
    ) = statusMutex.withLock {
        withContext(Dispatchers.IO) {
            val updated = db.messagePartDao().updateSent(token, partIndex, state, failureCode)
            if (updated == 0) {
                // Repli historique (aucune ligne de segment) : même
                // protection, l'écriture passe par le DAO conditionnel.
                when (state) {
                    PartSentState.CONFIRMED -> updateMessageStatus(token, MessageStatus.SENT)
                    PartSentState.FAILED ->
                        updateMessageStatus(token, MessageStatus.FAILED, failureCode)
                    else -> Unit
                }
                resolveScheduledInternal(token)
            } else {
                refreshAggregatedStatus(token)
            }
        }
    }

    /** Enregistre la livraison d'un segment (L3) puis recalcule le statut agrégé. */
    suspend fun recordPartDelivered(token: String, partIndex: Int) =
        statusMutex.withLock {
            withContext(Dispatchers.IO) {
                val updated = db.messagePartDao().updateDelivered(token, partIndex)
                if (updated == 0) {
                    updateMessageStatus(token, MessageStatus.DELIVERED)
                    resolveScheduledInternal(token)
                } else {
                    refreshAggregatedStatus(token)
                }
            }
        }

    /** À appeler uniquement sous [statusMutex] (ou depuis un chemin déjà sérialisé). */
    private suspend fun refreshAggregatedStatus(token: String) {
        val message = db.messageDao().findByToken(token) ?: return
        val parts = db.messagePartDao().getByToken(token)
        if (parts.isNotEmpty()) {
            val newStatus = MultipartStatus.aggregate(
                parts.map { PartOutcome(it.sentState, it.delivered) },
                message.status,
            )
            if (newStatus != message.status) {
                val failureCode = if (newStatus == MessageStatus.FAILED) {
                    parts.firstOrNull { it.sentState == PartSentState.FAILED }?.failureCode
                } else {
                    null
                }
                updateMessageStatus(token, newStatus, failureCode)
            }
        }
        resolveScheduledInternal(token)
    }

    /**
     * Résout un envoi programmé dont la tentative porte ce jeton (L3).
     * Appelée après chaque accusé et juste après l'attache du jeton par
     * le worker : les deux ordres d'arrivée convergent. Un accusé tardif
     * résout aussi une tentative déjà balayée en AMBIGUOUS (DAO).
     */
    suspend fun resolveScheduled(token: String) = statusMutex.withLock {
        withContext(Dispatchers.IO) { resolveScheduledInternal(token) }
    }

    /** À appeler uniquement sous [statusMutex]. */
    private suspend fun resolveScheduledInternal(token: String) {
        val message = db.messageDao().findByToken(token) ?: return
        when (message.status) {
            MessageStatus.SENT, MessageStatus.DELIVERED ->
                db.scheduledMessageDao().resolveByToken(token, ScheduledStatus.SENT)
            MessageStatus.FAILED ->
                db.scheduledMessageDao().resolveByToken(token, ScheduledStatus.FAILED)
            else -> Unit
        }
    }

    // ---- Lecture du provider ----

    private data class ThreadRow(
        val threadId: Long,
        val address: String,
        val snippet: String,
        val date: Long,
        val unread: Int,
        val subscriptionId: Int?,
    )

    private suspend fun loadConversations(): List<ConversationItem> {
        val rows = try {
            queryThreads()
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "lecture conversations sans permission SMS")
            return emptyList()
        }
        val archived = db.archiveDao().archivedIds().toSet()
        val pinned = db.pinnedThreadDao().pinnedIds().toSet()
        // Noms des contacts en parallèle : N requêtes séquentielles au
        // provider Contacts coûtaient plusieurs secondes sur gros carnet.
        return coroutineScope {
            rows
                .filter { it.threadId !in archived }
                .map { row ->
                    async {
                        ConversationItem(
                            threadId = row.threadId,
                            address = row.address,
                            displayName = runCatching { lookupNameSync(row.address) }.getOrDefault(row.address),
                            snippet = row.snippet,
                            date = row.date,
                            unreadCount = row.unread,
                            subscriptionId = row.subscriptionId,
                            pinned = row.threadId in pinned,
                            messageCount = db.messageDao().countByThread(row.threadId),
                        )
                    }
                }.awaitAll()
                .sortedWith(compareByDescending<ConversationItem> { it.pinned }.thenByDescending { it.date })
        }
    }

    /**
     * Liste des fils via le provider Threads (une ligne par fil) : rapide
     * même avec des milliers de SMS. La ROM expose directement address et
     * body dans son provider (pas de recipient_ids, pas de snippet) : on
     * les lit en une seule requête. Les non-lus viennent d'une requête
     * groupée séparée.
     */
    private fun queryThreads(): List<ThreadRow> {
        val rows = mutableListOf<ThreadRow>()
        val projection = arrayOf(
            Telephony.Threads._ID,
            "thread_id",
            Telephony.Threads.DATE,
            "address",
            "body",
            "sim_id",
        )
        try {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI, projection, null, null,
                "${Telephony.Threads.DATE} DESC",
            )?.use { c ->
                val iThread = c.getColumnIndex("thread_id")
                val iDate = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
                val iAddress = c.getColumnIndex("address")
                val iBody = c.getColumnIndex("body")
                val iSim = c.getColumnIndex("sim_id")
                while (c.moveToNext()) {
                    // thread_id peut manquer sur ROM non standard : repli sur _id.
                    val threadId = if (iThread >= 0) c.getLong(iThread) else c.getLong(
                        c.getColumnIndexOrThrow(Telephony.Threads._ID),
                    )
                    rows.add(
                        ThreadRow(
                            threadId = threadId,
                            address = if (iAddress >= 0) c.getString(iAddress).orEmpty() else "",
                            snippet = if (iBody >= 0) c.getString(iBody).orEmpty() else "",
                            date = c.getLong(iDate),
                            unread = 0,
                            subscriptionId = if (iSim >= 0) {
                                c.getInt(iSim).takeIf { it >= 0 }
                            } else {
                                null
                            },
                        ),
                    )
                }
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: IllegalArgumentException) {
            // Provider Threads incompatible : repli sur le balayage direct.
            return legacyScanThreads()
        } catch (e: android.database.sqlite.SQLiteException) {
            return legacyScanThreads()
        }
        if (rows.isEmpty()) return emptyList()
        val unread = queryUnreadCounts()
        return rows.map { it.copy(unread = unread[it.threadId] ?: 0) }
    }

    /**
     * Repli historique : balaye la table des SMS (lent sur gros volumes
     * mais universel). Utilisé seulement si le provider Threads de la ROM
     * est incompatible.
     */
    private fun legacyScanThreads(): List<ThreadRow> {
        val rows = linkedMapOf<Long, ThreadRow>()
        val unread = mutableMapOf<Long, Int>()
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, null, null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iThread = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iRead = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val iSub = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            while (c.moveToNext()) {
                val threadId = c.getLong(iThread)
                if (c.getInt(iRead) == 0) unread[threadId] = (unread[threadId] ?: 0) + 1
                if (!rows.containsKey(threadId)) {
                    rows[threadId] = ThreadRow(
                        threadId = threadId,
                        address = c.getString(iAddress).orEmpty(),
                        snippet = c.getString(iBody).orEmpty(),
                        date = c.getLong(iDate),
                        unread = 0,
                        subscriptionId = if (iSub >= 0) {
                            c.getInt(iSub).takeIf { it >= 0 }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        return rows.values.map { it.copy(unread = unread[it.threadId] ?: 0) }
    }

    /** Compte les non-lus par fil en une seule requête filtrée (peu de lignes). */
    private fun queryUnreadCounts(): Map<Long, Int> {
        val out = mutableMapOf<Long, Int>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            "${Telephony.Sms.READ} = 0",
            null,
            null,
        )?.use { c ->
            val iThread = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (c.moveToNext()) {
                val threadId = c.getLong(iThread)
                out[threadId] = (out[threadId] ?: 0) + 1
            }
        }
        return out
    }

    private fun queryThreadMessages(threadId: Long, limit: Int? = null): List<Message> {
        val out = mutableListOf<Message>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
        val sortOrder = if (limit != null) {
            Telephony.Sms.DATE + " ASC LIMIT " + limit
        } else {
            Telephony.Sms.DATE + " ASC"
        }
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection,
            "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
            sortOrder,
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iRead = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val iStatus = c.getColumnIndexOrThrow(Telephony.Sms.STATUS)
            val iSub = c.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID)
            while (c.moveToNext()) {
                val type = c.getInt(iType)
                out.add(
                    Message(
                        providerId = c.getLong(iId),
                        threadId = threadId,
                        address = c.getString(iAddress).orEmpty(),
                        body = c.getString(iBody).orEmpty(),
                        date = c.getLong(iDate),
                        read = c.getInt(iRead) == 1,
                        incoming = type == Telephony.Sms.MESSAGE_TYPE_INBOX,
                        status = when {
                            type == Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageStatus.RECEIVED
                            c.getInt(iStatus) == Telephony.Sms.STATUS_COMPLETE -> MessageStatus.DELIVERED
                            c.getInt(iStatus) == Telephony.Sms.STATUS_FAILED -> MessageStatus.FAILED
                            else -> MessageStatus.SENT
                        },
                        subscriptionId = c.getInt(iSub),
                    ),
                )
            }
        }
        return out
    }

    /** Identifiants des fils les plus récents (pour le remplissage FTS). */
    private fun queryRecentThreadIds(limit: Int): List<Long> {
        val seen = linkedSetOf<Long>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID),
            null, null,
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val iThread = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (c.moveToNext() && seen.size < limit) {
                seen.add(c.getLong(iThread))
            }
        }
        return seen.toList()
    }

    private fun lookupNameSync(address: String): String {
        if (address.isBlank()) return address
        nameCache[address]?.let { return it }
        val name = try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(address),
            )
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: address
        } catch (e: SecurityException) {
            address
        }
        nameCache[address] = name
        return name
    }

    private fun conversationEntity(
        threadId: Long,
        address: String,
        snippet: String,
        date: Long,
        unread: Int,
    ) = Conversation(
        threadId = threadId,
        displayName = nameCache[address] ?: address,
        lastSnippet = snippet,
        lastDate = date,
        unreadCount = unread,
    )

    companion object {
        private const val TAG = "SmsRepository"

        /** Remplissage de l'index FTS : 100 fils récents, 3000 messages par fil, 1 fois par jour. */
        private const val BACKFILL_THREADS = 100
        private const val BACKFILL_PER_THREAD = 3000
        private const val BACKFILL_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
