package tech.dmampro.tinda.sms

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.ScheduledMessage
import tech.dmampro.tinda.data.local.ScheduledStatus
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.SendJournal

/**
 * Planificateur de SMS (F9) : persiste la demande et programme un
 * WorkManager à l'échéance. Après un redémarrage, [reschedulePending]
 * réenfile les envois restants (appelé par BootReceiver).
 *
 * Note : WorkManager n'est pas exact à la minute en Doze (retard possible
 * de quelques minutes). Pas de permission d'alarme exacte requise.
 */
@Singleton
class ScheduledSmsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
) {

    fun observeAll(): Flow<List<ScheduledMessage>> =
        db.scheduledMessageDao().observeAll()

    /**
     * Programme un SMS. Lève IllegalArgumentException si la date est passée.
     * Retourne l'identifiant de la ligne.
     */
    suspend fun schedule(
        address: String,
        body: String,
        scheduledAt: Long,
        subscriptionId: Int = -1,
    ): Long {
        require(address.isNotBlank() && body.isNotBlank()) { "adresse et corps requis" }
        val delay = scheduledAt - System.currentTimeMillis()
        require(delay > 0) { "la date doit être dans le futur" }
        val id = db.scheduledMessageDao().insert(
            ScheduledMessage(
                address = address,
                body = body,
                scheduledAt = scheduledAt,
                subscriptionId = subscriptionId,
                createdAt = System.currentTimeMillis(),
            ),
        )
        enqueue(id, delay)
        SendJournal.log("programmé : planifié dans ${delay / 60000} min")
        return id
    }

    /** Annule un envoi programmé (travail + ligne). */
    suspend fun cancel(id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        db.scheduledMessageDao().updateStatus(id, ScheduledStatus.CANCELLED)
        SendJournal.log("programmé : annulé")
    }

    /** Supprime définitivement une ligne (historique). */
    suspend fun delete(id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        db.scheduledMessageDao().delete(id)
    }

    /** Réenfile les PENDING après un redémarrage. */
    suspend fun reschedulePending() {
        val now = System.currentTimeMillis()
        for (item in db.scheduledMessageDao().getPending()) {
            val delay = item.scheduledAt - now
            if (delay > 0) {
                enqueue(item.id, delay)
            } else {
                // Échéance dépassée pendant l'arrêt : envoi immédiat.
                enqueue(item.id, 0)
            }
        }
    }

    private fun enqueue(id: Long, delayMillis: Long) {
        val input = Data.Builder()
            .putLong(ScheduledSmsWorker.KEY_SCHEDULED_ID, id)
            .build()
        val request = OneTimeWorkRequestBuilder<ScheduledSmsWorker>()
            .setInputData(input)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(ScheduledSmsWorker.WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun workName(id: Long) = "${ScheduledSmsWorker.WORK_TAG}_$id"
}
