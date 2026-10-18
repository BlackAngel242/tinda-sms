package tech.dmampro.tinda.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import tech.dmampro.tinda.util.TindaLogger

/**
 * Réception des SMS (action SMS_DELIVER_ACTION, réservée à la default SMS app).
 * Lot 1 : écrire dans le provider système + miroir local, notifier.
 * Règle : jamais de contenu ni de numéro dans les logs.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        // Compte uniquement, aucune donnée personnelle (AGENTS.md).
        TindaLogger.i(TAG, "SMS_DELIVER reçu : ${messages.size} segment(s)")
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
