package tech.dmampro.tinda.ui.settings

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.util.EventLog

/**
 * Verrou des options avancées : biométrie si disponible, sinon code du coffre.
 * Appelé quand l'utilisateur touche "Options avancées" et que le coffre a un code.
 */
@Composable
fun AdvancedLockDialog(
    viewModel: SettingsViewModel,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var bioLaunched by remember { mutableStateOf(false) }
    val bioAvailable = remember { viewModel.isBiometricAvailable() }

    fun startBiometric() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val act = context as? FragmentActivity ?: return
        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(context.getString(R.string.settings_advanced_bio_title))
            .setNegativeButton(
                context.getString(R.string.vault_use_pin),
                executor,
                { _, _ -> },
            )
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    scope.launch {
                        busy = true
                        val ok = viewModel.unlockAdvancedWithBiometric()
                        busy = false
                        if (ok) onUnlocked()
                        else error = context.getString(R.string.vault_biometric_failed)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Annulation ou repli PIN : on reste sur le dialogue.
                    if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    ) {
                        error = errString.toString()
                    }
                }
            },
        )
    }

    // Biométrie proposée d'emblée si disponible.
    LaunchedEffect(bioAvailable) {
        if (bioAvailable && !bioLaunched) {
            bioLaunched = true
            startBiometric()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_advanced_locked_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.settings_advanced_locked_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.vault_enter_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (bioAvailable) {
                    TextButton(onClick = ::startBiometric) {
                        Text(stringResource(R.string.settings_advanced_use_biometric))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        val ok = viewModel.verifyAdvancedPin(pin)
                        busy = false
                        if (ok) onUnlocked()
                        else {
                            error = context.getString(R.string.vault_wrong_pin)
                            EventLog.log("settings.advanced_unlock_pin_fail")
                        }
                    }
                },
                enabled = pin.length >= 4 && !busy,
            ) {
                Text(stringResource(R.string.vault_unlock))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
