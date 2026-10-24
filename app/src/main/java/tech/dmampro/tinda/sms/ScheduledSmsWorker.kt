package tech.dmampro.tinda.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.ScheduledMessage
import tech.dmampro.tinda.data.local.ScheduledMessageDao
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.isDefaultSmsApp

/** Point d'entrée Hilt pour le worker (sans dépendance hilt-work). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScheduledWorkerEntryPoint {
    fun db(): TindaDatabase
    fun repository(): SmsRepository
    fun sender(): SmsSender
}

/**
 * Envoie un SMS programmé (F9) à l'échéance.
 *
 * Le déroulé vit dans [ScheduledSendRunner] (testé sur JVM) ; ce
 * worker ne fait que brancher le DAO, le rôle SMS et l'émetteur réels,
 * puis traduire l'issue en résultat WorkManager.
 *
 * Cycle de vie (L3) : prise atomique PENDING -> SENDING avant l'appel
 * radio ; le retour de l'appel ne vaut pas « envoyé ». La ligne passe
 * à SENT ou FAILED quand le verdict d'émission arrive (résolution par
 * jeton). Si le processus meurt entre l'appel radio et le verdict, la
 * demande reste SENDING puis devient AMBIGUOUS au balayage : jamais de
 * renvoi automatique d'un état incertain. Toutes les écritures d'échec
 * sont conditionnelles (voir [ScheduledSendRunner]).
 */
class ScheduledSmsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val entryPoint: ScheduledWorkerEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ScheduledWorkerEntryPoint::class.java,
        )

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_SCHEDULED_ID, -1L)
        if (id < 0) return Result.failure()
        val dao = entryPoint.db().scheduledMessageDao()
        val repository = entryPoint.repository()
        val runner = ScheduledSendRunner(
            store = DaoScheduledStore(dao),
            isDefaultSmsApp = { isDefaultSmsApp(applicationContext) },
            nowMillis = { System.currentTimeMillis() },
            resolveThreadId = { address -> repository.getOrCreateThreadId(address) },
            send = { item, threadId ->
                entryPoint.sender().send(
                    address = item.address,
                    body = item.body,
                    subscriptionId = item.subscriptionId,
                    threadId = threadId,
                    deliveryReport = true,
                )
            },
            resolveByToken = { token -> repository.resolveScheduled(token) },
            log = { message -> SendJournal.log(message) },
        )
        return when (runner.run(id)) {
            ScheduledSendRunner.Outcome.NOT_FOUND,
            ScheduledSendRunner.Outcome.FAILED_NO_ROLE,
            ScheduledSendRunner.Outcome.FAILED_SEND,
            -> Result.failure()

            ScheduledSendRunner.Outcome.ALREADY_HANDLED,
            ScheduledSendRunner.Outcome.CLAIM_LOST,
            ScheduledSendRunner.Outcome.LAUNCHED,
            -> Result.success()
        }
    }

    /** Branche le DAO Room réel sur le port du déroulé. */
    private class DaoScheduledStore(
        private val dao: ScheduledMessageDao,
    ) : ScheduledSendRunner.Store {
        override suspend fun getById(id: Long): ScheduledMessage? = dao.getById(id)
        override suspend fun failIfPending(id: Long): Int = dao.failIfPending(id)
        override suspend fun claim(id: Long, startedAt: Long): Int = dao.claim(id, startedAt)
        override suspend fun attachToken(id: Long, token: String): Int =
            dao.attachToken(id, token)

        override suspend fun failAttemptIfStillSending(id: Long, startedAt: Long): Int =
            dao.failAttemptIfStillSending(id, startedAt)
    }

    companion object {
        const val KEY_SCHEDULED_ID = "scheduled_id"
        const val WORK_TAG = "tinda_scheduled_sms"
    }
}
