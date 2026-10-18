package tech.dmampro.tinda.sms

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.AutoReplyRule
import tech.dmampro.tinda.data.local.AutoReplyScope
import tech.dmampro.tinda.data.local.AutoReplyState
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.TindaLogger
import tech.dmampro.tinda.util.isDefaultSmsApp

/**
 * Répondeur automatique (F10) : à chaque SMS reçu, la première règle active
 * correspondante répond une fois. Garde-fous anti-boucle :
 * - pas de réponse aux numéros courts (≤ 6 chiffres) ni aux expéditeurs
 *   non numériques ;
 * - délai minimal (cooldownHours) entre deux réponses au même numéro ;
 * - pas de réponse à un message identique au nôtre (ping-pong) ;
 * - pas de réponse aux SMS du coffre secret ni à la liste noire
 *   (filtrage en amont, dans SmsReceiver).
 */
@Singleton
class AutoReplyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
    private val repository: SmsRepository,
    private val sender: SmsSender,
) {

    /**
     * Traite un SMS entrant déjà stocké (threadId ≥ 0). Ne fait rien si
     * aucune règle ne correspond. Appelé depuis SmsReceiver, jamais sur
     * le fil principal.
     */
    suspend fun handleIncoming(
        address: String,
        body: String,
        threadId: Long,
        subscriptionId: Int,
    ) {
        val rules = db.autoReplyDao().getEnabled()
        if (rules.isEmpty()) return
        if (!isDefaultSmsApp(context)) return
        if (!isReplyableAddress(address)) {
            TindaLogger.i(TAG, "répondeur ignoré : adresse non répondable")
            return
        }
        // Nettoie les vieux états (une semaine) avant de vérifier le délai.
        db.autoReplyStateDao().cleanup(System.currentTimeMillis() - CLEANUP_WINDOW_MS)

        val now = Calendar.getInstance()
        for (rule in rules) {
            if (!matchesWindow(rule, now)) continue
            if (!matchesScope(rule, address)) continue
            if (body.trim() == rule.message.trim()) continue // ping-pong
            val lastSent = db.autoReplyStateDao().lastSentAt(rule.id, address)
            val cooldownMs = rule.cooldownHours.coerceAtLeast(1) * 3_600_000L
            if (lastSent != null && System.currentTimeMillis() - lastSent < cooldownMs) continue
            try {
                sender.send(
                    address = address,
                    body = rule.message,
                    subscriptionId = subscriptionId,
                    threadId = threadId,
                    deliveryReport = false,
                )
                db.autoReplyStateDao().upsert(
                    AutoReplyState(rule.id, address, System.currentTimeMillis()),
                )
                SendJournal.log("répondeur : réponse envoyée (règle ${rule.name})")
            } catch (e: Exception) {
                TindaLogger.w(TAG, "échec de la réponse auto")
            }
            return // une seule règle par SMS reçu
        }
    }

    /** Portée : tous, contacts uniquement, hors contacts. */
    suspend fun matchesScope(rule: AutoReplyRule, address: String): Boolean {
        return when (rule.scope) {
            AutoReplyScope.ALL -> true
            AutoReplyScope.CONTACTS_ONLY -> isContact(address)
            AutoReplyScope.NON_CONTACTS -> !isContact(address)
        }
    }

    private suspend fun isContact(address: String): Boolean {
        return try {
            repository.resolveDisplayName(address) != address
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "AutoReplyManager"
        private const val CLEANUP_WINDOW_MS = 7L * 24 * 3_600_000
    }
}
