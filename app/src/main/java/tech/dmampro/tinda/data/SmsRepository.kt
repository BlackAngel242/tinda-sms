package tech.dmampro.tinda.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.data.local.ArchivedThread
import tech.dmampro.tinda.data.local.Conversation
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.data.local.TindaDatabase
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
    private val canonicalCache = mutableMapOf<String, String>()

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
            db.messageDao().updateStatusAndFailureByToken(token, status, failureCode)
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

    // ---- Lecture du provider ----

    private data class ThreadRow(
        val threadId: Long,
        val address: String,
        val snippet: String,
        val date: Long,
        val unread: Int,
    )

    private suspend fun loadConversations(): List<ConversationItem> {
        val rows = try {
            queryThreads()
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "lecture conversations sans permission SMS")
            return emptyList()
        }
        val archived = db.archiveDao().archivedIds().toSet()
        return rows
            .filter { it.threadId !in archived }
            .map { row ->
                ConversationItem(
                    threadId = row.threadId,
                    address = row.address,
                    displayName = runCatching { lookupNameSync(row.address) }.getOrDefault(row.address),
                    snippet = row.snippet,
                    date = row.date,
                    unread = row.unread > 0,
                )
            }
    }

    /**
     * Liste des fils via le provider Threads (une ligne par fil) : rapide
     * même avec des milliers de SMS, contrairement au balayage complet de
     * la table des SMS. L'adresse vient de canonical_addresses via
     * recipient_ids ; les non-lus d'une seule requête groupée.
     */
    private fun queryThreads(): List<ThreadRow> {
        val rows = mutableListOf<ThreadRow>()
        val projection = arrayOf(
            Telephony.Threads._ID,
            Telephony.Threads.DATE,
            Telephony.Threads.SNIPPET,
            Telephony.Threads.RECIPIENT_IDS,
        )
        try {
            context.contentResolver.query(
                Telephony.Threads.CONTENT_URI, projection, null, null,
                "${Telephony.Threads.DATE} DESC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(Telephony.Threads._ID)
                val iDate = c.getColumnIndexOrThrow(Telephony.Threads.DATE)
                val iSnippet = c.getColumnIndexOrThrow(Telephony.Threads.SNIPPET)
                val iRecipients =
                    c.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS)
                while (c.moveToNext()) {
                    rows.add(
                        ThreadRow(
                            threadId = c.getLong(iId),
                            address = canonicalAddress(c.getString(iRecipients).orEmpty()),
                            snippet = c.getString(iSnippet).orEmpty(),
                            date = c.getLong(iDate),
                            unread = 0,
                        ),
                    )
                }
            }
        } catch (e: SecurityException) {
            throw e
        }
        if (rows.isEmpty()) return emptyList()
        val unread = queryUnreadCounts()
        return rows.map { it.copy(unread = unread[it.threadId] ?: 0) }
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

    /**
     * Adresse canonique d'un fil à partir de ses recipient_ids (cache en mémoire).
     * La table canonical-addresses n'a pas de classe dans le SDK public :
     * on passe par son URI directe et les colonnes CanonicalAddressesColumns.
     */
    private fun canonicalAddress(recipientIds: String): String {
        if (recipientIds.isBlank()) return ""
        canonicalCache[recipientIds]?.let { return it }
        val ids = recipientIds.trim().split(" ").filter { it.isNotBlank() }
        if (ids.isEmpty()) return ""
        val address = try {
            val placeholders = ids.joinToString(",") { "?" }
            context.contentResolver.query(
                Uri.parse("content://mms-sms/canonical-addresses"),
                arrayOf(
                    Telephony.CanonicalAddressesColumns._ID,
                    Telephony.CanonicalAddressesColumns.ADDRESS,
                ),
                "${Telephony.CanonicalAddressesColumns._ID} IN ($placeholders)",
                ids.toTypedArray(),
                null,
            )?.use { c ->
                val iAddr = c.getColumnIndexOrThrow(
                    Telephony.CanonicalAddressesColumns.ADDRESS,
                )
                buildList {
                    while (c.moveToNext()) add(c.getString(iAddr).orEmpty())
                }.filter { it.isNotBlank() }.joinToString(", ")
            }.orEmpty()
        } catch (e: SecurityException) {
            ""
        }
        canonicalCache[recipientIds] = address
        return address
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
