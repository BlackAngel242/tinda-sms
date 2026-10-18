package tech.dmampro.tinda.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.util.TindaLogger
import javax.inject.Inject

/**
 * Accusés d'envoi et de livraison (DEC-008).
 * Receiver interne (exported=false) : seuls nos propres PendingIntent arrivent ici.
 *
 * Honnêteté PRD (RB-05) : en SMS pur, seule la LIVRAISON est confirmable.
 * L'UI affiche donc "livré", jamais "lu", sauf app-à-app (lot ultérieur).
 */
@AndroidEntryPoint
class SmsStatusReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra(SmsSender.EXTRA_TOKEN) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    SmsSender.ACTION_SMS_SENT -> {
                        if (resultCode == Activity.RESULT_OK) {
                            repository.updateMessageStatus(token, MessageStatus.SENT)
                        } else {
                            // Code seul, aucune donnée personnelle (AGENTS.md).
                            TindaLogger.w(TAG, "échec d'envoi, code=$resultCode")
                            repository.updateMessageStatus(token, MessageStatus.FAILED)
                        }
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> {
                        // Un rapport perdu n'est pas un échec : on garde SENT.
                        if (resultCode == Activity.RESULT_OK) {
                            repository.updateMessageStatus(token, MessageStatus.DELIVERED)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsStatus"
    }
}
