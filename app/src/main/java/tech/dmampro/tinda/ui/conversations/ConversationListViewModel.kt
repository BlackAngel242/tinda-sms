package tech.dmampro.tinda.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.SmsRepository
import javax.inject.Inject

/** Élément affiché dans la liste des conversations. */
data class ConversationItem(
    val threadId: Long,
    val displayName: String,
    val snippet: String,
    val date: Long,
    val unread: Boolean,
)

/** Liste des conversations (lot 1 : vraies données du provider SMS). */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: SmsRepository,
) : ViewModel() {

    val conversations: StateFlow<List<ConversationItem>> =
        repository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isDefaultSmsApp = MutableStateFlow(true)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp

    init {
        refresh()
    }

    /** À appeler à chaque retour au premier plan : recharge depuis le provider. */
    fun refresh() {
        viewModelScope.launch {
            repository.syncConversations()
            _isDefaultSmsApp.update { repository.isDefaultSmsApp() }
        }
    }
}
