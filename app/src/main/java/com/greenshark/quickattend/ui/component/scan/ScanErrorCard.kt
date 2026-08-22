/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.EARLY_CHECK_IN_MINUTES
import com.greenshark.quickattend.data.formatDistance
import com.greenshark.quickattend.ui.screens.ScanError
import com.greenshark.quickattend.ui.screens.ScanErrorReason
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * A scan that produced no record, and what to do about it.
 *
 * Every reason gets its own sentence rather than one generic apology, because
 * standing in front of a QR that will not take is the moment somebody most needs
 * to be told whether to move closer, wait for the session, or ask the host.
 */
@Composable
fun ScanErrorCard(
    error: ScanError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White),
        shape = RoundedCornerShape(size = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                ErrorBadge(icon = error.reason.icon, tint = error.reason.tint)
                Text(
                    text = error.title(),
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Text(text = error.description(), color = Gray, fontSize = 15.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                if (onOpenSettings != null && error.reason.opensSettings) {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(weight = 1f)
                    ) {
                        Text(
                            text = stringResource(R.string.scan_open_settings),
                            color = Turquoise,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(weight = 1f),
                    shape = RoundedCornerShape(size = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
                ) {
                    Text(
                        text = stringResource(R.string.action_retry),
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBadge(@DrawableRes icon: Int, tint: Color) {
    Box(
        modifier = Modifier
            .background(color = tint.copy(alpha = 0.15f), shape = CircleShape)
            .padding(all = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size = 28.dp)
        )
    }
}

@Composable
private fun ScanError.title(): String = when (reason) {
    ScanErrorReason.INVALID_CODE -> stringResource(R.string.scan_error_invalid_code)
    ScanErrorReason.EVENT_NOT_FOUND -> stringResource(R.string.scan_error_event_not_found)
    ScanErrorReason.NOT_IN_SESSION -> stringResource(
        R.string.scan_error_not_in_session,
        eventTitle.orEmpty().ifBlank { stringResource(R.string.event_detail_title) }
    )

    ScanErrorReason.NOT_SIGNED_IN -> stringResource(R.string.scan_error_not_signed_in)
    ScanErrorReason.LOCATION_PERMISSION_DENIED ->
        stringResource(R.string.scan_error_location_permission)

    ScanErrorReason.LOCATION_DISABLED -> stringResource(R.string.scan_error_location_disabled)
    ScanErrorReason.LOCATION_UNAVAILABLE ->
        stringResource(R.string.scan_error_location_unavailable)

    ScanErrorReason.TOO_FAR -> stringResource(R.string.scan_error_too_far)
    ScanErrorReason.NETWORK -> stringResource(R.string.scan_error_network)
    ScanErrorReason.UNKNOWN -> stringResource(R.string.scan_error_unknown)
}

@Composable
private fun ScanError.description(): String = when (reason) {
    ScanErrorReason.INVALID_CODE -> stringResource(R.string.scan_error_invalid_code_description)
    ScanErrorReason.EVENT_NOT_FOUND ->
        stringResource(R.string.scan_error_event_not_found_description)

    ScanErrorReason.NOT_IN_SESSION -> stringResource(
        R.string.scan_error_not_in_session_description,
        EARLY_CHECK_IN_MINUTES.toInt()
    )

    ScanErrorReason.NOT_SIGNED_IN -> stringResource(R.string.scan_error_not_signed_in_description)
    ScanErrorReason.LOCATION_PERMISSION_DENIED ->
        stringResource(R.string.scan_error_location_permission_description)

    ScanErrorReason.LOCATION_DISABLED ->
        stringResource(R.string.scan_error_location_disabled_description)

    ScanErrorReason.LOCATION_UNAVAILABLE ->
        stringResource(R.string.scan_error_location_unavailable_description)

    ScanErrorReason.TOO_FAR -> stringResource(
        R.string.scan_error_too_far_description,
        formatDistance(distanceMeters ?: 0.0),
        allowedMeters ?: 0
    )

    ScanErrorReason.NETWORK -> stringResource(R.string.scan_error_network_description)
    ScanErrorReason.UNKNOWN -> stringResource(R.string.scan_error_unknown_description)
}

/** Amber for anything the attendant can fix by moving or waiting, red for the rest. */
private val ScanErrorReason.tint: Color
    get() = when (this) {
        ScanErrorReason.TOO_FAR,
        ScanErrorReason.NOT_IN_SESSION,
        ScanErrorReason.LOCATION_DISABLED,
        ScanErrorReason.LOCATION_UNAVAILABLE,
        ScanErrorReason.LOCATION_PERMISSION_DENIED -> Amber

        else -> Red
    }

private val ScanErrorReason.icon: Int
    @DrawableRes get() = when (this) {
        ScanErrorReason.TOO_FAR,
        ScanErrorReason.LOCATION_DISABLED,
        ScanErrorReason.LOCATION_UNAVAILABLE,
        ScanErrorReason.LOCATION_PERMISSION_DENIED -> R.drawable.location_on

        ScanErrorReason.NOT_IN_SESSION -> R.drawable.timer_off
        ScanErrorReason.INVALID_CODE -> R.drawable.qr_code_scanner
        else -> R.drawable.error
    }

/** Only a permission the user revoked is worth sending them to Settings for. */
private val ScanErrorReason.opensSettings: Boolean
    get() = this == ScanErrorReason.LOCATION_PERMISSION_DENIED ||
            this == ScanErrorReason.LOCATION_DISABLED

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ScanErrorCardTooFarPreview() {
    QuickAttendTheme {
        ScanErrorCard(
            error = ScanError(
                reason = ScanErrorReason.TOO_FAR,
                eventTitle = "Clase de Yoga",
                distanceMeters = 412.0,
                allowedMeters = 75
            ),
            onDismiss = {},
            onOpenSettings = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ScanErrorCardNotInSessionPreview() {
    QuickAttendTheme {
        ScanErrorCard(
            error = ScanError(
                reason = ScanErrorReason.NOT_IN_SESSION,
                eventTitle = "Clase de Yoga"
            ),
            onDismiss = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun ScanErrorCardInvalidPreview() {
    QuickAttendTheme {
        ScanErrorCard(
            error = ScanError(reason = ScanErrorReason.INVALID_CODE),
            onDismiss = {},
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}
