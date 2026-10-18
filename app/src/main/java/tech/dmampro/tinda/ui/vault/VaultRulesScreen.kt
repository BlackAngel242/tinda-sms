package tech.dmampro.tinda.ui.vault

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import tech.dmampro.tinda.data.VaultRepository
import tech.dmampro.tinda.data.local.SecretRule

@HiltViewModel
class VaultRulesViewModel @Inject constructor(
    private val vault: VaultRepository,
) : ViewModel() {
    val rules = vault.observeRules()

    fun addNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) { vault.addNumberRule(number) }
    }

    fun addKeyword(keyword: String) {
        viewModelScope.launch(Dispatchers.IO) { vault.addKeywordRule(keyword) }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { vault.deleteRule(id) }
    }

    suspend fun getDisguise(): String =
        withContext(Dispatchers.IO) { vault.getDisguisePhrase() }

    fun setDisguise(phrase: String) {
        viewModelScope.launch(Dispatchers.IO) { vault.setDisguisePhrase(phrase) }
    }
}

/**
 * Liste jaune : numéros et mots-clés dont les SMS entrants vont au coffre.
 * Plus la phrase de reconnaissance de la notification déguisée.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultRulesScreen(
    onBack: () -> Unit,
    viewModel: VaultRulesViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var addIsNumber by remember { mutableStateOf(true) }
    var addValue by remember { mutableStateOf("") }
    var disguise by remember { mutableStateOf<String?>(null) }
    var disguiseDraft by remember { mutableStateOf("") }
    var disguiseEditing by remember { mutableStateOf(false) }

    // Charge la phrase actuelle une fois.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val p = viewModel.getDisguise()
        disguise = p
        disguiseDraft = p
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_rules)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.vault_rules_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(rules, key = { it.id }) { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (rule.type == VaultRepository.RULE_NUMBER) {
                                stringResource(R.string.vault_rule_number)
                            } else {
                                stringResource(R.string.vault_rule_keyword)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(rule.value, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.vault_disguise_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.vault_disguise_explain),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (disguiseEditing) {
                    OutlinedTextField(
                        value = disguiseDraft,
                        onValueChange = { disguiseDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.setDisguise(disguiseDraft)
                                disguise = disguiseDraft.ifBlank { disguise }
                                disguiseEditing = false
                            },
                        ) { Text(stringResource(R.string.ok)) }
                        TextButton(onClick = {
                            disguiseDraft = disguise.orEmpty()
                            disguiseEditing = false
                        }) { Text(stringResource(R.string.cancel)) }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            disguise ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { disguiseEditing = true }) {
                            Text(stringResource(R.string.edit))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.vault_add_rule)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addIsNumber,
                            onClick = { addIsNumber = true },
                            label = { Text(stringResource(R.string.vault_rule_number)) },
                        )
                        FilterChip(
                            selected = !addIsNumber,
                            onClick = { addIsNumber = false },
                            label = { Text(stringResource(R.string.vault_rule_keyword)) },
                        )
                    }
                    OutlinedTextField(
                        value = addValue,
                        onValueChange = { addValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (addIsNumber) stringResource(R.string.vault_number_hint)
                                else stringResource(R.string.vault_keyword_hint),
                            )
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (addIsNumber) viewModel.addNumber(addValue)
                        else viewModel.addKeyword(addValue)
                        addValue = ""
                        showAdd = false
                    },
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
