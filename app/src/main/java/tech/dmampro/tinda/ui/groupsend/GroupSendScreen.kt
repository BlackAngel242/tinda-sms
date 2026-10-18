package tech.dmampro.tinda.ui.groupsend

import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SmsRepository
import tech.dmampro.tinda.data.SimRepository
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.sms.SmsSender
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.SendJournal
import tech.dmampro.tinda.util.SendSoundPlayer

/** Un destinataire d'envoi groupé (nom affiché + numéro). */
data class GroupRecipient(val name: String, val number: String)

data class GroupSendUiState(
    val sims: List<SimInfo> = emptyList(),
    val selectedSimId: Int = -1,
    val sending: Boolean = false,
    val sentCount: Int = 0,
)

@HiltViewModel
class GroupSendViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SmsRepository,
    private val sender: SmsSender,
    private val simRepository: SimRepository,
    private val soundPlayer: SendSoundPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSendUiState())
    val uiState: StateFlow<GroupSendUiState> = _uiState.asStateFlow()

    /** Destinataires choisis (numéro -> affichage). */
    val recipients = mutableStateListOf<GroupRecipient>()

    /** Résultat par numéro après envoi (true = parti). */
    val results = mutableStateMapOf<String, Boolean>()

    /** Carnet d'adresses, chargé à la demande. */
    var contacts by mutableStateOf<List<GroupRecipient>?>(null)
        private set

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

    fun addRecipient(name: String, number: String): Boolean {
        val clean = number.trim()
        if (clean.isBlank() || recipients.any { it.number == clean }) return false
        recipients.add(GroupRecipient(name.trim().ifBlank { clean }, clean))
        return true
    }

    fun removeRecipient(number: String) {
        recipients.removeAll { it.number == number }
        results.remove(number)
    }

    fun loadContacts() {
        if (contacts != null) return
        viewModelScope.launch {
            contacts = withContext(Dispatchers.IO) { readContacts() }
        }
    }

    private fun readContacts(): List<GroupRecipient> {
        val list = mutableListOf<GroupRecipient>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val nameIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numIdx).orEmpty().trim()
                    if (number.isBlank()) continue
                    val name = cursor.getString(nameIdx).orEmpty().trim().ifBlank { number }
                    list.add(GroupRecipient(name, number))
                }
            }
        } catch (e: SecurityException) {
            // Permission contacts refusée : saisie manuelle uniquement.
        }
        return list.distinctBy { it.number }
    }

    /** Envoie le message à chaque destinataire, séquentiellement. */
    fun sendAll(body: String) {
        val text = body.trim()
        if (text.isBlank() || recipients.isEmpty() || _uiState.value.sending) return
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true) }
            results.clear()
            var ok = 0
            try {
                if (!repository.isDefaultSmsApp()) {
                    SendJournal.log("envoi groupé : bloqué (pas appli par défaut)")
                    EventLog.log("groupsend.blocked")
                    return@launch
                }
                for (recipient in recipients.toList()) {
                    val sent = try {
                        val threadId = repository.getOrCreateThreadId(recipient.number)
                        if (threadId < 0) false
                        else {
                            sender.send(
                                recipient.number, text,
                                _uiState.value.selectedSimId, threadId,
                            )
                            true
                        }
                    } catch (e: Exception) {
                        false
                    }
                    results[recipient.number] = sent
                    if (sent) ok++
                }
                if (ok > 0) soundPlayer.playIfEnabled()
                EventLog.log("groupsend.sent", "$ok/${recipients.size}")
            } finally {
                _uiState.update { it.copy(sending = false, sentCount = ok) }
            }
        }
    }

    fun resetResults() {
        results.clear()
        _uiState.update { it.copy(sentCount = 0) }
    }
}

/** Envoi groupé (F14) : plusieurs destinataires, suivi par numéro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSendScreen(
    onBack: () -> Unit,
    viewModel: GroupSendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var body by remember { mutableStateOf("") }
    var manualNumber by remember { mutableStateOf("") }
    var showContacts by remember { mutableStateOf(false) }
    var contactQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.groupsend_title)) },
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
            Text(
                stringResource(R.string.groupsend_recipients, viewModel.recipients.size),
                style = MaterialTheme.typography.titleSmall,
            )
            if (viewModel.recipients.isEmpty()) {
                Text(
                    stringResource(R.string.groupsend_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(viewModel.recipients, key = { it.number }) { recipient ->
                        val result = viewModel.results[recipient.number]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    recipient.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    recipient.number,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (result != null) {
                                Icon(
                                    if (result) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = if (result) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeRecipient(recipient.number) },
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = manualNumber,
                    onValueChange = { manualNumber = it },
                    label = { Text(stringResource(R.string.recipient_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (viewModel.addRecipient("", manualNumber)) {
                            manualNumber = ""
                            viewModel.resetResults()
                        }
                    },
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = stringResource(R.string.add),
                    )
                }
                IconButton(
                    onClick = {
                        viewModel.loadContacts()
                        showContacts = true
                    },
                ) {
                    Icon(
                        Icons.Filled.Contacts,
                        contentDescription = stringResource(R.string.groupsend_pick_contacts),
                    )
                }
            }
            if (uiState.sims.size > 1) {
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
            if (uiState.sentCount > 0 || viewModel.results.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.groupsend_summary,
                        uiState.sentCount,
                        viewModel.recipients.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.sendAll(body) },
                enabled = !uiState.sending &&
                    body.isNotBlank() && viewModel.recipients.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(
                    stringResource(R.string.groupsend_send),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showContacts) {
        val allContacts = viewModel.contacts
        AlertDialog(
            onDismissRequest = { showContacts = false },
            title = { Text(stringResource(R.string.groupsend_pick_contacts)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = contactQuery,
                        onValueChange = { contactQuery = it },
                        label = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (allContacts == null) {
                        Text(stringResource(R.string.loading))
                    } else {
                        val filtered = allContacts.filter {
                            it.name.contains(contactQuery, ignoreCase = true) ||
                                it.number.contains(contactQuery)
                        }.take(200)
                        var selected by remember(contactQuery) {
                            mutableStateOf(
                                filtered.filter { c ->
                                    viewModel.recipients.any { it.number == c.number }
                                }.toSet(),
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(filtered, key = { it.number }) { contact ->
                                val checked = selected.contains(contact)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = if (checked) {
                                                selected - contact
                                            } else {
                                                selected + contact
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            contact.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            contact.number,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    selected.forEach {
                                        viewModel.addRecipient(it.name, it.number)
                                    }
                                    viewModel.resetResults()
                                    showContacts = false
                                },
                            ) {
                                Text(
                                    stringResource(
                                        R.string.groupsend_add_selected,
                                        selected.size,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContacts = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
