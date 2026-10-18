package tech.dmampro.tinda.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.AppLanguage
import tech.dmampro.tinda.data.SwipeAction
import tech.dmampro.tinda.theme.TINDA_THEMES
import tech.dmampro.tinda.theme.ThemeMode
import tech.dmampro.tinda.util.LocaleHelper

/** Écran des paramètres (lot 2) : simple par défaut, avancé replié (DEC-010). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBlocklist: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenScheduled: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeId by viewModel.themeId.collectAsState(initial = "lagoon")
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM.id)
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = true)
    val swipeLeft by viewModel.swipeLeft.collectAsState(initial = SwipeAction.ARCHIVE.name)
    val swipeRight by viewModel.swipeRight.collectAsState(initial = SwipeAction.REPLY.name)
    val soundEnabled by viewModel.soundEnabled.collectAsState(initial = true)
    val soundVolume by viewModel.soundVolume.collectAsState(initial = 70)
    val appLanguage by viewModel.appLanguage.collectAsState(initial = AppLanguage.SYSTEM.name)
    val archivedItems by viewModel.archivedItems.collectAsState()

    var advancedOpen by remember { mutableStateOf(false) }
    var archivedOpen by remember { mutableStateOf(false) }
    var gesturePicker by remember { mutableStateOf<SwipeAction?>(null) }
    var gestureSide by remember { mutableStateOf(true) } // true = gauche

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionTitle(stringResource(R.string.settings_appearance))
            }
            item {
                ThemePicker(
                    selectedId = themeId,
                    onSelect = viewModel::setTheme,
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_display_mode),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(
                        label = stringResource(R.string.settings_mode_system),
                        selected = themeMode == ThemeMode.SYSTEM.id,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM.id) },
                    )
                    ModeChip(
                        label = stringResource(R.string.settings_mode_light),
                        selected = themeMode == ThemeMode.LIGHT.id,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT.id) },
                    )
                    ModeChip(
                        label = stringResource(R.string.settings_mode_dark),
                        selected = themeMode == ThemeMode.DARK.id,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK.id) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingSwitch(
                        title = stringResource(R.string.settings_dynamic_color),
                        checked = dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }
            item {
                SectionTitle(stringResource(R.string.settings_sound))
                SettingSwitch(
                    title = stringResource(R.string.settings_sound_enabled),
                    checked = soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )
            }
            if (soundEnabled) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text(
                            stringResource(R.string.settings_sound_volume),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(90.dp),
                        )
                        Slider(
                            value = soundVolume.toFloat(),
                            onValueChange = { viewModel.setSoundVolume(it.toInt()) },
                            valueRange = 0f..100f,
                        )
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.settings_language))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(
                        label = stringResource(R.string.settings_language_system),
                        selected = appLanguage == AppLanguage.SYSTEM.name,
                        onClick = { viewModel.setLanguage(AppLanguage.SYSTEM) },
                    )
                    ModeChip(
                        label = stringResource(R.string.settings_language_french),
                        selected = appLanguage == AppLanguage.FRENCH.name,
                        onClick = { viewModel.setLanguage(AppLanguage.FRENCH) },
                    )
                    ModeChip(
                        label = stringResource(R.string.settings_language_english),
                        selected = appLanguage == AppLanguage.ENGLISH.name,
                        onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                    )
                }
                if (!LocaleHelper.isPerAppLanguageSupported()) {
                    Text(
                        stringResource(R.string.settings_language_system_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                SectionTitle(stringResource(R.string.settings_blocklist))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBlocklist)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_blocklist_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }
            item {
                SectionTitle(stringResource(R.string.settings_scheduled))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenScheduled)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_scheduled_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedOpen = !advancedOpen }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_advanced),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                }
            }
            if (advancedOpen) {
                item {
                    Text(
                        stringResource(R.string.settings_gestures),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    GestureRow(
                        label = stringResource(R.string.settings_swipe_right),
                        actionName = swipeRight,
                        onClick = { gestureSide = false; gesturePicker = SwipeAction.valueOf(swipeRight) },
                    )
                }
                item {
                    GestureRow(
                        label = stringResource(R.string.settings_swipe_left),
                        actionName = swipeLeft,
                        onClick = { gestureSide = true; gesturePicker = SwipeAction.valueOf(swipeLeft) },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenDiagnostic)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.settings_diagnostic),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { archivedOpen = !archivedOpen }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.settings_archived, archivedItems.size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (archivedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                        )
                    }
                }
                if (archivedOpen) {
                    if (archivedItems.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.settings_archived_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        items(archivedItems.size) { index ->
                            val item = archivedItems[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.unarchive(item.threadId) }) {
                                    Text(stringResource(R.string.settings_unarchive))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Dialogue de choix d'une action de geste.
    gesturePicker?.let { current ->
        AlertDialog(
            onDismissRequest = { gesturePicker = null },
            title = { Text(stringResource(R.string.settings_gesture_action)) },
            text = {
                Column {
                    SwipeAction.entries.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (gestureSide) viewModel.setSwipeLeft(action)
                                    else viewModel.setSwipeRight(action)
                                    gesturePicker = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = action == current,
                                onClick = null,
                            )
                            Text(
                                swipeActionLabel(action),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { gesturePicker = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Grille des 6 thèmes avec aperçu instantané (US-003). */
@Composable
private fun ThemePicker(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TINDA_THEMES.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { theme ->
                    val selected = theme.id == selectedId
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(theme.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Trois pastilles : primaire, secondaire, fond.
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(theme.light.primary),
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(theme.light.secondary),
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(theme.light.background)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(theme.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                // Équilibre la ligne si nombre impair (jamais le cas avec 6).
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GestureRow(label: String, actionName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            swipeActionLabel(SwipeAction.valueOf(actionName)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun swipeActionLabel(action: SwipeAction): String = stringResource(
    when (action) {
        SwipeAction.REPLY -> R.string.action_reply
        SwipeAction.ARCHIVE -> R.string.action_archive
        SwipeAction.DELETE -> R.string.action_delete
        SwipeAction.CALL -> R.string.action_call
    },
)
