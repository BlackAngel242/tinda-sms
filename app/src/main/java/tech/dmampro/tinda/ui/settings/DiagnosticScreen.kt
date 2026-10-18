package tech.dmampro.tinda.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.R
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.RoleCheck
import tech.dmampro.tinda.util.RoleDiagnostic
import tech.dmampro.tinda.util.SendJournal

/**
 * Écran de diagnostic (paramètres avancés) : montre ce que le téléphone voit
 * réellement des composants SMS de Tinda. Utile quand l'app n'apparaît pas
 * dans la liste des applis SMS par défaut : si tout est vert ici, le
 * problème vient de la ROM, pas de l'application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var checks by remember { mutableStateOf<List<RoleCheck>?>(null) }
    var journal by remember { mutableStateOf<List<SendJournal.Entry>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventLog.Event>>(emptyList()) }

    LaunchedEffect(Unit) {
        checks = withContext(Dispatchers.Default) { RoleDiagnostic.run(context) }
        journal = withContext(Dispatchers.Default) { SendJournal.snapshot() }
        events = withContext(Dispatchers.Default) { EventLog.snapshot().takeLast(50) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostic_title)) },
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
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.diagnostic_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            val list = checks
            if (list == null) {
                Text(stringResource(R.string.diagnostic_loading))
            } else {
                LazyColumn {
                    items(list) { check ->
                        ListItem(
                            headlineContent = { Text(check.label) },
                            supportingContent = { Text(check.detail) },
                            trailingContent = {
                                Icon(
                                    if (check.ok) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (check.ok) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val allOk = list.dropLast(1).all { it.ok }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (allOk) {
                            stringResource(R.string.diagnostic_all_ok)
                        } else {
                            stringResource(R.string.diagnostic_not_ok)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.diagnostic_journal_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                if (journal.isEmpty()) {
                    Text(
                        stringResource(R.string.diagnostic_journal_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(journal) { entry ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        entry.event,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                leadingContent = {
                                    Text(
                                        SendJournal.formatTime(entry.time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.diagnostic_events_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.diagnostic_events_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { shareEventLog(context) }) {
                    Text(stringResource(R.string.diagnostic_events_export))
                }
                Spacer(Modifier.height(8.dp))
                if (events.isNotEmpty()) {
                    LazyColumn {
                        items(events) { event ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        event.name,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                supportingContent = if (event.detail.isNotBlank()) {
                                    {
                                        Text(
                                            event.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    null
                                },
                                leadingContent = {
                                    Text(
                                        SendJournal.formatTime(event.time),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Partage le fichier du journal d'événements (diagnostic à distance).
 * Le fichier ne contient ni contenu de SMS, ni numéro, ni PIN.
 */
private fun shareEventLog(context: android.content.Context) {
    val file = EventLog.file()
    if (file == null) {
        EventLog.log("eventlog.export_fail", "no_file")
        return
    }
    EventLog.log("eventlog.export")
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        android.content.Intent.createChooser(
            intent,
            context.getString(R.string.diagnostic_events_export),
        ),
    )
}
