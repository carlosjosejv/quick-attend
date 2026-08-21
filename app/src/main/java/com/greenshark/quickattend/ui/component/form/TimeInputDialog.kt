/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Keyboard time entry locked to 24h, returning minutes from midnight so it
 * matches how ranges are stored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text(text = title, color = Black) },
        text = {
            TimeInput(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                colors = TimePickerDefaults.colors(
                    selectorColor = Turquoise,
                    timeSelectorSelectedContainerColor = Turquoise.copy(alpha = 0.25f)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(text = stringResource(R.string.action_confirm), color = Turquoise)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel), color = Black)
            }
        }
    )
}

@Preview
@Composable
fun TimeInputDialogPreview() {
    QuickAttendTheme {
        TimeInputDialog(
            title = "Start time",
            initialMinutes = 13 * 60,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
