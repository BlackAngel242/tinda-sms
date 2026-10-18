package tech.dmampro.tinda.sms

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import tech.dmampro.tinda.util.TindaLogger

/**
 * Service "headless" de réponse rapide (appel manqué -> SMS).
 *
 * Point critique : Android qualifie les candidates "application SMS par
 * défaut" en interrogeant les SERVICES pour RESPOND_VIA_MESSAGE. Une activité
 * seule ne suffit pas, c'est ce qui empêchait Tinda d'apparaître dans la
 * liste des applis SMS par défaut (référence : doc officielle
 * "Getting Your SMS Apps Ready", toujours en vigueur).
 * Le service envoie le texte puis écrit dans le provider Sent pour garder
 * l'historique cohérent. Jamais de contenu dans les logs.
 */
class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESPOND_VIA_MESSAGE) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val destination = intent.data?.schemeSpecificPart.orEmpty()
            if (text.isNotEmpty() && destination.isNotEmpty()) {
                sendQuickResponse(destination, text)
            } else {
                TindaLogger.w("RespondViaMessageService", "intent incomplet, ignoré")
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun sendQuickResponse(destination: String, text: String) {
        try {
            val smsManager = SmsManager.getDefault()
            for (part in smsManager.divideMessage(text)) {
                smsManager.sendTextMessage(destination, null, part, null, null)
            }
            // Historique : la default SMS app écrit ses envois dans le provider.
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, destination)
                put(Telephony.Sms.BODY, text)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        } catch (e: SecurityException) {
            TindaLogger.w("RespondViaMessageService", "envoi refusé (pas default handler)")
        } catch (e: IllegalArgumentException) {
            TindaLogger.w("RespondViaMessageService", "destinataire invalide")
        }
    }

    companion object {
        const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"
    }
}
