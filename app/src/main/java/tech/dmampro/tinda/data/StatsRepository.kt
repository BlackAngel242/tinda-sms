package tech.dmampro.tinda.data

import tech.dmampro.tinda.data.local.MessageDao
import tech.dmampro.tinda.data.local.BlockDao
import tech.dmampro.tinda.data.local.MessageStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Statistiques SMS calculées depuis la base locale (DEC-S1 : zéro tracking propriétaire). */
@Singleton
class StatsRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val blockDao: BlockDao,
) {
    /** Nombre de SMS envoyés via une SIM donnée. */
    suspend fun sentCountForSim(subscriptionId: Int): Int =
        messageDao.countOutgoingBySubscription(subscriptionId)

    /** Nombre total de SMS (entrants + sortants). */
    suspend fun totalCount(): Int = messageDao.countAll()

    /** Nombre de numéros bloqués. */
    suspend fun blockedCount(): Int = blockDao.countAll()

    /** Taux de délivrance : délivrés / envoyés (0..100). */
    suspend fun deliveryRate(): Int {
        val sent = messageDao.countByStatus(MessageStatus.SENT) +
            messageDao.countByStatus(MessageStatus.DELIVERED) +
            messageDao.countByStatus(MessageStatus.FAILED)
        if (sent == 0) return 100
        val delivered = messageDao.countByStatus(MessageStatus.DELIVERED)
        return (delivered * 100 / sent).coerceIn(0, 100)
    }

    /** Nombre de SMS échangés dans un fil (pour "N SMS échangés"). */
    suspend fun countForThread(threadId: Long): Int =
        messageDao.countByThread(threadId)
}
