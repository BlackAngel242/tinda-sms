package tech.dmampro.tinda.ui.vault

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.VaultRepository
import tech.dmampro.tinda.util.EventLog

/** État du déverrouillage. */
private enum class UnlockState { PIN_ENTRY, PIN_SETUP_1, PIN_SETUP_2 }

/** Déverrouillage du coffre : PIN et/ou biométrie (le PIN reste le repli). */
@HiltViewModel
class VaultUnlockViewModel @Inject constructor(
    private val vault: VaultRepository,
) : ViewModel() {
    val isSetup: Boolean get() = vault.isSetup()
    val biometricAvailable: Boolean get() = vault.isBiometricAvailable()

    suspend fun unlockWithPin(pin: String): Boolean =
        withContext(Dispatchers.IO) { vault.unlockWithPin(pin) }

    suspend fun unlockWithBiometric(): Boolean =
        withContext(Dispatchers.IO) { vault.unlockWithBiometric() }

    suspend fun setupPin(pin: String) =
        withContext(Dispatchers.IO) { vault.setupPin(pin) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(
    onUnlocked: () -> Unit,
    onBack: () -> Unit,
    viewModel: VaultUnlockViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(
            if (viewModel.isSetup) UnlockState.PIN_ENTRY else UnlockState.PIN_SETUP_1,
        )
    }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun tryUnlock(input: String) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            val ok = viewModel.unlockWithPin(input)
            busy = false
            EventLog.log(if (ok) "vault.unlock_pin_ok" else "vault.unlock_pin_fail")
            if (ok) {
                onUnlocked()
            } else {
                pin = ""
                error = context.getString(R.string.vault_wrong_pin)
            }
        }
    }

    fun trySetup(input: String) {
        if (busy) return
        when (state) {
            UnlockState.PIN_SETUP_1 -> {
                firstPin = input
                pin = ""
                state = UnlockState.PIN_SETUP_2
            }
            UnlockState.PIN_SETUP_2 -> {
                if (input != firstPin) {
                    pin = ""
                    firstPin = ""
                    state = UnlockState.PIN_SETUP_1
                    error = context.getString(R.string.vault_pin_mismatch)
                } else {
                    busy = true
                    scope.launch {
                        viewModel.setupPin(input)
                        busy = false
                        onUnlocked()
                    }
                }
            }
            else -> Unit
        }
    }

    fun onDigit(d: String) {
        if (pin.length >= 12) return
        error = null
        pin += d
    }

    fun onDelete() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    fun onValidate() {
        if (pin.length < 4) {
            error = context.getString(R.string.vault_pin_too_short)
            return
        }
        when (state) {
            UnlockState.PIN_ENTRY -> tryUnlock(pin)
            else -> trySetup(pin)
        }
    }

    /** Biométrie (API 29+, framework, sans dépendance). */
    fun startBiometric() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val act = activity ?: return
        val executor = Executors.newSingleThreadExecutor()
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(context.getString(R.string.vault_biometric_title))
            .setSubtitle(context.getString(R.string.vault_biometric_subtitle))
            .setNegativeButton(
                context.getString(R.string.cancel),
                executor,
                { _, _ -> },
            )
            .build()
        val cancel = CancellationSignal()
        prompt.authenticate(
            cancel,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    scope.launch {
                        busy = true
                        val ok = viewModel.unlockWithBiometric()
                        busy = false
                        EventLog.log(if (ok) "vault.unlock_bio_ok" else "vault.unlock_bio_fail")
                        if (ok) onUnlocked() else {
                            error = context.getString(R.string.vault_biometric_failed)
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // L'utilisateur a annulé : silence. Sinon, repli PIN.
                    if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                    ) {
                        error = errString.toString()
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                when (state) {
                    UnlockState.PIN_ENTRY -> stringResource(R.string.vault_enter_pin)
                    UnlockState.PIN_SETUP_1 -> stringResource(R.string.vault_create_pin)
                    UnlockState.PIN_SETUP_2 -> stringResource(R.string.vault_confirm_pin)
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            // Pastilles du PIN (jamais de chiffres affichés).
            Text(
                "●".repeat(pin.length) + "○".repeat((12 - pin.length).coerceAtLeast(0)),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Pavé numérique.
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(72.dp))
                        } else if (key == "⌫") {
                            OutlinedButton(
                                onClick = ::onDelete,
                                modifier = Modifier.size(72.dp),
                            ) { Text("⌫") }
                        } else {
                            Button(
                                onClick = { onDigit(key) },
                                modifier = Modifier.size(72.dp),
                            ) { Text(key, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = ::onValidate,
                enabled = pin.length >= 4 && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.ok))
            }
            if (state == UnlockState.PIN_ENTRY && viewModel.biometricAvailable) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = ::startBiometric,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.vault_use_biometric))
                }
            }
            if (state == UnlockState.PIN_SETUP_1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.vault_pin_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
