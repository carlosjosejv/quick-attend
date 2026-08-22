/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.ui.component.attendance.scoreColor
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red

/**
 * One attendant in the host's list: who they are, how their sessions broke
 * down, and the score those add up to. Tapping opens their own view; the
 * trailing button drops them from the event.
 */
@Composable
fun AttendeeRow(
    attendee: AttendeeAttendance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Ghost)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            AttendeeAvatar(profile = attendee.profile)

            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                Text(
                    text = attendee.profile.displayName,
                    color = Black,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.attendee_breakdown,
                        attendee.summary.onTime,
                        attendee.summary.late,
                        attendee.summary.absent
                    ),
                    color = Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (attendee.profile.hasSignatureImage) {
                    Text(
                        text = stringResource(R.string.signature_on_file),
                        color = Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = attendee.summary.score.toString(),
                color = attendee.summary.scoreColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1
            )

            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        painter = painterResource(R.drawable.person_remove),
                        contentDescription = stringResource(
                            R.string.remove_attendee_of,
                            attendee.profile.displayName
                        ),
                        tint = Red,
                        modifier = Modifier.size(size = 20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
fun AttendeeRowPreview() {
    QuickAttendTheme {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            AttendeeRow(
                attendee = previewAttendee(name = "Carlos Jiménez", onTime = 12, late = 1),
                onClick = {},
                onRemove = {}
            )
            AttendeeRow(
                attendee = previewAttendee(
                    name = "Ana Reyes",
                    onTime = 3,
                    late = 2,
                    absent = 8,
                    signed = true
                ),
                onClick = {},
                onRemove = {}
            )
        }
    }
}

internal fun previewAttendee(
    name: String,
    onTime: Int = 0,
    late: Int = 0,
    absent: Int = 0,
    signed: Boolean = false
): AttendeeAttendance = AttendeeAttendance(
    profile = UserProfile(
        id = name,
        name = name,
        email = "${name.substringBefore(' ').lowercase()}@example.com",
        signatureImage = if (signed) "placeholder" else ""
    ),
    summary = AttendanceSummary(onTime = onTime, late = late, absent = absent)
)
