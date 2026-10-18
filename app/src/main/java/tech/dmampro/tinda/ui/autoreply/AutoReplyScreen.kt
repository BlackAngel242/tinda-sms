package tech.dmampro.tinda.ui.autoreply

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import tech.dmampro.tinda.data.local.AutoReplyRule
import tech.dmampro.tinda.data.local.AutoReplyScope
import tech.dmampro.tinda.data.local.TindaDatabase

@HiltViewModel
class AutoReplyViewModel @Inject constructor(
    private val db: TindaDatabase,
) : ViewModel() {
    val rules = db.autoReplyDao().observeAll()

    fun toggle(id: Long, enabled: Boolean) {
        viewModelScope.launch { db.autoReplyDao().setEnabled(id, enabled) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { db.autoReplyDao().delete(id) }
    }

    fun save(rule: AutoReplyRule) {
        viewModelScope.launch {
            if (rule.id == 0L) db.autoReplyDao().insert(rule)
            else db.autoReplyDao().update(rule)
        }
    }
}

/** Répondeur automatique : règles, activation, édition (F10). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoReplyScreen(
    onBack: () -> Unit,
    viewModel: AutoReplyViewModel = hiltViewModel(),
) {
    val rules by viewModel.rules.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<AutoReplyRule?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autoreply_title)) },
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
                    stringResource(R.string.autoreply_empty),
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
                                scheduleSummary(rule),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                rule.message,
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
        AutoReplyEditDialog(
            rule = AutoReplyRule(name = "", message = ""),
            onDismiss = { creating = false },
            onSave = { viewModel.save(it); creating = false },
        )
    }
    editing?.let { rule ->
        AutoReplyEditDialog(
            rule = rule,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
        )
    }
}

@Composable
private fun scheduleSummary(rule: AutoReplyRule): String {
    val parts = mutableListOf<String>()
    if (rule.startHour >= 0 && rule.endHour >= 0 && rule.startHour != rule.endHour) {
        parts.add("${rule.startHour}h-${rule.endHour}h")
    }
    parts.add(
        when (rule.scope) {
            AutoReplyScope.ALL -> stringResource(R.string.autoreply_scope_all)
            AutoReplyScope.CONTACTS_ONLY -> stringResource(R.string.autoreply_scope_contacts)
            AutoReplyScope.NON_CONTACTS -> stringResource(R.string.autoreply_scope_non_contacts)
        },
    )
    parts.add("${rule.cooldownHours}h")
    return parts.joinToString(" · ")
}

/** Dialogue de création/édition d'une règle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoReplyEditDialog(
    rule: AutoReplyRule,
    onDismiss: () -> Unit,
    onSave: (AutoReplyRule) -> Unit,
) {
    var name by remember { mutableStateOf(rule.name) }
    var message by remember { mutableStateOf(rule.message) }
    var useWindow by remember { mutableStateOf(rule.startHour >= 0 && rule.endHour >= 0) }
    var startHour by remember { mutableStateOf(if (rule.startHour >= 0) rule.startHour else 22) }
    var endHour by remember { mutableStateOf(if (rule.endHour >= 0) rule.endHour else 7) }
    var days by remember { mutableStateOf(rule.days) }
    var scope by remember { mutableStateOf(rule.scope) }
    var cooldown by remember { mutableStateOf(rule.cooldownHours) }
    var scopeOpen by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && message.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.autoreply_edit_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.autoreply_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text(stringResource(R.string.autoreply_message)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.autoreply_window),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(checked = useWindow, onCheckedChange = { useWindow = it })
                    }
                }
                if (useWindow) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HourPicker(
                                label = stringResource(R.string.autoreply_start),
                                value = startHour,
                                onChange = { startHour = it },
                            )
                            HourPicker(
                                label = stringResource(R.string.autoreply_end),
                                value = endHour,
                                onChange = { endHour = it },
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.autoreply_days),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    DayChips(days = days, onToggle = { days = it })
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = scopeOpen,
                        onExpandedChange = { scopeOpen = it },
                    ) {
                        OutlinedTextField(
                            value = scopeLabel(scope),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.autoreply_scope)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeOpen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = scopeOpen,
                            onDismissRequest = { scopeOpen = false },
                        ) {
                            AutoReplyScope.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(scopeLabel(option)) },
                                    onClick = { scope = option; scopeOpen = false },
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.autoreply_cooldown),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        listOf(1, 6, 24, 48).forEach { hours ->
                            TextButton(
                                onClick = { cooldown = hours },
                                enabled = cooldown != hours,
                            ) { Text("${hours}h") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        rule.copy(
                            name = name.trim(),
                            message = message.trim(),
                            startHour = if (useWindow) startHour else -1,
                            endHour = if (useWindow) endHour else -1,
                            days = days,
                            scope = scope,
                            cooldownHours = cooldown,
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

@Composable
private fun RowScope.HourPicker(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value + 23) % 24) }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text("${value}h", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onChange((value + 1) % 24) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun DayChips(days: Int, onToggle: (Int) -> Unit) {
    val labels = listOf("D", "L", "M", "M", "J", "V", "S") // dimanche..samedi
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, label ->
            val bit = 1 shl index
            val selected = days and bit != 0
            TextButton(onClick = { onToggle(days xor bit) }) {
                Text(
                    label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun scopeLabel(scope: AutoReplyScope): String = when (scope) {
    AutoReplyScope.ALL -> stringResource(R.string.autoreply_scope_all)
    AutoReplyScope.CONTACTS_ONLY -> stringResource(R.string.autoreply_scope_contacts)
    AutoReplyScope.NON_CONTACTS -> stringResource(R.string.autoreply_scope_non_contacts)
}
