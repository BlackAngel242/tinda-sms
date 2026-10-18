package tech.dmampro.tinda

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
     * historique. Si Android bloque ou ignore la demande (fréquent pour une
     * app installée manuellement, paramètres restreints), on ne reste plus
     * silencieux : on guide vers le réglage manuel.
     */
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (!isDefaultSmsApp()) showRoleBlockedDialog()
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
            } else {
                // Avant ce correctif : rien ne se passait ici, d'où le bouton muet.
                showRoleBlockedDialog()
            }
        } else {
            startActivity(
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName),
            )
        }
    }

    /**
     * Secours quand la demande de rôle est bloquée : explique le réglage
     * manuel et ouvre directement l'écran des applications par défaut.
     */
    private fun showRoleBlockedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.role_blocked_title)
            .setMessage(R.string.role_blocked_message)
            .setPositiveButton(R.string.role_blocked_open_settings) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    // ROM sans cet écran : on retombe sur la fiche de l'application.
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        },
                    )
                }
            }
            .setNegativeButton(R.string.role_blocked_later, null)
            .show()
    }

    companion object {
        /** Fil à ouvrir depuis une notification. */
        const val EXTRA_OPEN_THREAD = "open_thread"
    }
}
