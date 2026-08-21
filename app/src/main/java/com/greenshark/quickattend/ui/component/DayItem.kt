/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * One day of the calendar row. [today] tints the day the user is actually on,
 * [selected] marks the day whose events are being shown.
 */
@Composable
fun DayItem(
    onClick: () -> Unit,
    dayName: String,
    dayNumber: Int,
    today: Boolean = false,
    selected: Boolean = false
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.size(width = 80.dp, height = 100.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                selected -> Turquoise
                today -> Turquoise.copy(alpha = 0.2f)
                else -> White
            },
            contentColor = if (selected) White else Black
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = dayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = dayNumber.toString(),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayItemPreview() {
    QuickAttendTheme {
        DayItem(onClick = {}, dayName = "Mon", dayNumber = 24, today = true, selected = true)
    }
}

@Preview(showBackground = true)
@Composable
fun DayItemTodayPreview() {
    QuickAttendTheme {
        DayItem(onClick = {}, dayName = "Mon", dayNumber = 24, today = true)
    }
}