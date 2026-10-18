package tech.dmampro.tinda.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.dmampro.tinda.util.TindaLogger
import javax.inject.Inject
import javax.inject.Singleton

/** Carte SIM active vue par l'app. */
data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val slotIndex: Int,
)

/**
 * Double SIM (US-002) : liste les SIM actives, mémorise le choix par
 * conversation (DataStore) avec repli sur la SIM par défaut du système.
 */
@Singleton
class SimRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository,
) {
    private fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

    /** Cartes SIM actives (vide si la permission manque). */
    fun getActiveSims(): List<SimInfo> {
        if (!hasPhoneStatePermission()) return emptyList()
        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.getSystemService(SubscriptionManager::class.java)
        } else {
            null
        } ?: return emptyList()
        return try {
            manager.activeSubscriptionInfoList.orEmpty().map { info: SubscriptionInfo ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    displayName = info.displayName?.toString()
                        ?: "SIM ${info.simSlotIndex + 1}",
                    slotIndex = info.simSlotIndex,
                )
            }.sortedBy { it.slotIndex }
        } catch (e: SecurityException) {
            TindaLogger.w(TAG, "liste SIM sans permission téléphone")
            emptyList()
        }
    }

    /** Abonnement SMS par défaut du système. */
    fun getDefaultSubscriptionId(): Int =
        SmsManager.getDefaultSmsSubscriptionId()

    /** SIM à utiliser pour une conversation : choix mémorisé, sinon défaut système. */
    suspend fun getSimForThread(threadId: Long): Int =
        prefs.getThreadSim(threadId) ?: getDefaultSubscriptionId()

    suspend fun setSimForThread(threadId: Long, subscriptionId: Int) {
        prefs.setThreadSim(threadId, subscriptionId)
    }

    companion object {
        private const val TAG = "SimRepository"
    }
}
