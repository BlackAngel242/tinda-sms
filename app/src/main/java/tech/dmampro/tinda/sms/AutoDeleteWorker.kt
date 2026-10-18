package tech.dmampro.tinda.sms

import android.content.Context
import android.provider.Telephony
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.AutoDeleteRule
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.TindaLogger
import tech.dmampro.tinda.util.isDefaultSmsApp

/** Point d'entrée Hilt pour le worker (sans dépendance hilt-work). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoDeleteWorkerEntryPoint {
    fun db(): TindaDatabase
    fun repository(): SmsRepository
}

/**
 * Suppression automatique (lot 4) : une fois par jour, supprime les messages
 * plus vieux que [AutoDeleteRule.olderThanDays] dans les fils visés, en
 * conservant au moins [AutoDeleteRule.keepMinimum] messages récents par fil.
 * Supprime du provider système ET du miroir Room.
 */
class AutoDeleteWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val entryPoint: AutoDeleteWorkerEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AutoDeleteWorkerEntryPoint::class.java,
        )

    override suspend fun doWork(): Result {
        if (!isDefaultSmsApp(applicationContext)) {
            TindaLogger.i(TAG, "suppression auto ignorée : pas appli par défaut")
            return Result.success()
        }
        val db = entryPoint.db()
        val repository = entryPoint.repository()
        val rules = db.autoDeleteDao().getEnabled()
        if (rules.isEmpty()) return Result.success()
        var deleted = 0
        for (rule in rules) {
            deleted += applyRule(db, repository, rule)
        }
        EventLog.log("autodelete.run", "$deleted")
        TindaLogger.i(TAG, "suppression auto : $deleted message(s)")
        return Result.success()
    }

    private suspend fun applyRule(
        db: TindaDatabase,
        repository: SmsRepository,
        rule: AutoDeleteRule,
    ): Int {
        val cutoff = deletionCutoff(rule.olderThanDays)
        val candidates = db.messageDao().getOlderThan(cutoff)
        if (candidates.isEmpty()) return 0
        // Regroupe par fil (déjà triés fil puis date décroissante).
        val byThread = candidates.groupBy { it.threadId }
        var deleted = 0
        for ((threadId, messages) in byThread) {
            if (rule.threadAddress.isNotBlank()) {
                val address = repository.getThreadAddress(threadId)
                if (normalize(address) != normalize(rule.threadAddress)) continue
            }
            for (message in selectForDeletion(messages, rule.keepMinimum)) {
                if (deleteMessage(db, message)) deleted++
            }
        }
        return deleted
    }

    /**
     * Supprime un message du provider système (si connu) et du miroir Room.
     * Retourne true si la suppression Room a eu lieu.
     */
    private suspend fun deleteMessage(db: TindaDatabase, message: Message): Boolean {
        return try {
            message.providerId?.let { providerId ->
                applicationContext.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "_id=?",
                    arrayOf(providerId.toString()),
                )
            }
            db.messageDao().deleteById(message.id)
            true
        } catch (e: Exception) {
            TindaLogger.w(TAG, "échec suppression message")
            false
        }
    }

    private fun normalize(number: String): String =
        number.filter { it.isDigit() }.takeLast(9)

    companion object {
        private const val TAG = "AutoDeleteWorker"
        const val WORK_NAME = "tinda_auto_delete"
    }
}

/** Échéance : maintenant moins [days] jours, en millisecondes. (logique pure) */
fun deletionCutoff(days: Int, now: Long = System.currentTimeMillis()): Long =
    now - days.coerceAtLeast(1) * 24L * 3_600_000

/**
 * Messages à supprimer : la liste est triée du plus récent au plus ancien,
 * on garde les [keep] premiers. (logique pure)
 */
fun selectForDeletion(messages: List<Message>, keep: Int): List<Message> =
    messages.drop(keep.coerceAtLeast(0))
