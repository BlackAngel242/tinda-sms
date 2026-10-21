package tech.dmampro.tinda.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.ScheduledStatus
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.TindaLogger
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
 * Cycle de vie (L3) : le worker prend la demande de façon atomique
 * (PENDING -> SENDING) avant d'envoyer ; le retour de l'appel radio ne
 * vaut pas « envoyé ». La ligne passe à SENT ou FAILED quand le
 * verdict d'émission du message arrive (résolution par jeton dans le
 * dépôt). Si le processus meurt entre l'appel radio et le verdict, la
 * demande reste SENDING puis devient AMBIGUOUS au balayage : jamais de
 * renvoi automatique d'un état incertain.
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
        val item = dao.getById(id) ?: return Result.failure()
        if (item.status != ScheduledStatus.PENDING) {
            TindaLogger.i(TAG, "SMS programmé déjà traité")
            return Result.success()
        }
        if (!isDefaultSmsApp(applicationContext)) {
            dao.updateStatus(id, ScheduledStatus.FAILED)
            SendJournal.log("programmé : échec, pas appli par défaut")
            return Result.failure()
        }
        // Prise atomique : si une autre exécution ou une annulation est
        // passée entre la lecture et ici, claim vaut 0 et on n'envoie pas.
        if (dao.claim(id, System.currentTimeMillis()) == 0) {
            TindaLogger.i(TAG, "SMS programmé pris par un autre chemin")
            return Result.success()
        }
        return try {
            val threadId = entryPoint.repository().getOrCreateThreadId(item.address)
            val result = entryPoint.sender().send(
                address = item.address,
                body = item.body,
                subscriptionId = item.subscriptionId,
                threadId = threadId,
                deliveryReport = true,
            )
            dao.attachToken(id, result.token)
            // Le verdict a pu arriver pendant l'appel : résolution immédiate.
            entryPoint.repository().resolveScheduled(result.token)
            SendJournal.log("programmé : émission lancée, en attente du verdict")
            Result.success()
        } catch (e: Exception) {
            TindaLogger.w(TAG, "échec d'envoi programmé")
            dao.updateStatus(id, ScheduledStatus.FAILED)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "ScheduledSmsWorker"
        const val KEY_SCHEDULED_ID = "scheduled_id"
        const val WORK_TAG = "tinda_scheduled_sms"
    }
}
