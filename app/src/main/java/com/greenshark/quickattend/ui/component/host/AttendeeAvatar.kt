/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Initials bubble standing in for an attendant's photo. The tint is derived
 * from the account id, so the same person keeps the same colour on every screen
 * and a list of names is easier to scan.
 */
@Composable
fun AttendeeAvatar(
    profile: UserProfile,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size = size)
            .background(color = profile.avatarColor(), shape = CircleShape)
            // The name is always right next to it, so a reader hears it twice
            // unless the bubble keeps quiet.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.initials,
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / AVATAR_TEXT_RATIO).sp,
            maxLines = 1
        )
    }
}

/** Stable per account, and always dark enough for white initials to read. */
private fun UserProfile.avatarColor(): Color {
    val hue = (id.ifBlank { displayName }.hashCode().rem(HUE_STEPS) + HUE_STEPS)
        .rem(HUE_STEPS) * (FULL_HUE / HUE_STEPS)
    return Color.hsv(hue = hue.toFloat(), saturation = AVATAR_SATURATION, value = AVATAR_VALUE)
}

private const val AVATAR_TEXT_RATIO = 2.5f
private const val HUE_STEPS = 12
private const val FULL_HUE = 360
private const val AVATAR_SATURATION = 0.55f
private const val AVATAR_VALUE = 0.72f

@Preview(showBackground = true, widthDp = 200)
@Composable
fun AttendeeAvatarPreview() {
    QuickAttendTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            modifier = Modifier.background(color = Turquoise.copy(alpha = 0.1f))
        ) {
            listOf("Carlos Jiménez", "Ana Reyes", "Luis Pérez").forEachIndexed { index, name ->
                AttendeeAvatar(profile = UserProfile(id = "u$index", name = name))
            }
        }
    }
}
