package tech.dmampro.tinda.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.data.PrefsRepository
import tech.dmampro.tinda.data.SearchRepository
import tech.dmampro.tinda.data.SearchResultItem
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.SwipeAction
import javax.inject.Inject

/** Élément affiché dans la liste des conversations. */
data class ConversationItem(
    val threadId: Long,
    val address: String,
    val displayName: String,
    val snippet: String,
    val date: Long,
    val unreadCount: Int,
    val subscriptionId: Int?,
)

/** Liste des conversations (lot 1 : vraies données ; lot 2 : recherche, gestes). */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: SmsRepository,
    private val searchRepository: SearchRepository,
    private val prefs: PrefsRepository,
    private val simRepository: SimRepository,
) : ViewModel() {

    val conversations: StateFlow<List<ConversationItem>> =
        repository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isDefaultSmsApp = MutableStateFlow(true)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp

    // ---- Double SIM : infos et filtre (design Radiant Slate) ----

    private val _sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val sims: StateFlow<List<SimInfo>> = _sims.asStateFlow()

    /** Filtre SIM actif : subscriptionId, ou null pour "Toutes". */
    private val _simFilter = MutableStateFlow<Int?>(null)
    val simFilter: StateFlow<Int?> = _simFilter.asStateFlow()

    val filteredConversations: StateFlow<List<ConversationItem>> =
        kotlinx.coroutines.flow.combine(conversations, _simFilter) { list, filter ->
            if (filter == null) list else list.filter { it.subscriptionId == filter }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSimFilter(subscriptionId: Int?) {
        _simFilter.value = subscriptionId
    }

    // ---- Recherche plein texte (F3) ----

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<SearchResultItem>> = _searchQuery
        .debounce(300)
        .mapLatest { query ->
            if (query.isBlank()) emptyList() else searchRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ---- Gestes swipe (F5) ----

    val swipeLeftAction: StateFlow<SwipeAction> = prefs.swipeLeftAction
        .map { runCatching { SwipeAction.valueOf(it) }.getOrDefault(SwipeAction.ARCHIVE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.ARCHIVE)

    val swipeRightAction: StateFlow<SwipeAction> = prefs.swipeRightAction
        .map { runCatching { SwipeAction.valueOf(it) }.getOrDefault(SwipeAction.REPLY) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwipeAction.REPLY)

    fun archiveThread(threadId: Long) {
        viewModelScope.launch { repository.setThreadArchived(threadId, true) }
    }

    fun deleteThread(threadId: Long) {
        viewModelScope.launch { repository.deleteThread(threadId) }
    }

    init {
        refresh()
    }

    /** À appeler à chaque retour au premier plan : recharge depuis le provider. */
    fun refresh() {
        viewModelScope.launch {
            repository.syncConversations()
            _isDefaultSmsApp.update { repository.isDefaultSmsApp() }
            _sims.update { simRepository.getActiveSims() }
        }
    }
}
