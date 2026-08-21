/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.component.UpcomingEventCard
import com.greenshark.quickattend.ui.component.previewOccurrences
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** The whole schedule ahead, reached from the home carousel's "view all". */
@Composable
fun UpcomingEventsScreen(
    navController: NavController,
    viewModel: UpcomingEventsViewModel = viewModel(factory = UpcomingEventsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    UpcomingEventsContent(
        state = state,
        onBack = { navController.popBackStack() },
        onEventClick = { occurrence ->
            navController.navigate(
                NavigationItem.AttendanceDetail.createRoute(
                    eventId = occurrence.event.id,
                    date = occurrence.date
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingEventsContent(
    state: UpcomingEventsState,
    onBack: () -> Unit,
    onEventClick: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.my_upcoming_events),
                        color = Black,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_return),
                            contentDescription = stringResource(R.string.action_back),
                            tint = Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Turquoise)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.upcoming_events_description),
                color = Gray,
                fontSize = 16.sp
            )

            state.error?.let { error ->
                Text(text = error.message(), color = Red, fontSize = 16.sp)
            }

            if (state.occurrences.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_upcoming_events),
                        color = Gray,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(
                    items = state.occurrences,
                    key = { occurrence -> occurrence.key }
                ) { occurrence ->
                    UpcomingEventCard(
                        occurrence = occurrence,
                        onClick = { onEventClick(occurrence) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "Sign in again to see your events."
    DataError.NOT_FOUND -> "Those events are no longer available."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun UpcomingEventsContentPreview() {
    QuickAttendTheme {
        UpcomingEventsContent(
            state = UpcomingEventsState(occurrences = previewOccurrences()),
            onBack = {},
            onEventClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun UpcomingEventsContentEmptyPreview() {
    QuickAttendTheme {
        UpcomingEventsContent(
            state = UpcomingEventsState(),
            onBack = {},
            onEventClick = {}
        )
    }
}
