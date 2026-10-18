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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.SwipeAction
import tech.dmampro.tinda.data.local.HIGHLIGHT_END
import tech.dmampro.tinda.data.local.HIGHLIGHT_START
import tech.dmampro.tinda.util.TimeFormat

/**
 * Liste des conversations : vraies données (lot 1), recherche plein texte
 * et gestes swipe configurables (lot 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (Long) -> Unit,
    onNewMessageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onVaultLongPress: () -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conversations_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            // Entrée discrète du coffre secret : appui long sur le +.
            // Aucun indice visible dans l'interface.
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.combinedClickable(
                    onClick = onNewMessageClick,
                    onLongClick = onVaultLongPress,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_message))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Barre de recherche plein texte (F3).
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(R.string.banner_not_default),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = onOpenOnboarding) {
                                Text(stringResource(R.string.banner_activate))
                            }
                        }
                    }
                }
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.empty_conversations))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(conversations, key = { it.threadId }) { item ->
                            SwipeableConversationRow(
                                item = item,
                                swipeLeftAction = swipeLeftAction,
                                swipeRightAction = swipeRightAction,
                                onSwipe = ::doSwipeAction,
                                onClick = { onConversationClick(item.threadId) },
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

/** Ligne de conversation avec swipe configurable (F5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    item: ConversationItem,
    swipeLeftAction: SwipeAction,
    swipeRightAction: SwipeAction,
    onSwipe: (SwipeAction, ConversationItem) -> Unit,
    onClick: () -> Unit,
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
                        .background(MaterialTheme.colorScheme.primaryContainer)
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        content = {
            ConversationRow(item = item, onClick = onClick)
        },
    )
}

private fun swipeActionIcon(action: SwipeAction): ImageVector = when (action) {
    SwipeAction.REPLY -> Icons.AutoMirrored.Filled.Reply
    SwipeAction.ARCHIVE -> Icons.Filled.Archive
    SwipeAction.DELETE -> Icons.Filled.Delete
    SwipeAction.CALL -> Icons.Filled.Call
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
    val highlightColor = MaterialTheme.colorScheme.primary
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
                    color = highlightColor,
                ),
            ) {
                append(snippet.substring(start + HIGHLIGHT_START.length, end))
            }
            index = end + HIGHLIGHT_END.length
        }
    }
}

@Composable
private fun ConversationRow(item: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    item.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    TimeFormat.shortDate(item.date, stringResource(R.string.yesterday)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item.unread) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
