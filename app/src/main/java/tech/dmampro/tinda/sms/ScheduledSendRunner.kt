package tech.dmampro.tinda.sms

import tech.dmampro.tinda.data.local.ScheduledMessage
import tech.dmampro.tinda.data.local.ScheduledStatus

/**
 * Déroulé d'un envoi programmé, séparé du worker WorkManager pour
 * être testé sur JVM avec un magasin factice qui rejoue les
 * entrelacements réels.
 *
 * Règle (leçon du 24 octobre 2026) : une lecture PENDING ne garantit
 * pas que la ligne est encore PENDING au moment de l'écriture. Toutes
 * les transitions d'échec passent donc par des écritures DAO
 * conditionnelles (état courant et, pour une tentative, son identité
 * de début) ; quand l'écriture touche 0 ligne, un autre chemin a déjà
 * tranché et son état gagne.
 */
class ScheduledSendRunner(
    private val store: Store,
    private val isDefaultSmsApp: () -> Boolean,
    private val nowMillis: () -> Long,
    private val resolveThreadId: suspend (address: String) -> Long,
    private val send: suspend (item: ScheduledMessage, threadId: Long) -> SmsSender.SendResult,
    private val resolveByToken: suspend (token: String) -> Unit,
    private val log: (String) -> Unit = {},
) {

    /** Port du magasin des programmations (implémenté par le DAO Room en production). */
    interface Store {
        suspend fun getById(id: Long): ScheduledMessage?
        suspend fun failIfPending(id: Long): Int
        suspend fun claim(id: Long, startedAt: Long): Int
        suspend fun attachToken(id: Long, token: String): Int
        suspend fun failAttemptIfStillSending(id: Long, startedAt: Long): Int
    }

    enum class Outcome {
        NOT_FOUND,
        ALREADY_HANDLED,
        FAILED_NO_ROLE,
        CLAIM_LOST,
        LAUNCHED,
        FAILED_SEND,
    }

    suspend fun run(id: Long): Outcome {
        val item = store.getById(id) ?: return Outcome.NOT_FOUND
        if (item.status != ScheduledStatus.PENDING) {
            return Outcome.ALREADY_HANDLED
        }
        if (!isDefaultSmsApp()) {
            // Écriture conditionnelle : une annulation arrivée après
            // la lecture laisse la ligne CANCELLED intacte.
            return if (store.failIfPending(id) == 1) {
                log("programmé : échec, pas appli par défaut")
                Outcome.FAILED_NO_ROLE
            } else {
                Outcome.ALREADY_HANDLED
            }
        }
        val startedAt = nowMillis()
        if (store.claim(id, startedAt) == 0) {
            return Outcome.CLAIM_LOST
        }
        return try {
            val threadId = resolveThreadId(item.address)
            val result = send(item, threadId)
            store.attachToken(id, result.token)
            // Le verdict a pu arriver pendant l'appel : résolution immédiate.
            resolveByToken(result.token)
            log("programmé : émission lancée, en attente du verdict")
            Outcome.LAUNCHED
        } catch (e: Exception) {
            // Écriture conditionnelle : un verdict ou un changement
            // d'état survenu entre-temps n'est jamais écrasé.
            if (store.failAttemptIfStillSending(id, startedAt) == 1) {
                Outcome.FAILED_SEND
            } else {
                Outcome.ALREADY_HANDLED
            }
        }
    }
}
