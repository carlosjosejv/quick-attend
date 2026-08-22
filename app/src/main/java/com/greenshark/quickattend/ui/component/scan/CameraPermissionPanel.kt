/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * What fills the scan screen while there is no camera to fill it with.
 *
 * [isPermanentlyDenied] changes the offer rather than the wording alone: once
 * Android stops showing its own dialog, a button that asks again does nothing,
 * so the only honest action left is a trip to Settings.
 */
@Composable
fun CameraPermissionPanel(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isPermanentlyDenied: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 14.dp, alignment = Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(R.drawable.qr_code_scanner),
            contentDescription = null,
            tint = Turquoise,
            modifier = Modifier.size(size = 64.dp)
        )
        Text(
            text = stringResource(R.string.scan_camera_permission_title),
            color = Black,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(
                if (isPermanentlyDenied) {
                    R.string.scan_camera_permission_denied
                } else {
                    R.string.scan_camera_permission_description
                }
            ),
            color = Gray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        if (isPermanentlyDenied) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(size = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
            ) {
                Text(
                    text = stringResource(R.string.scan_open_settings),
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            }
            return@Column
        }

        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
        ) {
            Text(
                text = stringResource(R.string.scan_camera_permission_grant),
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(text = stringResource(R.string.scan_open_settings), color = Gray)
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
fun CameraPermissionPanelPreview() {
    QuickAttendTheme {
        CameraPermissionPanel(onGrant = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
fun CameraPermissionPanelDeniedPreview() {
    QuickAttendTheme {
        CameraPermissionPanel(
            onGrant = {},
            onOpenSettings = {},
            isPermanentlyDenied = true
        )
    }
}
