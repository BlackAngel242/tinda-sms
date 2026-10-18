package tech.dmampro.tinda.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import tech.dmampro.tinda.R

/**
 * Onboarding en 2 écrans (US-012) : présentation, puis rôle SMS par défaut
 * et autorisations. En cas de refus, on explique les limites sans bloquer.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onRequestDefaultRole: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var page by remember { mutableIntStateOf(0) }
    var skipped by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    // Le dialogue du rôle est externe : on revérifie à chaque retour.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                skipped -> RefusedContent(
                    onRetry = { skipped = false; page = 1 },
                    onContinue = { viewModel.markDone(); onFinish() },
                )
                page == 0 -> WelcomeContent(onContinue = { page = 1 })
                else -> RoleContent(
                    state = state,
                    onRequestRole = onRequestDefaultRole,
                    onGrantPermissions = {
                        permissionLauncher.launch(OnboardingViewModel.requiredPermissions())
                    },
                    onSkip = { skipped = true },
                    onFinish = { viewModel.markDone(); onFinish() },
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(onContinue: () -> Unit) {
    Icon(
        Icons.Filled.Textsms,
        contentDescription = null,
        modifier = Modifier.padding(bottom = 16.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(
        stringResource(R.string.onboarding_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.onboarding_desc),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Bullet(stringResource(R.string.onboarding_bullet_1))
    Bullet(stringResource(R.string.onboarding_bullet_2))
    Bullet(stringResource(R.string.onboarding_bullet_3))
    Spacer(Modifier.height(32.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_continue))
    }
}

@Composable
private fun Bullet(text: String) {
    ListItem(
        headlineContent = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}

@Composable
private fun RoleContent(
    state: OnboardingState,
    onRequestRole: () -> Unit,
    onGrantPermissions: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    Text(
        stringResource(R.string.onboarding_role_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.onboarding_role_desc),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))

    StepRow(stringResource(R.string.onboarding_step_role), state.isDefaultSmsApp)
    StepRow(stringResource(R.string.onboarding_step_permissions), state.smsGranted)

    Spacer(Modifier.height(24.dp))
    if (!state.isDefaultSmsApp) {
        Button(onClick = onRequestRole, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_set_default))
        }
        Spacer(Modifier.height(8.dp))
    }
    if (!state.smsGranted) {
        OutlinedButton(onClick = onGrantPermissions, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_grant_permissions))
        }
        Spacer(Modifier.height(8.dp))
    }
    if (state.telephonyReady) {
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_finish))
        }
        Spacer(Modifier.height(8.dp))
    }
    TextButton(onClick = onSkip) {
        Text(stringResource(R.string.onboarding_skip))
    }
}

@Composable
private fun StepRow(label: String, done: Boolean) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun RefusedContent(onRetry: () -> Unit, onContinue: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.onboarding_refused_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_refused_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.onboarding_retry))
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onContinue) {
        Text(stringResource(R.string.onboarding_continue_anyway))
    }
}
