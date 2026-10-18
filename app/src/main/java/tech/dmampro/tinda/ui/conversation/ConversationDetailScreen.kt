package tech.dmampro.tinda.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.MessageStatus
import tech.dmampro.tinda.util.SmsEncoding
import tech.dmampro.tinda.util.TimeFormat

/**
 * Fil de discussion (lot 1) : bulles, zone d'envoi, compteur de segments,
 * statut de livraison (DEC-008, RB-05 : "livré", jamais "lu" en SMS pur),
 * badge SIM visible et choix de la SIM par conversation (US-002).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    threadId: Long,
    onBack: () -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val draft by viewModel.draftText.collectAsState()
    var simMenuOpen by remember { mutableStateOf(false) }
    var failedMessage by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    // Toujours scroller vers le dernier message.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val segmentInfo = remember(draft) { SmsEncoding.segmentInfo(draft) }
    val selectedSimName = uiState.sims.firstOrNull { it.subscriptionId == uiState.selectedSimId }
        ?.displayName

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        if (selectedSimName != null) {
                            Text(
                                selectedSimName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (uiState.sims.size > 1) {
                        Box {
                            AssistChip(
                                onClick = { simMenuOpen = true },
                                label = { Text(selectedSimName ?: "") },
                            )
                            DropdownMenu(
                                expanded = simMenuOpen,
                                onDismissRequest = { simMenuOpen = false },
                            ) {
                                Text(
                                    stringResource(R.string.sim_choose),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                                uiState.sims.forEach { sim ->
                                    DropdownMenuItem(
                                        text = { Text(sim.displayName) },
                                        onClick = {
                                            viewModel.onSimSelected(sim.subscriptionId)
                                            simMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Le champ de saisie doit rester au-dessus de la barre de
            // navigation système (gestes ou boutons) et remonter
            // au-dessus du clavier quand il s'ouvre.
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(8.dp)
            ) {
                if (uiState.sendError) {
                    Text(
                        stringResource(R.string.status_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = viewModel::onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.message_hint)) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                    )
                    IconButton(
                        onClick = viewModel::send,
                        enabled = draft.isNotBlank() && !uiState.sending,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (segmentInfo.parts > 1 || draft.isNotEmpty()) {
                    Text(
                        stringResource(
                            R.string.segments,
                            segmentInfo.remaining,
                            if (segmentInfo.encoding == SmsEncoding.Encoding.GSM_7) 160 else 70,
                            segmentInfo.parts,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        if (!uiState.ready) {
            Box(Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onFailedClick = { failedMessage = it },
                    )
                }
            }
        }
    }
    failedMessage?.let { message ->
        FailureDialog(
            message = message,
            onDismiss = { failedMessage = null },
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onFailedClick: (Message) -> Unit,
) {
    val isIncoming = message.incoming
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isIncoming) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .let {
                        if (!isIncoming && message.status == MessageStatus.FAILED) {
                            it.clickable { onFailedClick(message) }
                        } else {
                            it
                        }
                    },
            ) {
                Text(
                    message.body,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        TimeFormat.shortDate(message.date, stringResource(R.string.yesterday)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isIncoming) {
                        StatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

/** Dialogue de diagnostic : pourquoi ce message est en échec (toucher le ⚠️). */
@Composable
private fun FailureDialog(
    message: Message,
    onDismiss: () -> Unit,
) {
    val causeRes = when (message.failureCode) {
        1 -> R.string.send_fail_generic
        2 -> R.string.send_fail_no_service
        3 -> R.string.send_fail_invalid
        4 -> R.string.send_fail_radio_off
        -1 -> R.string.send_fail_app
        else -> R.string.send_fail_unknown
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_fail_title)) },
        text = { Text(stringResource(causeRes)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

/** Icône de statut : envoi, envoyé, livré, échec. Jamais "lu" en SMS pur (RB-05). */
@Composable
private fun StatusIcon(status: MessageStatus) {
    val (icon, tint, description) = when (status) {
        MessageStatus.SENDING -> Triple(
            Icons.Filled.Schedule,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.status_sending,
        )
        MessageStatus.SENT -> Triple(
            Icons.Filled.Check,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.status_sent,
        )
        MessageStatus.DELIVERED -> Triple(
            Icons.Filled.DoneAll,
            MaterialTheme.colorScheme.primary,
            R.string.status_delivered,
        )
        MessageStatus.FAILED -> Triple(
            Icons.Filled.ErrorOutline,
            MaterialTheme.colorScheme.error,
            R.string.status_failed,
        )
        MessageStatus.RECEIVED -> return
    }
    Icon(
        icon,
        contentDescription = stringResource(description),
        tint = tint,
        modifier = Modifier.padding(start = 4.dp),
    )
}
