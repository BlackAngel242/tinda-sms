package tech.dmampro.tinda.ui.conversations

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Élément affiché dans la liste (lot 0 : structure ; données réelles au lot 1). */
data class ConversationItem(
    val threadId: Long,
    val displayName: String,
    val snippet: String,
    val date: Long,
    val unread: Boolean,
)

@HiltViewModel
class ConversationListViewModel @Inject constructor() : ViewModel() {

    // Lot 1 : alimenter depuis le provider SMS système via un repository.
    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()
}
