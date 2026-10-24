package tech.dmampro.tinda.ui.newmessage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.SendSoundPlayer
import javax.inject.Inject

/** État de l'écran de nouveau message. */
data class NewMessageUiState(
    val sims: List<SimInfo> = emptyList(),
    val selectedSimId: Int = -1,
    val sending: Boolean = false,
    val addressError: Boolean = false,
    val sendError: Boolean = false,
    val drafts: List<tech.dmampro.tinda.data.local.Draft> = emptyList(),
)

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val repository: SmsRepository,
    private val sender: SmsSender,
    private val simRepository: SimRepository,
    private val soundPlayer: SendSoundPlayer,
    private val db: tech.dmampro.tinda.data.local.TindaDatabase,
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
    val context = LocalContext.current

    // Sélecteur de contact natif : remplit le destinataire.
    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        address = readContactNumber(context, uri) ?: address
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
            // Destinataire.
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
            // SIM : pastilles neutres, libellé explicite.
            if (uiState.sims.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sim_choose),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    uiState.sims.forEachIndexed { index, sim ->
                        // Pastille neutre, libellé explicite « SIM n nom ».
                        val simColor = MaterialTheme.colorScheme.onSurfaceVariant
                        FilterChip(
                            selected = sim.subscriptionId == uiState.selectedSimId,
                            onClick = { viewModel.onSimSelected(sim.subscriptionId) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(simColor),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "SIM ${index + 1} ${sim.displayName}",
                                        color = if (sim.subscriptionId == uiState.selectedSimId)
                                            MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = null,
                        )
                    }
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
                onClick = { viewModel.send(address, body, onThreadOpened) },
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

/**
 * Lit le numéro de téléphone d'un contact choisi via le sélecteur natif.
 * Retourne null si aucun numéro trouvé (contact sans numéro, par exemple).
 */
private fun readContactNumber(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.trim()?.takeIf { it.isNotBlank() }
            } else {
                // Le sélecteur a renvoyé le contact sans ses numéros :
                // on cherche le premier numéro de ce contact.
                val contactId = uri.lastPathSegment ?: return null
                context.contentResolver.query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                    arrayOf(contactId), null,
                )?.use { phones ->
                    if (phones.moveToFirst()) phones.getString(0)?.trim() else null
                }
            }
        }
    } catch (e: SecurityException) {
        null
    }
}
