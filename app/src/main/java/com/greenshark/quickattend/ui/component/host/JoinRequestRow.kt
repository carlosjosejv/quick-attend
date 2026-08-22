/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.ui.theme.Amber
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White
import java.time.format.DateTimeFormatter

/**
 * Somebody who checked in without being on the roll, and the host's two answers.
 *
 * Accepting is the affirmative action and gets the coloured button; ignoring is
 * deliberately quiet, because it is not a deletion — the attendance stays in the
 * backend either way and ignoring only keeps it out of the roll.
 */
@Composable
fun JoinRequestRow(
    request: JoinRequest,
    onAccept: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val locale = LocalConfiguration.current.locales[0]
    val scannedLabel = remember(request.firstCheckInAt, request.requestedAt, locale) {
        (request.firstCheckInDateTime ?: request.requestedAtDateTime)
            ?.format(DateTimeFormatter.ofPattern(SCANNED_PATTERN, locale))
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                InitialsBubble(initials = request.initials())

                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 2.dp)
                ) {
                    Text(
                        text = request.displayName,
                        color = Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = scannedLabel
                            ?.let { stringResource(R.string.join_request_scanned_at, it) }
                            ?: request.userEmail,
                        color = Gray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                TextButton(
                    onClick = onIgnore,
                    enabled = enabled,
                    modifier = Modifier.weight(weight = 1f)
                ) {
                    Text(
                        text = stringResource(R.string.join_request_ignore),
                        color = if (enabled) Red else Gray
                    )
                }
                OutlinedButton(
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier.weight(weight = 1f),
                    shape = RoundedCornerShape(size = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = null,
                            tint = if (enabled) Turquoise else Gray,
                            modifier = Modifier.size(size = 18.dp)
                        )
                        Text(
                            text = stringResource(R.string.join_request_accept),
                            color = if (enabled) Black else Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/** Amber rather than turquoise: a request is something outstanding, not something done. */
@Composable
private fun InitialsBubble(initials: String) {
    Box(
        modifier = Modifier
            .size(size = 42.dp)
            .background(color = Amber.copy(alpha = 0.18f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Two letters for the bubble, e.g. "AR" for "Ana Reyes". */
private fun JoinRequest.initials(): String = displayName
    .split(' ')
    .filter { it.isNotBlank() }
    .take(n = 2)
    .joinToString(separator = "") { it.first().uppercase() }
    .ifBlank { "?" }

private const val SCANNED_PATTERN = "d MMM, HH:mm"

@Preview(showBackground = true, widthDp = 380)
@Composable
fun JoinRequestRowPreview() {
    QuickAttendTheme {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            JoinRequestRow(
                request = previewJoinRequest(name = "Ana Reyes"),
                onAccept = {},
                onIgnore = {}
            )
            JoinRequestRow(
                request = previewJoinRequest(name = "Luis Pérez"),
                onAccept = {},
                onIgnore = {},
                enabled = false
            )
        }
    }
}

internal fun previewJoinRequest(name: String): JoinRequest = JoinRequest(
    id = "u1_e1",
    eventId = "e1",
    eventTitle = "Clase de Yoga",
    userId = "u1",
    userName = name,
    userEmail = "${name.substringBefore(' ').lowercase()}@mail.com",
    hostId = "host",
    requestedAt = System.currentTimeMillis(),
    firstCheckInAt = System.currentTimeMillis()
)
