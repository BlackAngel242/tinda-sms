package tech.dmampro.tinda.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import tech.dmampro.tinda.util.TindaLogger

/**
 * STUB MMS (DEC-002).
 *
 * Nuance importante : Android exige ce composant (action WAP_PUSH_DELIVER_ACTION)
 * pour lister l'application comme candidate au rôle « default SMS », même si
 * l'app ne gère pas le MMS. Périmètre SMS uniquement : ce stub ignore
 * proprement l'événement. Aucun traitement MMS, aucun code MMS dans l'app.
 */
class MmsStubReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        // Volontairement ignoré : pas de MMS dans Tinda SMS (DEC-002).
        TindaLogger.i(TAG, "Push MMS reçu et ignoré (périmètre SMS uniquement)")
    }

    companion object {
        private const val TAG = "MmsStubReceiver"
    }
}
