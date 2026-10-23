package tech.dmampro.tinda.ui.automations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dmampro.tinda.R
import tech.dmampro.tinda.ui.components.TindaSectionTitle
import tech.dmampro.tinda.ui.components.TindaSettingsRow

/**
 * Écran hôte Automatisations (lot 1) : regroupe les entrées vers les
 * moteurs existants (programmation, répondeur, transfert, envoi
 * groupé, suppression automatique) sans les réécrire.
 */
@Composable
fun AutomationsScreen(
    onOpenScheduled: () -> Unit,
    onOpenAutoreply: () -> Unit,
    onOpenForward: () -> Unit,
    onOpenGroupSend: () -> Unit,
    onOpenAutodelete: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        "TINDA SMS",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        stringResource(R.string.automations_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.automations_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            item { TindaSectionTitle(stringResource(R.string.settings_section_automation)) }
            item {
                TindaSettingsRow(
                    icon = Icons.Filled.Schedule,
                    title = stringResource(R.string.settings_scheduled),
                    subtitle = stringResource(R.string.settings_scheduled_desc),
                    onClick = onOpenScheduled,
                )
            }
            item {
                TindaSettingsRow(
                    icon = Icons.Filled.SmartToy,
                    title = stringResource(R.string.settings_autoreply),
                    subtitle = stringResource(R.string.settings_autoreply_desc),
                    onClick = onOpenAutoreply,
                )
            }
            item {
                TindaSettingsRow(
                    icon = Icons.Filled.Forward,
                    title = stringResource(R.string.settings_forward),
                    subtitle = stringResource(R.string.settings_forward_desc),
                    onClick = onOpenForward,
                )
            }
            item {
                TindaSettingsRow(
                    icon = Icons.Filled.Group,
                    title = stringResource(R.string.groupsend_title),
                    subtitle = stringResource(R.string.automations_groupsend_desc),
                    onClick = onOpenGroupSend,
                )
            }
            item {
                TindaSettingsRow(
                    icon = Icons.Filled.DeleteSweep,
                    title = stringResource(R.string.settings_autodelete),
                    subtitle = stringResource(R.string.settings_autodelete_desc),
                    onClick = onOpenAutodelete,
                )
            }
        }
    }
}
