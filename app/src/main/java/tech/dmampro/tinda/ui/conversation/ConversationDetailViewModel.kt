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
import tech.dmampro.tinda.data.SimAssignmentRepository
import tech.dmampro.tinda.data.FavoriteRepository
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.local.Draft
import tech.dmampro.tinda.data.local.Message
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.sms.ScheduledSmsManager
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
    val isFavorite: Boolean = false,
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
    private val scheduler: ScheduledSmsManager,
    private val simAssignment: SimAssignmentRepository,
    private val favoriteRepository: FavoriteRepository,
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
            // Priorité : attribution par contact (Stitch 2.1), puis par fil.
            val byContact = simAssignment.getForAddress(address)
            val selected = byContact ?: simRepository.getSimForThread(threadId)
            val draft = db.draftDao().get(draftKey())?.text.orEmpty()
            val isFav = favoriteRepository.isFavorite(address)
            _uiState.update {
                it.copy(
                    displayName = name.ifBlank { address },
                    sims = sims,
                    selectedSimId = selected,
                    ready = true,
                    isFavorite = isFav,
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
            // Mémorise aussi par contact (Stitch 2.1).
            val address = repository.getThreadAddress(threadId)
            if (address.isNotBlank()) {
                simAssignment.setForAddress(address, subscriptionId)
            }
            _uiState.update { it.copy(selectedSimId = subscriptionId) }
        }
    }

    /** Bascule le statut favori du contact (maquette Stitch). */
    fun toggleFavorite() {
        viewModelScope.launch {
            val address = repository.getThreadAddress(threadId)
            if (address.isBlank()) return@launch
            val isFav = favoriteRepository.toggleFavorite(address)
            _uiState.update { it.copy(isFavorite = isFav) }
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

    /** Programme le brouillon comme SMS différé (F9). */
    fun scheduleMessage(text: String, atMillis: Long) {
        val clean = text.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            try {
                val address = repository.getThreadAddress(threadId)
                if (address.isBlank() || !repository.isDefaultSmsApp()) {
                    _uiState.update { it.copy(sendError = true) }
                    return@launch
                }
                scheduler.schedule(
                    address = address,
                    body = clean,
                    scheduledAt = atMillis,
                    subscriptionId = _uiState.value.selectedSimId,
                )
                db.draftDao().delete(draftKey())
                draftText.value = ""
            } catch (e: Exception) {
                _uiState.update { it.copy(sendError = true) }
            }
        }
    }

    private fun draftKey() = "thread:$threadId"
}
