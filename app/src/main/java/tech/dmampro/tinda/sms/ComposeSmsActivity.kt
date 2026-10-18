package tech.dmampro.tinda.sms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import tech.dmampro.tinda.MainActivity

/**
 * Composition d'un SMS depuis une autre application (action SENDTO, schemes
 * smsto:/sms:). Lot 0 : redirige vers l'accueil ; lot 1 : pré-remplit le destinataire.
 */
class ComposeSmsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
