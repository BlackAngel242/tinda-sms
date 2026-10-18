package tech.dmampro.tinda.sms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import tech.dmampro.tinda.MainActivity
import tech.dmampro.tinda.util.TindaLogger

/**
 * Réponse rapide depuis une notification d'appel manqué
 * (action RESPOND_VIA_MESSAGE). Lot 0 : redirige vers l'accueil.
 */
class RespondViaMessageActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TindaLogger.i(TAG, "Demande de réponse via message reçue")
        // Lot 1 : pré-remplir le destinataire issu de l'intent.
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "RespondViaMessage"
    }
}
