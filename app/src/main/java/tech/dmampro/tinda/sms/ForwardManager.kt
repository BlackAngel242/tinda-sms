package tech.dmampro.tinda.sms

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.ForwardLog
import tech.dmampro.tinda.data.local.ForwardRule
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.TindaLogger
import tech.dmampro.tinda.util.isDefaultSmsApp

/**
 * Transfert automatique (F11) : recopie les SMS reçus correspondant à une
 * règle vers un numéro destinataire. Anti-boucle :
 * - jamais de transfert vers l'expéditeur lui-même (boucle A <-> B) ;
 * - jamais deux transferts identiques (règle, expéditeur) dans la minute
 *   (déduplication des réceptions multiples) ;
 * - la liste noire et le coffre secret filtrent en amont (SmsReceiver).
 */
@Singleton
class ForwardManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
    private val repository: SmsRepository,
    private val sender: SmsSender,
) {

    /** Traite un SMS entrant déjà stocké. Appelé depuis SmsReceiver. */
    suspend fun handleIncoming(
        address: String,
        body: String,
        threadId: Long,
        subscriptionId: Int,
    ) {
        val rules = db.forwardDao().getEnabled()
        if (rules.isEmpty()) return
        if (!isDefaultSmsApp(context)) return
        db.forwardDao().cleanup(System.currentTimeMillis() - CLEANUP_WINDOW_MS)

        for (rule in rules) {
            if (!matchesSource(rule, address, body)) continue
            if (!isForwardableDestination(rule.destination, address)) continue
            val now = System.currentTimeMillis()
            if (db.forwardDao().countRecent(rule.id, address, now - DEDUP_WINDOW_MS) > 0) continue
            // Garde-fou P0-8 : un code de vérification détecté n'est
            // transféré que si la règle l'autorise explicitement. La
            // suspension est journalisée, jamais silencieuse.
            if (!ForwardGuard.shouldForward(body, rule.allowVerificationCodes)) {
                db.forwardDao().logForward(
                    ForwardLog(rule.id, address, now, heldVerificationCode = true),
                )
                EventLog.log("forward.held_verification_code")
                SendJournal.log("transfert : suspendu, code de vérification (règle ${rule.name})")
                continue
            }
            val text = buildForwardText(rule, address, body)
            try {
                val destThreadId = repository.getOrCreateThreadId(rule.destination)
                sender.send(
                    address = rule.destination,
                    body = text,
                    subscriptionId = subscriptionId,
                    threadId = destThreadId,
                    deliveryReport = false,
                )
                db.forwardDao().logForward(ForwardLog(rule.id, address, now))
                EventLog.log("forward.sent")
                SendJournal.log("transfert : envoyé (règle ${rule.name})")
            } catch (e: Exception) {
                TindaLogger.w(TAG, "échec du transfert")
                EventLog.log("forward.failed", e.javaClass.simpleName)
            }
        }
    }

    companion object {
        private const val TAG = "ForwardManager"
        private const val DEDUP_WINDOW_MS = 60_000L
        private const val CLEANUP_WINDOW_MS = 7L * 24 * 3_600_000
    }
}

/** La règle correspond-elle à cet expéditeur / ce texte ? (logique pure) */
fun matchesSource(rule: ForwardRule, address: String, body: String): Boolean {
    if (rule.sourceValue.isBlank()) return false
    return when (rule.sourceType.uppercase()) {
        "KEYWORD" -> body.contains(rule.sourceValue, ignoreCase = true)
        else -> normalizeNumber(address) == normalizeNumber(rule.sourceValue)
    }
}

/**
 * Anti-boucle : la destination doit être un numéro composable, différent
 * de l'expéditeur (normalisé). (logique pure)
 */
fun isForwardableDestination(destination: String, senderAddress: String): Boolean {
    if (!isReplyableAddress(destination)) return false
    return normalizeNumber(destination) != normalizeNumber(senderAddress)
}

/** Texte transféré : préfixé par l'expéditeur d'origine. (logique pure) */
fun buildForwardText(rule: ForwardRule, address: String, body: String): String {
    return if (rule.includeContent) {
        "Transfert de $address : $body"
    } else {
        "Transfert : message reçu de $address"
    }
}

private fun normalizeNumber(number: String): String =
    number.filter { it.isDigit() }.takeLast(9)
