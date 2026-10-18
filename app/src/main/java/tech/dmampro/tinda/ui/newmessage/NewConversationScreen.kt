package tech.dmampro.tinda.ui.newmessage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.util.SendSoundPlayer
import javax.inject.Inject

/** État de l'écran de nouveau message. */
data class NewMessageUiState(
    val sims: List<SimInfo> = emptyList(),
    val selectedSimId: Int = -1,
    val sending: Boolean = false,
    val addressError: Boolean = false,
    val sendError: Boolean = false,
)

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val repository: SmsRepository,
    private val sender: SmsSender,
    private val simRepository: SimRepository,
    private val soundPlayer: SendSoundPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewMessageUiState())
    val uiState: StateFlow<NewMessageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sims = simRepository.getActiveSims()
            _uiState.update {
                it.copy(
                    sims = sims,
                    selectedSimId = simRepository.getDefaultSubscriptionId(),
                )
            }
        }
    }

    fun onSimSelected(subscriptionId: Int) {
        _uiState.update { it.copy(selectedSimId = subscriptionId) }
    }

    fun send(address: String, body: String, onThreadOpened: (Long) -> Unit) {
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(addressError = cleanAddress.isBlank()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, sendError = false, addressError = false) }
            try {
                if (!repository.isDefaultSmsApp()) {
                    _uiState.update { it.copy(sendError = true, sending = false) }
                    return@launch
                }
                val threadId = repository.getOrCreateThreadId(cleanAddress)
                if (threadId < 0) {
                    _uiState.update { it.copy(sendError = true, sending = false) }
                    return@launch
                }
                sender.send(cleanAddress, body, _uiState.value.selectedSimId, threadId)
                soundPlayer.playIfEnabled()
                onThreadOpened(threadId)
            } catch (e: Exception) {
                _uiState.update { it.copy(sendError = true) }
            } finally {
                _uiState.update { it.copy(sending = false) }
            }
        }
    }
}

/** Nouveau message : destinataire, SIM visible, texte, envoi (US-001). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    onBack: () -> Unit,
    onThreadOpened: (Long) -> Unit,
    viewModel: NewConversationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var address by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_conversation_title)) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.recipient_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = uiState.addressError,
                supportingText = {
                    if (uiState.addressError) Text(stringResource(R.string.recipient_error))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.sims.size > 1) {
                Text(
                    stringResource(R.string.sim_choose),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.sims.forEach { sim ->
                        FilterChip(
                            selected = sim.subscriptionId == uiState.selectedSimId,
                            onClick = { viewModel.onSimSelected(sim.subscriptionId) },
                            label = { Text(sim.displayName) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.message_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            if (uiState.sendError) {
                Text(
                    stringResource(R.string.status_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.send(address, body, onThreadOpened) },
                enabled = !uiState.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    stringResource(R.string.send),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
