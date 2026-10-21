package tech.dmampro.tinda.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.dmampro.tinda.util.TindaLogger

/**
 * Après un redémarrage, réenfile les SMS programmés restants (F9).
 * Les échéances dépassées pendant l'arrêt partent immédiatement.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ScheduledSmsManager

    @Inject
    lateinit var autoDeleteManager: AutoDeleteManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                scheduler.sweepAmbiguous()
                scheduler.reschedulePending()
                autoDeleteManager.reschedule()
                TindaLogger.i(TAG, "travaux replanifiés après démarrage")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
