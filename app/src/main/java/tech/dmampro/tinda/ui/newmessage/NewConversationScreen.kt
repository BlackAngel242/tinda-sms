package tech.dmampro.tinda.ui.newmessage

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.ContactSuggestion
import tech.dmampro.tinda.data.ContactsSearcher
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.ui.components.SimChoiceControl
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.SendSoundPlayer
import javax.inject.Inject

/** Ce que la zone sous le champ destinataire doit montrer. */
enum class SuggestionUiState {
    /** Rien : saisie vide, choix fait, ou recherche en cours. */
    HIDDEN,

    /** Des contacts correspondent à la saisie. */
    RESULTS,

    /** Recherche faite, aucun contact ne correspond. */
    EMPTY,

    /** Permission contacts refusée : saisie manuelle seulement. */
    DENIED,

    /** Contact choisi au sélecteur natif, mais sans aucun numéro. */
    PICKED_NO_NUMBER,
}

/** État de l'écran de nouveau message. */
data class NewMessageUiState(
    val sims: List<SimInfo> = emptyList(),
    val selectedSimId: Int = -1,
    val sending: Boolean = false,
    val addressError: Boolean = false,
    val sendError: Boolean = false,
    val drafts: List<tech.dmampro.tinda.data.local.Draft> = emptyList(),
    /** Texte du champ destinataire (saisie manuelle ou numéro choisi). */
    val recipient: String = "",
    /** Suggestions de contacts pour la saisie en cours. */
    val suggestions: List<ContactSuggestion> = emptyList(),
    val suggestionState: SuggestionUiState = SuggestionUiState.HIDDEN,
    /** Vrai si l'accès aux contacts est accordé (null = pas encore su). */
    val contactsPermission: Boolean? = null,
    /**
     * Un contact a été choisi au sélecteur mais aucun numéro n'a pu
     * être résolu : l'envoi est bloqué tant que le champ n'est pas
     * corrigé à la main, pour ne jamais envoyer au texte tapé en le
     * faisant passer pour le numéro de ce contact.
     */
    val pickedContactUnresolved: Boolean = false,
)

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val repository: SmsRepository,
    private val sender: SmsSender,
    private val simRepository: SimRepository,
    private val soundPlayer: SendSoundPlayer,
    private val db: tech.dmampro.tinda.data.local.TindaDatabase,
    private val contactsSearcher: ContactsSearcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewMessageUiState())
    val uiState: StateFlow<NewMessageUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

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
        // Brouillons (maquette Stitch : "Brouillons (N)").
        viewModelScope.launch {
            db.draftDao().observeAll().collect { drafts ->
                _uiState.update { it.copy(drafts = drafts) }
            }
        }
    }

    fun onSimSelected(subscriptionId: Int) {
        _uiState.update { it.copy(selectedSimId = subscriptionId) }
    }

    fun setContactsPermission(granted: Boolean) {
        _uiState.update { it.copy(contactsPermission = granted) }
        // Relance la recherche pour la saisie déjà présente.
        onRecipientChange(_uiState.value.recipient, keepText = true)
    }

    /**
     * Saisie du destinataire : met à jour le champ et programme une
     * recherche de contacts. Le travail précédent est annulé à chaque
     * frappe et le résultat n'est publié que si la saisie n'a pas
     * changé entre-temps : une ancienne recherche ne peut jamais
     * remplacer une plus récente.
     */
    fun onRecipientChange(text: String, keepText: Boolean = false) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                recipient = text,
                addressError = false,
                pickedContactUnresolved = if (keepText) it.pickedContactUnresolved else false,
                suggestionState = SuggestionUiState.HIDDEN,
                suggestions = emptyList(),
            )
        }
        if (text.isBlank()) return
        if (_uiState.value.contactsPermission == false) {
            _uiState.update { it.copy(suggestionState = SuggestionUiState.DENIED) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(200)
            val results = contactsSearcher.search(text)
            if (_uiState.value.recipient != text) return@launch
            _uiState.update {
                it.copy(
                    suggestions = results,
                    suggestionState = if (results.isEmpty()) {
                        SuggestionUiState.EMPTY
                    } else {
                        SuggestionUiState.RESULTS
                    },
                )
            }
        }
    }

    /** Choix explicite d'une suggestion : le champ prend le numéro du contact. */
    fun onSuggestionPicked(suggestion: ContactSuggestion) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                recipient = suggestion.number,
                suggestions = emptyList(),
                suggestionState = SuggestionUiState.HIDDEN,
                addressError = false,
                pickedContactUnresolved = false,
            )
        }
    }

    /**
     * Contact rendu par le sélecteur natif : ses numéros sont relus
     * depuis le fournisseur (l'extraction directe depuis l'URI est
     * fragile et muette quand le contact a plusieurs numéros).
     * Zéro numéro : état explicite et envoi bloqué. Un numéro : champ
     * rempli. Plusieurs : choix explicite dans la liste.
     */
    fun onContactPicked(uri: android.net.Uri) {
        viewModelScope.launch {
            val contactId = contactsSearcher.contactIdFor(uri) ?: return@launch
            val numbers = contactsSearcher.numbersForContact(contactId)
            when {
                numbers.isEmpty() -> _uiState.update {
                    it.copy(
                        suggestions = emptyList(),
                        suggestionState = SuggestionUiState.PICKED_NO_NUMBER,
                        pickedContactUnresolved = true,
                    )
                }
                numbers.size == 1 -> onSuggestionPicked(numbers.first())
                else -> _uiState.update {
                    it.copy(
                        suggestions = numbers,
                        suggestionState = SuggestionUiState.RESULTS,
                    )
                }
            }
        }
    }

    fun send(body: String, onThreadOpened: (Long) -> Unit) {
        val cleanAddress = _uiState.value.recipient.trim()
        if (_uiState.value.pickedContactUnresolved) {
            // Le contact choisi n'a pas de numéro résolu : on n'envoie
            // pas au texte tapé en le faisant passer pour ce contact.
            return
        }
        if (cleanAddress.isBlank() || body.isBlank()) {
            _uiState.update { it.copy(addressError = cleanAddress.isBlank()) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, sendError = false, addressError = false) }
            try {
                if (!repository.isDefaultSmsApp()) {
                    SendJournal.log("nouveau message : envoi bloqué (pas appli par défaut)")
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

/** Nouveau message : destinataire cherchable, SIM compacte, texte, envoi (US-001). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    onBack: () -> Unit,
    onThreadOpened: (Long) -> Unit,
    viewModel: NewConversationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var body by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Permission contacts : état initial, puis résultat de la demande.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setContactsPermission(granted) }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setContactsPermission(granted)
    }

    // Sélecteur de contact natif : le ViewModel résout les numéros.
    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) viewModel.onContactPicked(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "TINDA SMS",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            stringResource(R.string.new_conversation_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            // Espacement resserré : fini l'espace mort entre les blocs.
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Destinataire : la saisie cherche les contacts en direct.
            OutlinedTextField(
                value = uiState.recipient,
                onValueChange = { viewModel.onRecipientChange(it) },
                label = { Text(stringResource(R.string.recipient_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                isError = uiState.addressError,
                supportingText = {
                    if (uiState.addressError) Text(stringResource(R.string.recipient_error))
                },
                trailingIcon = {
                    IconButton(onClick = { pickContact.launch(null) }) {
                        Icon(
                            Icons.Filled.Contacts,
                            contentDescription = stringResource(R.string.groupsend_pick_contacts),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = tindaTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            RecipientSuggestions(
                state = uiState.suggestionState,
                suggestions = uiState.suggestions,
                query = uiState.recipient,
                onPick = { viewModel.onSuggestionPicked(it) },
                onAllowClick = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
            )
            // SIM du prochain envoi : contrôle compact à choix explicites.
            if (uiState.sims.size > 1) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.sim_choose),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    SimChoiceControl(
                        sims = uiState.sims,
                        selectedSimId = uiState.selectedSimId,
                        onSelect = { viewModel.onSimSelected(it) },
                    )
                }
            }
            // Message : hauteur contenue, pas d'étirement artificiel.
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.message_hint)) },
                shape = RoundedCornerShape(16.dp),
                colors = tindaTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp),
            )
            if (uiState.sendError) {
                Text(
                    stringResource(R.string.status_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            // Envoi : bouton teal pleine largeur.
            Button(
                onClick = { viewModel.send(body, onThreadOpened) },
                enabled = !uiState.sending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContentColor = MaterialTheme.colorScheme.outline,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    stringResource(R.string.send),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Zone de résultats sous le champ destinataire : contacts, vide, refus, sans numéro. */
@Composable
private fun RecipientSuggestions(
    state: SuggestionUiState,
    suggestions: List<ContactSuggestion>,
    query: String,
    onPick: (ContactSuggestion) -> Unit,
    onAllowClick: () -> Unit,
) {
    when (state) {
        SuggestionUiState.HIDDEN -> Unit
        SuggestionUiState.RESULTS -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
            ) {
                items(suggestions, key = { "${it.contactId}:${it.number}" }) { suggestion ->
                    val description = "${suggestion.displayName}, ${suggestion.number}"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(suggestion) }
                            .semantics { contentDescription = description }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    ) {
                        Text(
                            suggestion.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                suggestion.number,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            suggestion.typeLabel?.let { type ->
                                Text(
                                    type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        }
        SuggestionUiState.EMPTY -> Text(
            stringResource(R.string.contacts_no_result, query.trim()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        SuggestionUiState.DENIED -> Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                stringResource(R.string.contacts_permission_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAllowClick) {
                Text(stringResource(R.string.contacts_allow))
            }
        }
        SuggestionUiState.PICKED_NO_NUMBER -> Text(
            stringResource(R.string.contacts_no_number),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** Couleurs de champ du thème pour les OutlinedTextField. */
@Composable
private fun tindaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
)
