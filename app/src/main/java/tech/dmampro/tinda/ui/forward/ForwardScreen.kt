package tech.dmampro.tinda.ui.forward

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.local.ForwardRule
import tech.dmampro.tinda.data.local.TindaDatabase
import tech.dmampro.tinda.util.EventLog

@HiltViewModel
class ForwardViewModel @Inject constructor(
    private val db: TindaDatabase,
) : ViewModel() {
    val rules = db.forwardDao().observeAll()

    fun toggle(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            db.forwardDao().setEnabled(id, enabled)
            EventLog.log("forward.rule_toggle", "$id:$enabled")
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            db.forwardDao().delete(id)
            EventLog.log("forward.rule_delete")
        }
    }

    fun save(rule: ForwardRule) {
        viewModelScope.launch {
            if (rule.id == 0L) db.forwardDao().insert(rule)
            else db.forwardDao().update(rule)
            EventLog.log("forward.rule_save")
        }
    }
}

/** Transfert automatique : règles, activation, édition (F11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardScreen(
    onBack: () -> Unit,
    viewModel: ForwardViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<ForwardRule?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forward_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add))
            }
        },
    ) { innerPadding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.forward_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
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
                                sourceSummary(rule),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.forward_to, rule.destination),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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

    if (creating) {
        ForwardEditDialog(
            rule = ForwardRule(name = ""),
            onDismiss = { creating = false },
            onSave = { viewModel.save(it); creating = false },
        )
    }
    editing?.let { rule ->
        ForwardEditDialog(
            rule = rule,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
        )
    }
}

@Composable
private fun sourceSummary(rule: ForwardRule): String {
    val source = if (rule.sourceType == "KEYWORD") {
        stringResource(R.string.forward_source_keyword, rule.sourceValue)
    } else {
        stringResource(R.string.forward_source_number, rule.sourceValue)
    }
    val content = if (rule.includeContent) {
        stringResource(R.string.forward_with_content)
    } else {
        stringResource(R.string.forward_notify_only)
    }
    return "$source · $content"
}

/** Dialogue de création/édition d'une règle de transfert. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardEditDialog(
    rule: ForwardRule,
    onDismiss: () -> Unit,
    onSave: (ForwardRule) -> Unit,
) {
    var name by remember { mutableStateOf(rule.name) }
    var sourceType by remember { mutableStateOf(rule.sourceType) }
    var sourceValue by remember { mutableStateOf(rule.sourceValue) }
    var destination by remember { mutableStateOf(rule.destination) }
    var includeContent by remember { mutableStateOf(rule.includeContent) }
    var typeOpen by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && sourceValue.isNotBlank() && destination.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.forward_edit_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.forward_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = typeOpen,
                        onExpandedChange = { typeOpen = it },
                    ) {
                        OutlinedTextField(
                            value = if (sourceType == "KEYWORD") {
                                stringResource(R.string.forward_type_keyword)
                            } else {
                                stringResource(R.string.forward_type_number)
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.forward_source_type)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeOpen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = typeOpen,
                            onDismissRequest = { typeOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.forward_type_number)) },
                                onClick = { sourceType = "NUMBER"; typeOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.forward_type_keyword)) },
                                onClick = { sourceType = "KEYWORD"; typeOpen = false },
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = sourceValue,
                        onValueChange = { sourceValue = it },
                        label = {
                            Text(
                                if (sourceType == "KEYWORD") {
                                    stringResource(R.string.forward_keyword)
                                } else {
                                    stringResource(R.string.forward_source_number_hint)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text(stringResource(R.string.forward_destination)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.forward_include_content),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = includeContent,
                            onCheckedChange = { includeContent = it },
                        )
                    }
                }
                item {
                    Text(
                        stringResource(R.string.forward_loop_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            sourceType = sourceType,
                            sourceValue = sourceValue.trim(),
                            destination = destination.trim(),
                            includeContent = includeContent,
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
