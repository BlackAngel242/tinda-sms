package tech.dmampro.tinda.ui.conversations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SimInfo
import tech.dmampro.tinda.data.SwipeAction
import tech.dmampro.tinda.data.local.HIGHLIGHT_END
import tech.dmampro.tinda.data.local.HIGHLIGHT_START
import tech.dmampro.tinda.theme.RadiantBg
import tech.dmampro.tinda.theme.RadiantOnPrimary
import tech.dmampro.tinda.theme.RadiantPrimary
import tech.dmampro.tinda.theme.RadiantSim1
import tech.dmampro.tinda.theme.RadiantSim2
import tech.dmampro.tinda.theme.RadiantSurface
import tech.dmampro.tinda.theme.RadiantSurfaceHigh
import tech.dmampro.tinda.theme.RadiantSurfaceLow
import tech.dmampro.tinda.theme.RadiantText
import tech.dmampro.tinda.theme.RadiantTextSecondary
import tech.dmampro.tinda.util.OtpDetection
import tech.dmampro.tinda.theme.RadiantTextTertiary
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.TimeFormat

/**
 * Liste des conversations, style "Radiant Slate" (design Stitch) :
 * en-tête, recherche en pilule, chips de filtre SIM, cartes de
 * conversation et carte OTP mise en avant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (Long) -> Unit,
    onNewMessageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onVaultLongPress: () -> Unit = {},
    onGroupSendClick: () -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val allConversations by viewModel.conversations.collectAsState()
    val conversations by viewModel.filteredConversations.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()
    val sims by viewModel.sims.collectAsState()
    val simFilter by viewModel.simFilter.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<ConversationItem?>(null) }
    val searching = searchQuery.isNotBlank()

    // Rafraîchit à chaque retour (un SMS peut être arrivé entre-temps).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /** Exécute l'action configurée pour un geste swipe. */
    fun doSwipeAction(action: SwipeAction, item: ConversationItem) {
        when (action) {
            SwipeAction.REPLY -> onConversationClick(item.threadId)
            SwipeAction.ARCHIVE -> scope.launch { viewModel.archiveThread(item.threadId) }
            SwipeAction.DELETE -> pendingDelete = item
            SwipeAction.CALL -> {
                if (item.address.isNotBlank()) {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.address}")),
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = RadiantBg,
        floatingActionButton = {
            // Entrée discrète du coffre secret : appui long sur le bouton.
            // Bouton reconstruit avec UN SEUL combinedClickable (le
            // FloatingActionButton de Material3 consommerait l'appui long
            // via son clickable interne).
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(RadiantPrimary)
                    .combinedClickable(
                        onClick = {
                            EventLog.log("tap.fab_new_message")
                            onNewMessageClick()
                        },
                        onLongClick = {
                            EventLog.log("tap.fab_vault_longpress")
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onVaultLongPress()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.new_message),
                    tint = RadiantOnPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RadiantBg)
                .padding(innerPadding),
        ) {
            RadiantHeader(
                onGroupSendClick = {
                    EventLog.log("tap.group_send_open")
                    onGroupSendClick()
                },
                onSettingsClick = onSettingsClick,
            )
            // Barre de recherche en pilule (F3).
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = {
                    Text(
                        stringResource(R.string.search_hint),
                        color = RadiantTextTertiary,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = RadiantTextTertiary)
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = RadiantSurface,
                    unfocusedContainerColor = RadiantSurface,
                    disabledContainerColor = RadiantSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = RadiantText,
                    unfocusedTextColor = RadiantText,
                    cursorColor = RadiantPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (searching) {
                SearchResultsList(
                    results = searchResults,
                    query = searchQuery,
                    onResultClick = { onConversationClick(it.threadId) },
                )
            } else {
                if (!isDefaultSmsApp) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RadiantSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(R.string.banner_not_default),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RadiantText,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onOpenOnboarding) {
                                Text(
                                    stringResource(R.string.banner_activate),
                                    color = RadiantPrimary,
                                )
                            }
                        }
                    }
                }
                // Chips de filtre SIM (style Stitch).
                if (sims.size > 1) {
                    SimFilterChips(
                        sims = sims,
                        activeFilter = simFilter,
                        totalCount = allConversations.size,
                        onSelect = viewModel::setSimFilter,
                    )
                }
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.empty_conversations),
                            color = RadiantTextSecondary,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.recent_messages),
                        style = MaterialTheme.typography.labelMedium,
                        color = RadiantTextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(conversations, key = { it.threadId }) { item ->
                            SwipeableConversationRow(
                                item = item,
                                swipeLeftAction = swipeLeftAction,
                                swipeRightAction = swipeRightAction,
                                onSwipe = ::doSwipeAction,
                                onClick = { onConversationClick(item.threadId) },
                                simColor = simColorFor(item.subscriptionId, sims),
                                simLabel = simLabelFor(item.subscriptionId, sims),
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation avant suppression définitive d'un fil.
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_thread_title)) },
            text = { Text(stringResource(R.string.delete_thread_message, item.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteThread(item.threadId)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/** En-tête "TINDA SMS / Conversations" avec actions (style Stitch). */
@Composable
private fun RadiantHeader(
    onGroupSendClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "TINDA SMS",
                style = MaterialTheme.typography.labelMedium,
                color = RadiantPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                stringResource(R.string.conversations_title),
                style = MaterialTheme.typography.headlineMedium,
                color = RadiantText,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = onGroupSendClick) {
            Icon(
                Icons.Filled.Group,
                contentDescription = stringResource(R.string.groupsend_title),
                tint = RadiantTextSecondary,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = RadiantTextSecondary)
        }
    }
}

/** Couleur de la pastille SIM : slot 0 = SIM1, slot 1 = SIM2. */
private fun simColorFor(subscriptionId: Int?, sims: List<SimInfo>): Color? {
    if (subscriptionId == null) return null
    val slot = sims.firstOrNull { it.subscriptionId == subscriptionId }?.slotIndex
        ?: return null
    return when (slot) {
        0 -> RadiantSim1
        1 -> RadiantSim2
        else -> null
    }
}

/** Libellé court de la SIM ("SIM 1 Airtel"), ou null si inconnu. */
private fun simLabelFor(subscriptionId: Int?, sims: List<SimInfo>): String? {
    if (subscriptionId == null) return null
    val sim = sims.firstOrNull { it.subscriptionId == subscriptionId } ?: return null
    return "SIM ${sim.slotIndex + 1} ${sim.displayName}"
}

/** Chips de filtre par SIM : "Toutes (N)" + une chip par SIM. */
@Composable
private fun SimFilterChips(
    sims: List<SimInfo>,
    activeFilter: Int?,
    totalCount: Int,
    onSelect: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SimChip(
            active = activeFilter == null,
            onClick = { onSelect(null) },
            dotColor = null,
            topText = "${stringResource(R.string.filter_all)} $totalCount",
            bottomText = null,
        )
        sims.forEach { sim ->
            val dot = when (sim.slotIndex) {
                0 -> RadiantSim1
                1 -> RadiantSim2
                else -> RadiantTextTertiary
            }
            SimChip(
                active = activeFilter == sim.subscriptionId,
                onClick = { onSelect(sim.subscriptionId) },
                dotColor = dot,
                topText = stringResource(R.string.sim_chip_label, sim.slotIndex + 1),
                bottomText = sim.displayName,
            )
        }
    }
}

/** Une chip de filtre SIM (pilule, style Stitch). */
@Composable
private fun SimChip(
    active: Boolean,
    onClick: () -> Unit,
    dotColor: Color?,
    topText: String,
    bottomText: String?,
) {
    val bg = if (active) RadiantPrimary else RadiantSurfaceLow
    val fg = if (active) RadiantOnPrimary else RadiantText
    val sub = if (active) RadiantOnPrimary.copy(alpha = 0.8f) else RadiantTextSecondary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Column {
            Text(
                topText,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.Bold,
            )
            if (bottomText != null) {
                Text(
                    bottomText,
                    style = MaterialTheme.typography.labelSmall,
                    color = sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Initiales d'un nom (2 lettres max) pour l'avatar. */
private fun initialsOf(displayName: String): String {
    val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    val first = parts[0].first().uppercase()
    val second = if (parts.size > 1) parts[1].first().uppercase() else ""
    return "$first$second"
}

/** Ligne de conversation avec swipe configurable (F5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    item: ConversationItem,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    onSwipe: (SwipeAction, ConversationItem) -> Unit,
    onClick: () -> Unit,
    simColor: Color?,
    simLabel: String?,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipe(swipeRightAction, item)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipe(swipeLeftAction, item)
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val action = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    swipeRightAction
                } else {
                    swipeLeftAction
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RadiantSurfaceHigh)
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    },
                ) {
                    Icon(
                        swipeActionIcon(action),
                        contentDescription = null,
                        tint = RadiantPrimary,
                    )
                }
            }
        },
        content = {
            val otpCode = remember(item.snippet) { OtpDetection.detectCode(item.snippet) }
            if (otpCode != null) {
                OtpConversationCard(
                    item = item,
                    code = otpCode,
                    simColor = simColor,
                    simLabel = simLabel,
                    onClick = onClick,
                )
            } else {
                ConversationCard(
                    item = item,
                    simColor = simColor,
                    simLabel = simLabel,
                    onClick = onClick,
                )
            }
        },
    )
}

private fun swipeActionIcon(action: SwipeAction): ImageVector = when (action) {
    SwipeAction.REPLY -> Icons.AutoMirrored.Filled.Reply
    SwipeAction.ARCHIVE -> Icons.Filled.Archive
    SwipeAction.DELETE -> Icons.Filled.Delete
    SwipeAction.CALL -> Icons.Filled.Call
}

/** Carte de conversation (style Stitch) : avatar, nom, aperçu, heure, badge. */
@Composable
private fun ConversationCard(
    item: ConversationItem,
    simColor: Color?,
    simLabel: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = RadiantSurfaceLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar circulaire 48dp avec initiales.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RadiantPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initialsOf(item.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    color = RadiantPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.displayName.ifBlank { item.address },
                        style = MaterialTheme.typography.bodyLarge,
                        color = RadiantText,
                        fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (simColor != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(simColor),
                        )
                    }
                }
                if (simLabel != null) {
                    Text(
                        simLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantTextTertiary,
                        maxLines = 1,
                    )
                }
                Text(
                    item.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadiantTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    TimeFormat.shortDate(item.date, stringResource(R.string.yesterday)),
                    style = MaterialTheme.typography.bodySmall,
                    color = RadiantPrimary,
                )
                if (item.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(RadiantPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantOnPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // Liseré coloré de la SIM sur le bord droit.
        if (simColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 2.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(simColor),
                )
            }
        }
    }
}

/** Carte OTP mise en avant : code en grand (monospace) + bouton Copier. */
@Composable
private fun OtpConversationCard(
    item: ConversationItem,
    code: String,
    simColor: Color?,
    simLabel: String?,
    onClick: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = RadiantSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RadiantPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initialsOf(item.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = RadiantPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.displayName.ifBlank { item.address },
                            style = MaterialTheme.typography.bodyLarge,
                            color = RadiantText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (simColor != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(simColor),
                            )
                        }
                    }
                    if (simLabel != null) {
                        Text(
                            simLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantTextTertiary,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RadiantPrimary.copy(alpha = 0.15f),
                ) {
                    Text(
                        stringResource(R.string.otp_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = RadiantPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    TimeFormat.shortDate(item.date, stringResource(R.string.yesterday)),
                    style = MaterialTheme.typography.bodySmall,
                    color = RadiantPrimary,
                )
            }
            Text(
                item.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = RadiantTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RadiantBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = RadiantText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                TextButton(
                    onClick = {
                        EventLog.log("tap.otp_copy")
                        clipboard.setText(AnnotatedString(code))
                    },
                ) {
                    Text(
                        stringResource(R.string.copy_code),
                        color = RadiantPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Résultats de recherche avec extraits surlignés (F3). */
@Composable
private fun SearchResultsList(
    results: List<tech.dmampro.tinda.data.SearchResultItem>,
    query: String,
    onResultClick: (tech.dmampro.tinda.data.SearchResultItem) -> Unit,
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.search_no_results, query),
                style = MaterialTheme.typography.bodyMedium,
                color = RadiantTextSecondary,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.threadId to it.date }) { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(result) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            result.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = RadiantText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            TimeFormat.shortDate(result.date, stringResource(R.string.yesterday)),
                            style = MaterialTheme.typography.bodySmall,
                            color = RadiantTextSecondary,
                        )
                    }
                    Text(
                        parseHighlight(result.snippet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RadiantTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Convertit les marqueurs de surlignage FTS en texte enrichi. */
@Composable
private fun parseHighlight(snippet: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < snippet.length) {
            val start = snippet.indexOf(HIGHLIGHT_START, index)
            if (start < 0) {
                append(snippet.substring(index))
                break
            }
            append(snippet.substring(index, start))
            val end = snippet.indexOf(HIGHLIGHT_END, start + HIGHLIGHT_START.length)
            if (end < 0) {
                append(snippet.substring(start + HIGHLIGHT_START.length))
                break
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = RadiantPrimary,
                ),
            ) {
                append(snippet.substring(start + HIGHLIGHT_START.length, end))
            }
            index = end + HIGHLIGHT_END.length
        }
    }
}
