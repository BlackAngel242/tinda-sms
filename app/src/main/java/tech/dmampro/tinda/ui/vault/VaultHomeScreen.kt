package tech.dmampro.tinda.ui.vault

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.VaultRepository
import tech.dmampro.tinda.data.local.VaultThread
import tech.dmampro.tinda.util.TimeFormat

@HiltViewModel
class VaultHomeViewModel @Inject constructor(
    private val vault: VaultRepository,
) : ViewModel() {
    val threads = vault.observeThreads()

    fun lock() = vault.lock()
}

/** Liste des conversations secrètes (noms masqués : initiales uniquement). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    onThreadClick: (String) -> Unit,
    onOpenRules: () -> Unit,
    onLock: () -> Unit,
    viewModel: VaultHomeViewModel = hiltViewModel(),
) {
    val threads by viewModel.threads.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.lock()
                            onLock()
                        },
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.vault_lock))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenRules) {
                        Icon(Icons.Filled.Rule, contentDescription = stringResource(R.string.vault_rules))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.vault_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(threads, key = { it.address }) { thread ->
                    VaultRow(thread = thread, onClick = { onThreadClick(thread.address) })
                }
            }
        }
    }
}

@Composable
private fun VaultRow(thread: VaultThread, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Adresse masquée : initiales + fin de numéro uniquement.
                Text(
                    maskAddress(thread.address),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    TimeFormat.shortDate(thread.lastDate, "…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Aperçu volontairement neutre : pas de contenu sensible.
                Text(
                    stringResource(R.string.vault_hidden_preview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (thread.unreadCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Masque un numéro : "••• 42". */
private fun maskAddress(address: String): String {
    val digits = address.filter { it.isDigit() }
    return if (digits.length >= 2) "••• " + digits.takeLast(2) else "•••"
}
