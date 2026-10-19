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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.AppLanguage
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SwipeAction
import tech.dmampro.tinda.theme.RadiantBg
import tech.dmampro.tinda.theme.RadiantOnPrimary
import tech.dmampro.tinda.theme.RadiantPrimary
import tech.dmampro.tinda.theme.RadiantSim1
import tech.dmampro.tinda.theme.RadiantSim2
import tech.dmampro.tinda.theme.RadiantSurface
import tech.dmampro.tinda.theme.RadiantSurfaceLow
import tech.dmampro.tinda.theme.RadiantText
import tech.dmampro.tinda.theme.RadiantTextSecondary
import tech.dmampro.tinda.theme.RadiantTextTertiary
import tech.dmampro.tinda.theme.TINDA_THEMES
import tech.dmampro.tinda.theme.ThemeMode
import tech.dmampro.tinda.util.LocaleHelper

/**
 * Écran des paramètres au style Radiant Slate (design Stitch).
 * Sections : Double SIM, Préférences, Automatisation, Sécurité, Avancé (verrouillé).
 * Toutes les routes vers les sous-écrans sont conservées.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBlocklist: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onOpenScheduled: () -> Unit = {},
    onOpenAutoreply: () -> Unit = {},
    onOpenForward: () -> Unit = {},
    onOpenAutodelete: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeId by viewModel.themeId.collectAsState(initial = "radiant")
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM.id)
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = true)
    val swipeLeft by viewModel.swipeLeft.collectAsState(initial = SwipeAction.ARCHIVE.name)
    val swipeRight by viewModel.swipeRight.collectAsState(initial = SwipeAction.REPLY.name)
    val soundEnabled by viewModel.soundEnabled.collectAsState(initial = true)
    val soundVolume by viewModel.soundVolume.collectAsState(initial = 70)
    val appLanguage by viewModel.appLanguage.collectAsState(initial = AppLanguage.SYSTEM.name)
    val archivedItems by viewModel.archivedItems.collectAsState()
    val sims by viewModel.sims.collectAsState()
    val pendingScheduled by viewModel.pendingScheduledCount.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState(initial = 0)

    var advancedOpen by remember { mutableStateOf(false) }
    var advancedUnlocked by remember { mutableStateOf(false) }
    var showAdvancedLock by remember { mutableStateOf(false) }
    var archivedOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    var gesturePicker by remember { mutableStateOf<SwipeAction?>(null) }
    var gestureSide by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = RadiantBg,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .background(RadiantBg)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // En-tête : TINDA SMS / Paramètres / sous-titre.
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = RadiantText,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "TINDA SMS",
                            color = RadiantPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            stringResource(R.string.settings_title),
                            color = RadiantText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.settings_subtitle),
                            color = RadiantTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Carte Double SIM.
            item {
                DualSimCard(sims = sims)
                Spacer(Modifier.height(8.dp))
            }

            // PRÉFÉRENCES.
            item { RadiantSectionTitle(stringResource(R.string.settings_section_prefs)) }
            item {
                RadiantRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_appearance),
                    subtitle = themeLabel(themeId),
                    onClick = { appearanceOpen = !appearanceOpen },
                    trailing = {
                        Icon(
                            if (appearanceOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = RadiantTextTertiary,
                        )
                    },
                )
            }
            if (appearanceOpen) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RadiantSurfaceLow)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemePickerCompact(
                            selectedId = themeId,
                            onSelect = viewModel::setTheme,
                        )
                        Text(
                            stringResource(R.string.settings_display_mode),
                            color = RadiantTextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadiantChip(
                                label = stringResource(R.string.settings_mode_system),
                                selected = themeMode == ThemeMode.SYSTEM.id,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM.id) },
                            )
                            RadiantChip(
                                label = stringResource(R.string.settings_mode_light),
                                selected = themeMode == ThemeMode.LIGHT.id,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT.id) },
                            )
                            RadiantChip(
                                label = stringResource(R.string.settings_mode_dark),
                                selected = themeMode == ThemeMode.DARK.id,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK.id) },
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    stringResource(R.string.settings_dynamic_color),
                                    color = RadiantText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = dynamicColor,
                                    onCheckedChange = viewModel::setDynamicColor,
                                )
                            }
                        }
                    }
                }
            }
            item {
                RadiantRow(
                    icon = Icons.Filled.VolumeUp,
                    title = stringResource(R.string.settings_sound),
                    subtitle = stringResource(R.string.settings_sound_enabled),
                    trailing = {
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = viewModel::setSoundEnabled,
                        )
                    },
                )
            }
            if (soundEnabled) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(
                            stringResource(R.string.settings_sound_volume),
                            color = RadiantTextSecondary,
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
                RadiantRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = languageLabel(appLanguage),
                    onClick = null,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RadiantChip(
                                label = stringResource(R.string.settings_language_french),
                                selected = appLanguage == AppLanguage.FRENCH.name,
                                onClick = { viewModel.setLanguage(AppLanguage.FRENCH) },
                            )
                            RadiantChip(
                                label = stringResource(R.string.settings_language_english),
                                selected = appLanguage == AppLanguage.ENGLISH.name,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            )
                        }
                    },
                )
            }

            // AUTOMATISATION & RÈGLES.
            item { RadiantSectionTitle(stringResource(R.string.settings_section_automation)) }
            item {
                RadiantRow(
                    icon = Icons.Filled.Schedule,
                    title = stringResource(R.string.settings_scheduled),
                    subtitle = stringResource(R.string.settings_scheduled_desc),
                    onClick = onOpenScheduled,
                    badge = pendingScheduled.takeIf { it > 0 }?.toString(),
                )
            }
            item {
                RadiantRow(
                    icon = Icons.Filled.SmartToy,
                    title = stringResource(R.string.settings_autoreply),
                    subtitle = stringResource(R.string.settings_autoreply_desc),
                    onClick = onOpenAutoreply,
                )
            }
            item {
                RadiantRow(
                    icon = Icons.Filled.Forward,
                    title = stringResource(R.string.settings_forward),
                    subtitle = stringResource(R.string.settings_forward_desc),
                    onClick = onOpenForward,
                )
            }
            item {
                RadiantRow(
                    icon = Icons.Filled.DeleteSweep,
                    title = stringResource(R.string.settings_autodelete),
                    subtitle = stringResource(R.string.settings_autodelete_desc),
                    onClick = onOpenAutodelete,
                )
            }

            // SÉCURITÉ & CONFIDENTIALITÉ.
            item { RadiantSectionTitle(stringResource(R.string.settings_section_security)) }
            item {
                RadiantRow(
                    icon = Icons.Filled.Block,
                    title = stringResource(R.string.settings_blocklist),
                    subtitle = stringResource(R.string.settings_blocklist_desc),
                    onClick = onOpenBlocklist,
                    badge = blockedCount.takeIf { it > 0 }?.toString(),
                )
            }

            // AVANCÉ (verrouillé par le code du coffre).
            item {
                val locked = !advancedOpen && !advancedUnlocked && viewModel.isAdvancedLocked()
                RadiantRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.settings_advanced),
                    subtitle = if (locked) stringResource(R.string.settings_advanced_locked_desc)
                    else stringResource(R.string.settings_advanced_desc),
                    onClick = {
                        if (!advancedOpen && !advancedUnlocked && viewModel.isAdvancedLocked()) {
                            showAdvancedLock = true
                        } else {
                            advancedOpen = !advancedOpen
                        }
                    },
                    trailing = {
                        Icon(
                            if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = RadiantTextTertiary,
                        )
                    },
                )
            }
            if (advancedOpen) {
                item {
                    Text(
                        stringResource(R.string.settings_gestures),
                        color = RadiantTextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                item {
                    RadiantRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = stringResource(R.string.settings_swipe_right),
                        subtitle = swipeActionLabel(SwipeAction.valueOf(swipeRight)),
                        onClick = { gestureSide = false; gesturePicker = SwipeAction.valueOf(swipeRight) },
                    )
                }
                item {
                    RadiantRow(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        title = stringResource(R.string.settings_swipe_left),
                        subtitle = swipeActionLabel(SwipeAction.valueOf(swipeLeft)),
                        onClick = { gestureSide = true; gesturePicker = SwipeAction.valueOf(swipeLeft) },
                    )
                }
                item {
                    RadiantRow(
                        icon = Icons.Filled.BugReport,
                        title = stringResource(R.string.settings_diagnostic),
                        subtitle = stringResource(R.string.settings_diagnostic_desc),
                        onClick = onOpenDiagnostic,
                    )
                }
                item {
                    RadiantRow(
                        icon = Icons.Filled.ExpandMore,
                        title = stringResource(R.string.settings_archived, archivedItems.size),
                        subtitle = null,
                        onClick = { archivedOpen = !archivedOpen },
                    )
                }
                if (archivedOpen) {
                    if (archivedItems.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.settings_archived_empty),
                                color = RadiantTextSecondary,
                                style = MaterialTheme.typography.bodySmall,
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
                                    color = RadiantText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.unarchive(item.threadId) }) {
                                    Text(
                                        stringResource(R.string.settings_unarchive),
                                        color = RadiantPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pied de page.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.settings_version, "0.1.0"),
                        color = RadiantTextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(R.string.settings_ondevice),
                        color = RadiantTextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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

    // Verrou des options avancées (code du coffre ou biométrie).
    if (showAdvancedLock) {
        AdvancedLockDialog(
            viewModel = viewModel,
            onUnlocked = {
                showAdvancedLock = false
                advancedUnlocked = true
                advancedOpen = true
            },
            onDismiss = { showAdvancedLock = false },
        )
    }
}

/** Titre de section : teal, petites capitales (style Stitch). */
@Composable
private fun RadiantSectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = RadiantPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp),
    )
}

/**
 * Ligne de paramètre Radiant : icône teal, titre blanc, sous-titre gris,
 * chevron ou badge ou contrôle à droite. Toute la ligne est cliquable.
 */
@Composable
private fun RadiantRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    badge: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(RadiantPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = RadiantPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = RadiantText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = RadiantTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
            }
        }
        if (badge != null) {
            Badge(
                containerColor = RadiantPrimary,
                contentColor = RadiantOnPrimary,
            ) { Text(badge) }
            Spacer(Modifier.width(8.dp))
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = RadiantTextTertiary,
            )
        }
    }
}

/** Chip Radiant : fond teal quand sélectionné, surface sinon. */
@Composable
private fun RadiantChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                color = if (selected) RadiantOnPrimary else RadiantTextSecondary,
            )
        },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = RadiantPrimary,
            containerColor = RadiantSurfaceLow,
        ),
        border = null,
    )
}

/** Carte Double SIM : SIM actives avec couleurs dédiées (rouge / ambre). */
@Composable
private fun DualSimCard(sims: List<SimInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RadiantSurfaceLow)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(RadiantPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.SimCard, contentDescription = null, tint = RadiantPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_dualsim_title),
                    color = RadiantText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.settings_dualsim_subtitle),
                    color = RadiantPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sims.forEachIndexed { index, sim ->
                val simColor = if (index == 0) RadiantSim1 else RadiantSim2
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, simColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .background(RadiantSurface)
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(simColor),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                R.string.settings_sim_label,
                                index + 1,
                            ),
                            color = RadiantText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        sim.displayName,
                        color = RadiantTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Sélecteur de thème compact : pastilles de couleur par thème. */
@Composable
private fun ThemePickerCompact(selectedId: String, onSelect: (String) -> Unit) {
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
                                color = if (selected) RadiantPrimary
                                else RadiantTextTertiary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(theme.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(theme.dark.primary),
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(theme.dark.secondary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(theme.labelRes),
                            color = RadiantText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun themeLabel(themeId: String): String {
    val theme = TINDA_THEMES.find { it.id == themeId } ?: TINDA_THEMES.first()
    return stringResource(theme.labelRes)
}

@Composable
private fun languageLabel(appLanguage: String): String = stringResource(
    when (appLanguage) {
        AppLanguage.FRENCH.name -> R.string.settings_language_french
        AppLanguage.ENGLISH.name -> R.string.settings_language_english
        else -> R.string.settings_language_system
    },
)

@Composable
private fun swipeActionLabel(action: SwipeAction): String = stringResource(
    when (action) {
        SwipeAction.REPLY -> R.string.action_reply
        SwipeAction.ARCHIVE -> R.string.action_archive
        SwipeAction.DELETE -> R.string.action_delete
        SwipeAction.CALL -> R.string.action_call
    },
)
