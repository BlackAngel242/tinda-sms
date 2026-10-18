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
import tech.dmampro.tinda.util.SendJournal
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
                        val rc = resultCode
                        SendJournal.log("accusé SMS_SENT : resultCode=$rc")
                        when (rc) {
                            Activity.RESULT_OK, Activity.RESULT_CANCELED -> {
                                // -1 : envoyé (AOSP). 0 : sur certaines ROM
                                // (constaté sur HiOS), le SMS part bien mais
                                // le résultat n'est pas positionné : le code
                                // reste à sa valeur par défaut (0 = annulé).
                                repository.updateMessageStatus(token, MessageStatus.SENT)
                            }
                            else -> {
                                // Seuls les codes d'erreur documentés
                                // (1 à 4) sont des échecs avérés. Code seul,
                                // aucune donnée personnelle (AGENTS.md).
                                TindaLogger.w(TAG, "échec d'envoi, code=$rc")
                                repository.updateMessageStatus(token, MessageStatus.FAILED, rc)
                            }
                        }
                    }
                    SmsSender.ACTION_SMS_DELIVERED -> {
                        val rc = resultCode
                        SendJournal.log("accusé SMS_DELIVERED : resultCode=$rc")
                        // La réception de l'intent = l'opérateur confirme la
                        // remise. On ne dégrade jamais vers l'échec ici.
                        if (rc == Activity.RESULT_OK || rc == Activity.RESULT_CANCELED) {
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
