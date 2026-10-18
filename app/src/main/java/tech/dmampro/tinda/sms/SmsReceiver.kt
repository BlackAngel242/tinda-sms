package tech.dmampro.tinda.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.BlockRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.notifications.NotificationHelper
import tech.dmampro.tinda.util.TindaLogger
import javax.inject.Inject

/**
 * Réception des SMS (action SMS_DELIVER_ACTION, réservée à la default SMS app).
 *
 * Analyse des PDU via Telephony.Sms.Intents.getMessagesFromIntent (qui parse
 * les PDU avec le bon format 3GPP/3GPP2), filtre liste noire (F7), écriture
 * dans le provider Inbox, miroir Room, notification par conversation.
 * Règle : jamais de contenu ni de numéro dans les logs.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsRepository

    @Inject
    lateinit var blockRepository: BlockRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        // Compte uniquement, aucune donnée personnelle (AGENTS.md).
        TindaLogger.i(TAG, "SMS_DELIVER reçu : ${messages.size} segment(s)")

        val subscriptionId = intent.getIntExtra("subscription", -1)
        val pending = goAsync()
        scope.launch {
            try {
                // Regroupe les segments d'un même SMS long.
                val body = messages.joinToString("") { it.messageBody.orEmpty() }
                val address = messages.firstOrNull()?.originatingAddress.orEmpty()
                val date = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                if (address.isBlank() || body.isEmpty()) return@launch

                // Liste noire (F7) : le message est ignoré (ni provider, ni
                // notification), seul l'événement est journalisé, sans contenu.
                if (blockRepository.isBlocked(address)) {
                    blockRepository.logBlockedEvent(address)
                    TindaLogger.i(TAG, "SMS bloqué par la liste noire")
                    return@launch
                }

                val threadId = repository.insertIncoming(address, body, date, subscriptionId)
                if (threadId < 0) return@launch

                val displayName = repository.resolveDisplayName(address)
                NotificationHelper.showMessage(context, threadId, displayName, body)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
