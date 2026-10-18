package tech.dmampro.tinda.ui.schedule

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch
import tech.dmampro.tinda.R
import tech.dmampro.tinda.data.local.ScheduledMessage
import tech.dmampro.tinda.data.local.ScheduledStatus
import tech.dmampro.tinda.sms.ScheduledSmsManager

@HiltViewModel
class ScheduledListViewModel @Inject constructor(
    private val scheduler: ScheduledSmsManager,
) : ViewModel() {
    val items = scheduler.observeAll()

    fun cancel(id: Long) {
        viewModelScope.launch { scheduler.cancel(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { scheduler.delete(id) }
    }
}

/** SMS programmés : liste, annulation, suppression de l'historique (F9). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledListScreen(
    onBack: () -> Unit,
    viewModel: ScheduledListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduled_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.scheduled_empty),
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
                items(items, key = { it.id }) { item ->
                    ScheduledRow(
                        item = item,
                        onCancel = { viewModel.cancel(item.id) },
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledRow(
    item: ScheduledMessage,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.address,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                dateFormat.format(Date(item.scheduledAt)) + " · " +
                    stringResource(statusLabel(item.status)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.status == ScheduledStatus.PENDING) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.scheduled_cancel))
            }
        } else {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

private fun statusLabel(status: ScheduledStatus): Int = when (status) {
    ScheduledStatus.PENDING -> R.string.scheduled_pending
    ScheduledStatus.SENT -> R.string.scheduled_sent
    ScheduledStatus.CANCELLED -> R.string.scheduled_cancelled
    ScheduledStatus.FAILED -> R.string.scheduled_failed
}
