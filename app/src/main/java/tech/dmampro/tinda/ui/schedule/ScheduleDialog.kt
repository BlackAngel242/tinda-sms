package tech.dmampro.tinda.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar
import tech.dmampro.tinda.R

/**
 * Dialogue de planification (F9) : choix de la date puis de l'heure.
 * Appelle [onConfirm] avec l'échéance en millisecondes (epoch).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(0) } // 0 = date, 1 = heure
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
    )
    val timeState = rememberTimePickerState(is24Hour = true)
    var dateError by remember { mutableStateOf(false) }

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dateState.selectedDateMillis != null) {
                            step = 1
                        } else {
                            dateError = true
                        }
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                DatePicker(state = dateState)
                if (dateError) {
                    Text(
                        stringResource(R.string.schedule_pick_date),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.schedule_pick_time)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateState.selectedDateMillis
                                ?: System.currentTimeMillis()
                            set(Calendar.HOUR_OF_DAY, timeState.hour)
                            set(Calendar.MINUTE, timeState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(cal.timeInMillis)
                    },
                ) { Text(stringResource(R.string.schedule_confirm)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { step = 0 }) {
                        Text(stringResource(R.string.back))
                    }
                    Spacer(Modifier.height(0.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
        )
    }
}
