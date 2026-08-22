/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component.scan

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise

/**
 * One fact about a recorded attendance: an icon, what it is, and what it says.
 * Every line of the confirmation is one of these, so the labels stay in a column
 * and the card reads as a receipt rather than as a paragraph.
 */
@Composable
fun ScanDetailRow(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = Turquoise
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size = 20.dp)
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 1.dp)
        ) {
            Text(text = label, color = Gray, fontSize = 12.sp)
            Text(
                text = value,
                color = Black,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 340)
@Composable
fun ScanDetailRowPreview() {
    QuickAttendTheme {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            ScanDetailRow(
                label = "Session",
                value = "Monday, 22 September · 13:00",
                icon = R.drawable.calendar_today
            )
            ScanDetailRow(
                label = "Checked in at",
                value = "13:02",
                icon = R.drawable.schedule
            )
            ScanDetailRow(
                label = "Location",
                value = "Salon C201 · Av. Máximo Gómez 72",
                icon = R.drawable.location_on
            )
        }
    }
}
