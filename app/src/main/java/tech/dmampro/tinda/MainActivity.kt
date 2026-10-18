package tech.dmampro.tinda

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import tech.dmampro.tinda.navigation.Routes
import tech.dmampro.tinda.navigation.TindaNavGraph
import tech.dmampro.tinda.theme.TindaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Requis : edge-to-edge imposé à partir de targetSdk 35.

        // Destination de départ : notification, onboarding, ou liste.
        val deepThread = intent.getLongExtra(EXTRA_OPEN_THREAD, -1L)
        val startDestination = when {
            deepThread >= 0 -> Routes.conversationDetail(deepThread)
            isDefaultSmsApp() -> Routes.CONVERSATIONS
            else -> Routes.ONBOARDING
        }
        setContent {
            TindaTheme {
                TindaNavGraph(
                    startDestination = startDestination,
                    onRequestDefaultRole = ::requestDefaultSmsRole,
                )
            }
        }
    }

    private fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(this) == packageName

    /**
     * Demande le rôle "application SMS par défaut" : RoleManager (API 29+)
     * via createRequestRoleIntent (dialogue système), sinon l'intent
     * historique. Le résultat se vérifie au retour (l'onboarding
     * ré-interroge le rôle).
     */
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Rien à traiter ici : l'onboarding revérifie le rôle au retour.
    }

    private fun requestDefaultSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            ) {
                requestRoleLauncher.launch(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS),
                )
            }
        } else {
            startActivity(
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName),
            )
        }
    }

    companion object {
        /** Fil à ouvrir depuis une notification. */
        const val EXTRA_OPEN_THREAD = "open_thread"
    }
}
