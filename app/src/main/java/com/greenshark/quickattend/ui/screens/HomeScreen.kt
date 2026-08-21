package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.ui.component.DayItem
import com.greenshark.quickattend.ui.component.Shortcut
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {

        val locale = LocalConfiguration.current.locales[0]
        val today = LocalDate.now()
        val monthName = remember(today.month, locale) {
            today.month.getDisplayName(TextStyle.FULL, locale)
        }
        val monthDays = remember(today.year, today.month) {
            (1..today.lengthOfMonth()).map(today::withDayOfMonth)
        }
        val dayListState = rememberLazyListState(
            initialFirstVisibleItemIndex = today.dayOfMonth - 1
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {

            Text(
                text = monthName,
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(alignment = Alignment.Start)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = dayListState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = dayListState),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(
                    items = monthDays,
                    key = { day -> day.dayOfMonth }
                ) { day ->
                    DayItem(
                        onClick = {

                        },
                        dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        dayNumber = day.dayOfMonth,
                        today = day == today
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            Shortcut(
                onClick = {

                },
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.mark_attendance),
                subtitle = stringResource(R.string.shortcut_1),
                icon = R.drawable.qr_code_scanner
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    QuickAttendTheme {
        HomeScreen(navController = rememberNavController())
    }
}
