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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.component.DayItem
import com.greenshark.quickattend.ui.component.common.NavCard
import com.greenshark.quickattend.ui.component.MainEvent
import com.greenshark.quickattend.ui.component.Shortcut
import com.greenshark.quickattend.ui.component.UpcomingEventsRow
import com.greenshark.quickattend.ui.component.previewOccurrences
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Created by Carlos Jiménez on 01-Aug-24.
 */

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onAction = viewModel::onAction,
        onScan = { navController.navigate(NavigationItem.Scan.route) },
        onEventClick = { occurrence ->
            navController.navigate(
                NavigationItem.AttendanceDetail.createRoute(
                    eventId = occurrence.event.id,
                    date = occurrence.date
                )
            )
        },
        onViewAllEvents = { navController.navigate(NavigationItem.UpcomingEvents.route) },
        onManagedEvents = { navController.navigate(NavigationItem.ManagedEvents.route) }
    )
}

@Composable
fun HomeContent(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    onScan: () -> Unit,
    onEventClick: (EventOccurrence) -> Unit,
    onViewAllEvents: () -> Unit,
    onManagedEvents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {

        val locale = LocalConfiguration.current.locales[0]
        val monthName = remember(state.selectedDate.month, locale) {
            state.selectedDate.month.getDisplayName(TextStyle.FULL, locale)
        }
        val dayListState = rememberLazyListState(
            initialFirstVisibleItemIndex = state.today.dayOfMonth - 1
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
                    items = state.monthDays,
                    key = { day -> day.dayOfMonth }
                ) { day ->
                    DayItem(
                        onClick = { onAction(HomeAction.DateSelected(day)) },
                        dayName = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        dayNumber = day.dayOfMonth,
                        today = day == state.today,
                        selected = day == state.selectedDate
                    )
                }
            }
        }

        // The featured card only makes sense while the selected day still has
        // something running or ahead of it; otherwise the scanner is the shortcut.
        val mainOccurrence = state.mainOccurrence
        if (mainOccurrence != null) {
            MainEvent(
                onClick = { onEventClick(mainOccurrence) },
                seeDetails = { onEventClick(mainOccurrence) },
                occurrence = mainOccurrence
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Shortcut(
                    onClick = onScan,
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.mark_attendance),
                    subtitle = stringResource(R.string.shortcut_1),
                    icon = R.drawable.qr_code_scanner
                )
            }
        }

        UpcomingEventsRow(
            occurrences = state.upcoming,
            onEventClick = onEventClick,
            onViewAll = onViewAllEvents
        )

        NavCard(
            title = stringResource(R.string.managed_events),
            subtitle = stringResource(R.string.description_2),
            icon = R.drawable.group,
            onClick = onManagedEvents
        )
    }
}


@Preview(showBackground = true, heightDp = 1200)
@Composable
fun HomeContentPreview() {
    val today = LocalDate.of(2026, 8, 24)
    val occurrences = previewOccurrences()
    QuickAttendTheme {
        HomeContent(
            state = HomeState(
                today = today,
                selectedDate = today,
                monthDays = (1..today.lengthOfMonth()).map(today::withDayOfMonth),
                mainOccurrence = occurrences.first(),
                upcoming = occurrences
            ),
            onAction = {},
            onScan = {},
            onEventClick = {},
            onViewAllEvents = {},
            onManagedEvents = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
fun HomeContentFreeDayPreview() {
    val today = LocalDate.of(2026, 8, 24)
    QuickAttendTheme {
        HomeContent(
            state = HomeState(
                today = today,
                selectedDate = today.plusDays(1),
                monthDays = (1..today.lengthOfMonth()).map(today::withDayOfMonth),
                mainOccurrence = null,
                upcoming = previewOccurrences()
            ),
            onAction = {},
            onScan = {},
            onEventClick = {},
            onViewAllEvents = {},
            onManagedEvents = {}
        )
    }
}
