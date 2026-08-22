/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.signature

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.White

/**
 * Where an attendant sets the signature that goes on every record they appear
 * in. Shows the one already on file, so replacing it is a deliberate act rather
 * than something that happens by opening the dialog.
 */
@Composable
fun SignatureCaptureDialog(
    onSigned: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    existingSignature: String = "",
    onRemove: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 24.dp),
            color = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 20.dp),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.signature_title),
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = stringResource(R.string.signature_description),
                    color = Gray,
                    fontSize = 14.sp
                )

                if (existingSignature.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.signature_current),
                        color = Gray,
                        fontSize = 13.sp
                    )
                    SignatureView(
                        signature = AttendanceSignature.Drawn(
                            imageBase64 = existingSignature,
                            code = ""
                        )
                    )
                }

                SignaturePad(onSigned = onSigned)

                DialogActions(
                    onDismiss = onDismiss,
                    onRemove = onRemove?.takeIf { existingSignature.isNotBlank() }
                )
            }
        }
    }
}

@Composable
private fun DialogActions(onDismiss: () -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // Removing sits opposite closing, so the destructive one is never the
        // button a thumb lands on by reflex.
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onRemove != null) {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.signature_remove), color = Red)
            }
        } else {
            Spacer(modifier = Modifier)
        }
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.action_cancel), color = Gray)
        }
    }
}

@Preview
@Composable
fun SignatureCaptureDialogPreview() {
    QuickAttendTheme {
        SignatureCaptureDialog(onSigned = {}, onDismiss = {})
    }
}
