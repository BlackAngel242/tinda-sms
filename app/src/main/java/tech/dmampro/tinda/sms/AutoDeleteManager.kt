package tech.dmampro.tinda.sms

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import tech.dmampro.tinda.data.local.AutoDeleteRule
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.EventLog

/**
 * Gestion des règles de suppression automatique (lot 4) : CRUD et
 * planification du travail quotidien.
 */
@Singleton
class AutoDeleteManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TindaDatabase,
) {

    fun observeAll(): Flow<List<AutoDeleteRule>> =
        db.autoDeleteDao().observeAll()

    suspend fun save(rule: AutoDeleteRule) {
        require(rule.name.isNotBlank()) { "nom requis" }
        require(rule.olderThanDays >= 1) { "ancienneté minimale : 1 jour" }
        if (rule.id == 0L) db.autoDeleteDao().insert(rule)
        else db.autoDeleteDao().update(rule)
        EventLog.log("autodelete.rule_save")
        reschedule()
    }

    suspend fun toggle(id: Long, enabled: Boolean) {
        db.autoDeleteDao().setEnabled(id, enabled)
        EventLog.log("autodelete.rule_toggle", "$id:$enabled")
        reschedule()
    }

    suspend fun delete(id: Long) {
        db.autoDeleteDao().delete(id)
        EventLog.log("autodelete.rule_delete")
        reschedule()
    }

    /**
     * Planifie (ou annule) le travail quotidien selon qu'il reste des
     * règles actives.
     */
    suspend fun reschedule() {
        val workManager = WorkManager.getInstance(context)
        if (db.autoDeleteDao().getEnabled().isEmpty()) {
            workManager.cancelUniqueWork(AutoDeleteWorker.WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoDeleteWorker>(24, TimeUnit.HOURS)
            .addTag(AutoDeleteWorker.WORK_NAME)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AutoDeleteWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
