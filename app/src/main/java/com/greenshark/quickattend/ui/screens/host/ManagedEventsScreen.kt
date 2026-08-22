/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventStatus
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.component.common.EmptyState
import com.greenshark.quickattend.ui.component.common.LoadingBox
import com.greenshark.quickattend.ui.component.host.HostEventCard
import com.greenshark.quickattend.ui.component.host.previewHostEvent
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/** Everything the signed in user hosts, and the way into managing one. */
@Composable
fun ManagedEventsScreen(
    navController: NavController,
    viewModel: ManagedEventsViewModel = viewModel(factory = ManagedEventsViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ManagedEventsContent(
        state = state,
        onBack = { navController.popBackStack() },
        onEventClick = { event ->
            navController.navigate(NavigationItem.HostEventDetail.createRoute(event.id))
        },
        onCreateEvent = { navController.navigate(NavigationItem.EventForm.createRoute()) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagedEventsContent(
    state: ManagedEventsState,
    onBack: () -> Unit,
    onEventClick: (Event) -> Unit,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.managed_events),
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateEvent,
                containerColor = Turquoise,
                contentColor = White,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.calendar_add),
                        contentDescription = null
                    )
                },
                text = { Text(text = stringResource(R.string.create_event)) }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(modifier = Modifier.padding(innerPadding))
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
                text = stringResource(R.string.managed_events_description),
                color = Gray,
                fontSize = 16.sp
            )

            state.error?.let { error ->
                Text(text = error.message(), color = Red, fontSize = 16.sp)
            }

            if (state.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.no_managed_events),
                    description = stringResource(R.string.no_managed_events_description),
                    icon = R.drawable.calendar_add,
                    actionLabel = stringResource(R.string.create_event),
                    onAction = onCreateEvent
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                // Clears the floating button, which would otherwise sit on the
                // last card.
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(
                    items = state.events,
                    key = { managed -> managed.event.id }
                ) { managed ->
                    HostEventCard(
                        event = managed.event,
                        onClick = { onEventClick(managed.event) },
                        status = managed.status
                    )
                }
            }
        }
    }
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "Sign in again to see the events you host."
    DataError.NOT_FOUND -> "Those events are no longer available."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun ManagedEventsContentPreview() {
    QuickAttendTheme {
        ManagedEventsContent(
            state = ManagedEventsState(
                events = listOf(
                    ManagedEvent(previewHostEvent(), EventStatus.IN_PROGRESS),
                    ManagedEvent(previewHostEvent().copy(id = "2", title = "Taller de Diseño"))
                )
            ),
            onBack = {},
            onEventClick = {},
            onCreateEvent = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun ManagedEventsContentEmptyPreview() {
    QuickAttendTheme {
        ManagedEventsContent(
            state = ManagedEventsState(),
            onBack = {},
            onEventClick = {},
            onCreateEvent = {}
        )
    }
}
