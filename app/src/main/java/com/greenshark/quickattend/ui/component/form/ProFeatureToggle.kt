/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Switch that only free accounts see disabled, with a PRO badge and a short
 * explanation of what unlocking it does.
 */
@Composable
fun ProFeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isProUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Ghost, shape = RoundedCornerShape(size = 12.dp))
            .padding(all = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Text(
                    text = title,
                    color = if (isProUser) Black else Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                ProBadge()
            }
            Text(
                text = if (isProUser) description else stringResource(R.string.pro_required),
                color = Gray,
                fontSize = 13.sp
            )
        }

        Switch(
            checked = checked && isProUser,
            onCheckedChange = onCheckedChange,
            enabled = isProUser,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = Turquoise
            )
        )
    }
}

@Composable
private fun ProBadge() {
    Box(
        modifier = Modifier.background(
            color = Turquoise,
            shape = RoundedCornerShape(size = 8.dp)
        )
    ) {
        Text(
            text = stringResource(R.string.pro_badge),
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProFeatureTogglePreview() {
    QuickAttendTheme {
        ProFeatureToggle(
            title = "Location validation",
            description = "Attendants must be at the venue to check in",
            checked = true,
            onCheckedChange = {},
            isProUser = true,
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProFeatureToggleLockedPreview() {
    QuickAttendTheme {
        ProFeatureToggle(
            title = "Location validation",
            description = "Attendants must be at the venue to check in",
            checked = false,
            onCheckedChange = {},
            isProUser = false,
            modifier = Modifier.padding(all = 16.dp)
        )
    }
}
