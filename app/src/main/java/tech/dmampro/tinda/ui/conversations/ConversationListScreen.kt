package tech.dmampro.tinda.ui.conversations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import tech.dmampro.tinda.ui.components.SimBadge
import tech.dmampro.tinda.ui.components.TindaAvatar
import tech.dmampro.tinda.util.OtpDetection
import tech.dmampro.tinda.util.EventLog
import tech.dmampro.tinda.util.TimeFormat

/**
 * Liste des conversations, accueil sobre ardoise (lot 1) :
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
    val otpFilter by viewModel.otpFilter.collectAsState()
    val otpCount by viewModel.otpCount.collectAsState()
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    .background(MaterialTheme.colorScheme.primary)
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
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            ConversationHeader(
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
                        color = MaterialTheme.colorScheme.outline,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onOpenOnboarding) {
                                Text(
                                    stringResource(R.string.banner_activate),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                // Chips de filtre SIM (style Stitch).
                if (sims.size > 1 || otpCount > 0) {
                    SimFilterChips(
                        sims = sims,
                        activeFilter = simFilter,
                        totalCount = allConversations.size,
                        onSelect = viewModel::setSimFilter,
                        otpCount = otpCount,
                        otpActive = otpFilter,
                        onOtpToggle = viewModel::setOtpFilter,
                    )
                }
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.empty_conversations),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.recent_messages),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        // Marge basse : la dernière carte (dont le
                        // bouton « Copier » des codes) doit pouvoir
                        // défiler entièrement au-dessus du bouton
                        // d'écriture, qui la recouvrait (brief
                        // terrain du 24 octobre). 56 bouton + 16
                        // marge + 16 d'air.
                        contentPadding = PaddingValues(bottom = 88.dp),
                    ) {
                        items(conversations, key = { it.threadId }) { item ->
                            SwipeableConversationRow(
                                item = item,
                                swipeLeftAction = swipeLeftAction,
                                swipeRightAction = swipeRightAction,
                                onSwipe = ::doSwipeAction,
                                onClick = { onConversationClick(item.threadId) },
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
private fun ConversationHeader(
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
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                stringResource(R.string.conversations_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(onClick = onGroupSendClick) {
            Icon(
                Icons.Filled.Group,
                contentDescription = stringResource(R.string.groupsend_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Libellé court de la SIM ("SIM 1 Airtel"), ou null si inconnu. */
private fun simLabelFor(subscriptionId: Int?, sims: List<SimInfo>): String? {
    if (subscriptionId == null) return null
    val sim = sims.firstOrNull { it.subscriptionId == subscriptionId } ?: return null
    return "SIM ${sim.slotIndex + 1} ${sim.displayName}"
}

/** Chips de filtre par SIM : "Toutes (N)" + une chip par SIM + "Codes 2FA". */
@Composable
private fun SimFilterChips(
    sims: List<SimInfo>,
    activeFilter: Int?,
    totalCount: Int,
    onSelect: (Int?) -> Unit,
    otpCount: Int,
    otpActive: Boolean,
    onOtpToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SimChip(
            active = activeFilter == null && !otpActive,
            onClick = {
                onSelect(null)
                onOtpToggle(false)
            },
            dotColor = null,
            topText = "${stringResource(R.string.filter_all)} $totalCount",
            bottomText = null,
        )
        sims.forEach { sim ->
            SimChip(
                active = activeFilter == sim.subscriptionId && !otpActive,
                onClick = {
                    onSelect(sim.subscriptionId)
                    onOtpToggle(false)
                },
                dotColor = null,
                topText = stringResource(R.string.sim_chip_label, sim.slotIndex + 1),
                bottomText = sim.displayName,
            )
        }
        if (otpCount > 0) {
            SimChip(
                active = otpActive,
                onClick = { onOtpToggle(!otpActive) },
                dotColor = MaterialTheme.colorScheme.primary,
                topText = "${stringResource(R.string.filter_otp)} $otpCount",
                bottomText = null,
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
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val sub = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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

/** Ligne de conversation avec swipe configurable (F5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    item: ConversationItem,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    onSwipe: (SwipeAction, ConversationItem) -> Unit,
    onClick: () -> Unit,
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
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        tint = MaterialTheme.colorScheme.primary,
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
                    simLabel = simLabel,
                    onClick = onClick,
                )
            } else {
                ConversationCard(
                    item = item,
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
    simLabel: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Le liseré SIM est superposé au bord droit (Box) : le placer
        // après la ligne de contenu l'ajoutait sous la carte (+40 dp de
        // vide par carte, constat capture du 19 oct., audit L4).
        Box {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar uniforme partage (lot 1) : cercle neutre, initiales.
            TindaAvatar(displayName = item.displayName.ifBlank { item.address })
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.displayName.ifBlank { item.address },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (simLabel != null) {
                    // Badge SIM neutre et explicite : le libellé porte
                    // l'information, jamais une couleur par SIM.
                    SimBadge(label = simLabel, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    item.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.primary,
                )
                if (item.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // Liseré neutre de la SIM, centré sur le bord droit de la
        // carte (superposé, il n'ajoute aucune hauteur : correctif L4).
        if (simLabel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(width = 3.dp, height = 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
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
    simLabel: String?,
    onClick: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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
                TindaAvatar(displayName = item.displayName.ifBlank { item.address })
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName.ifBlank { item.address },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (simLabel != null) {
                        SimBadge(label = simLabel, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ) {
                    Text(
                        stringResource(R.string.otp_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    TimeFormat.shortDate(item.date, stringResource(R.string.yesterday)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                item.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Même marge basse que la liste principale : le dernier
            // résultat ne doit pas rester sous le bouton d'écriture.
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            TimeFormat.shortDate(result.date, stringResource(R.string.yesterday)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        parseHighlight(result.snippet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.primary,
                ),
            ) {
                append(snippet.substring(start + HIGHLIGHT_START.length, end))
            }
            index = end + HIGHLIGHT_END.length
        }
    }
}
