package tech.dmampro.tinda.ui.autodelete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import javax.inject.Inject
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.local.AutoDeleteRule
import tech.dmampro.tinda.sms.AutoDeleteManager

@HiltViewModel
class AutoDeleteViewModel @Inject constructor(
    private val manager: AutoDeleteManager,
) : ViewModel() {
    val rules = manager.observeAll()

    fun toggle(id: Long, enabled: Boolean) {
        viewModelScope.launch { manager.toggle(id, enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { manager.delete(id) }
    }

    fun save(rule: AutoDeleteRule) {
        viewModelScope.launch {
            runCatching { manager.save(rule) }
        }
    }
}

/** Suppression automatique : règles de rétention (lot 4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoDeleteScreen(
    onBack: () -> Unit,
    viewModel: AutoDeleteViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<AutoDeleteRule?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autodelete_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            // Même style que le bouton d'écriture de la liste :
            // primaire plein, jamais le conteneur pâle par défaut
            // (harmonisation, brief terrain du 24 octobre).
            FloatingActionButton(
                onClick = { creating = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.autodelete_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.autodelete_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(rules, key = { it.id }) { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editing = rule }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rule.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    stringResource(
                                        R.string.autodelete_summary,
                                        rule.olderThanDays,
                                        rule.keepMinimum,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (rule.threadAddress.isNotBlank()) {
                                    Text(
                                        rule.threadAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { viewModel.toggle(rule.id, it) },
                            )
                            IconButton(onClick = { viewModel.delete(rule.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        AutoDeleteEditDialog(
            rule = AutoDeleteRule(name = ""),
            onDismiss = { creating = false },
            onSave = { viewModel.save(it); creating = false },
        )
    }
    editing?.let { rule ->
        AutoDeleteEditDialog(
            rule = rule,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
        )
    }
}

/** Dialogue de création/édition d'une règle de suppression. */
@Composable
private fun AutoDeleteEditDialog(
    rule: AutoDeleteRule,
    onDismiss: () -> Unit,
    onSave: (AutoDeleteRule) -> Unit,
) {
    var name by remember { mutableStateOf(rule.name) }
    var threadAddress by remember { mutableStateOf(rule.threadAddress) }
    var olderThanDays by remember { mutableStateOf(rule.olderThanDays.toString()) }
    var keepMinimum by remember { mutableStateOf(rule.keepMinimum.toString()) }
    val days = olderThanDays.toIntOrNull()
    val keep = keepMinimum.toIntOrNull()
    val valid = name.isNotBlank() && days != null && days >= 1 && keep != null && keep >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.autodelete_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.autodelete_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = threadAddress,
                    onValueChange = { threadAddress = it },
                    label = { Text(stringResource(R.string.autodelete_thread_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = olderThanDays,
                        onValueChange = { olderThanDays = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.autodelete_older_than)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = keepMinimum,
                        onValueChange = { keepMinimum = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.autodelete_keep)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        rule.copy(
                            name = name.trim(),
                            threadAddress = threadAddress.trim(),
                            olderThanDays = days ?: 90,
                            keepMinimum = keep ?: 20,
                        ),
                    )
                },
                enabled = valid,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
