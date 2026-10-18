package tech.dmampro.tinda.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.VaultRepository
import tech.dmampro.tinda.data.local.VaultMessage
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.util.TimeFormat

@HiltViewModel
class VaultConversationViewModel @Inject constructor(
    private val vault: VaultRepository,
    private val sender: SmsSender,
    private val smsRepository: SmsRepository,
) : ViewModel() {
    fun messages(address: String) = vault.observeForAddress(address)

    fun markRead(address: String) {
        viewModelScope.launch(Dispatchers.IO) { vault.markAddressRead(address) }
    }

    /**
     * Réponse depuis le coffre : envoyée par radio mais JAMAIS écrite dans
     * le provider système (RB-01). Seule la base chiffrée garde la trace.
     */
    fun sendVaultReply(address: String, body: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    sender.sendVault(address, body)
                    vault.storeOutgoing(address, body, System.currentTimeMillis())
                    true
                } catch (_: Exception) {
                    false
                }
            }
            onDone(ok)
        }
    }

    suspend fun deleteThread(address: String) =
        withContext(Dispatchers.IO) { vault.deleteAddress(address) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultConversationScreen(
    address: String,
    onBack: () -> Unit,
    viewModel: VaultConversationViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages(address).collectAsState(initial = emptyList())
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(address) { viewModel.markRead(address) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(maskAddress(address)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.message_hint)) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isBlank() || sending) return@IconButton
                        sending = true
                        viewModel.sendVaultReply(address, text) { sending = false }
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && !sending,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                VaultBubble(message = message)
            }
        }
    }
}

@Composable
private fun VaultBubble(message: VaultMessage) {
    val isIncoming = message.incoming
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isIncoming) 4.dp else 18.dp,
                bottomEnd = if (isIncoming) 18.dp else 4.dp,
            ),
            color = if (isIncoming) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    TimeFormat.shortDate(message.date, "…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private fun maskAddress(address: String): String {
    val digits = address.filter { it.isDigit() }
    return if (digits.length >= 2) "••• " + digits.takeLast(2) else "•••"
}
