package tech.dmampro.tinda.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.PrefsRepository
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.Draft
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.SendSoundPlayer
import javax.inject.Inject

/** État de l'écran de fil de discussion. */
data class DetailUiState(
    val displayName: String = "",
    val sims: List<SimInfo> = emptyList(),
    val selectedSimId: Int = -1,
    val sending: Boolean = false,
    val sendError: Boolean = false,
    val ready: Boolean = false,
)

/** Fil de discussion : vraies données, envoi, SIM par conversation, brouillon auto. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SmsRepository,
    private val sender: SmsSender,
    private val simRepository: SimRepository,
    private val prefs: PrefsRepository,
    private val db: TindaDatabase,
    private val soundPlayer: SendSoundPlayer,
) : ViewModel() {

    private val threadId: Long = savedStateHandle.get<Long>("threadId") ?: -1L

    val messages: StateFlow<List<Message>> =
        repository.observeThreadMessages(threadId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState

    /** Texte en cours de saisie (brouillon auto). */
    val draftText = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.syncThread(threadId)
            repository.markThreadRead(threadId)
            val address = repository.getThreadAddress(threadId)
            val name = repository.resolveDisplayName(address)
            val sims = simRepository.getActiveSims()
            val selected = simRepository.getSimForThread(threadId)
            val draft = db.draftDao().get(draftKey())?.text.orEmpty()
            _uiState.update {
                it.copy(
                    displayName = name.ifBlank { address },
                    sims = sims,
                    selectedSimId = selected,
                    ready = true,
                )
            }
            draftText.value = draft
        }
        // Sauvegarde auto du brouillon (F1), 600 ms après la dernière frappe.
        viewModelScope.launch {
            draftText.drop(1).debounce(600).collect { text ->
                if (text.isBlank()) {
                    db.draftDao().delete(draftKey())
                } else {
                    db.draftDao().save(Draft(draftKey(), text, System.currentTimeMillis()))
                }
            }
        }
    }

    fun onSimSelected(subscriptionId: Int) {
        viewModelScope.launch {
            simRepository.setSimForThread(threadId, subscriptionId)
            _uiState.update { it.copy(selectedSimId = subscriptionId) }
        }
    }

    fun onDraftChange(text: String) {
        draftText.value = text
    }

    fun send() {
        val text = draftText.value.trim()
        val simId = _uiState.value.selectedSimId
        if (text.isBlank() || _uiState.value.sending) return
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, sendError = false) }
            try {
                val address = repository.getThreadAddress(threadId)
                if (address.isBlank() || !repository.isDefaultSmsApp()) {
                    SendJournal.log("écran : envoi bloqué (pas appli par défaut ou adresse vide)")
                    _uiState.update { it.copy(sendError = true, sending = false) }
                    return@launch
                }
                sender.send(address, text, simId, threadId, deliveryReport = true)
                soundPlayer.playIfEnabled()
                db.draftDao().delete(draftKey())
                draftText.value = ""
            } catch (e: Exception) {
                _uiState.update { it.copy(sendError = true) }
            } finally {
                _uiState.update { it.copy(sending = false) }
            }
        }
    }

    private fun draftKey() = "thread:$threadId"
}
