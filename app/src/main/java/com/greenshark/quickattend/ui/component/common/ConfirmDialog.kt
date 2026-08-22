/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Asks before something that cannot be undone. [destructive] paints the confirm
 * action red, so deleting an event never looks like saving one.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = White,
        title = {
            Text(text = title, color = Black, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, color = Gray)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) Red else Turquoise,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel), color = Gray)
            }
        }
    )
}

@Preview
@Composable
fun ConfirmDialogPreview() {
    QuickAttendTheme {
        ConfirmDialog(
            title = "Delete this event?",
            message = "Every sign-up and attendance record for Clase de Yoga is " +
                    "deleted with it. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
            destructive = true
        )
    }
}
