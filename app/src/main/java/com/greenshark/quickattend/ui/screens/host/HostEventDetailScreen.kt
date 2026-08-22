/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.EventAttendanceOverview
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.component.common.ConfirmDialog
import com.greenshark.quickattend.ui.component.common.EmptyState
import com.greenshark.quickattend.ui.component.common.LoadingBox
import com.greenshark.quickattend.ui.component.common.SectionTitle
import com.greenshark.quickattend.ui.component.host.AttendeeRow
import com.greenshark.quickattend.ui.component.host.AttendeeSearchField
import com.greenshark.quickattend.ui.component.host.AttendeeSortRow
import com.greenshark.quickattend.ui.component.host.EventOverviewCard
import com.greenshark.quickattend.ui.component.host.previewAttendee
import com.greenshark.quickattend.ui.component.host.previewHostEvent
import com.greenshark.quickattend.ui.component.host.scheduleLine
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * One event as its host sees it: the roll, what it adds up to, and the four
 * things a host can do with it — edit the schedule, export the record, remove
 * someone, or delete the whole thing.
 */
@Composable
fun HostEventDetailScreen(
    navController: NavController,
    eventId: String,
    viewModel: HostEventDetailViewModel = viewModel(
        factory = HostEventDetailViewModel.factory(eventId)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // LocalResources rather than LocalContext, so the strings follow a locale
    // change instead of going stale on the one the screen opened in.
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HostEventDetailEvent.AttendeeRemoved -> snackbarHostState.showMessage(
                    resources.getString(R.string.attendee_removed, event.name)
                )

                HostEventDetailEvent.EventDeleted -> {
                    snackbarHostState.showMessage(resources.getString(R.string.event_deleted))
                    navController.popBackStack()
                }

                is HostEventDetailEvent.Error ->
                    snackbarHostState.showMessage(event.error.message())
            }
        }
    }

    HostEventDetailContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() },
        onEditSchedule = {
            navController.navigate(NavigationItem.EventForm.createRoute(eventId))
        },
        onExportAll = {
            navController.navigate(NavigationItem.ExportPreview.createRoute(eventId))
        },
        onAttendeeClick = { attendee ->
            navController.navigate(
                NavigationItem.HostAttendeeDetail.createRoute(
                    eventId = eventId,
                    userId = attendee.profile.id
                )
            )
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEventDetailContent(
    state: HostEventDetailState,
    onAction: (HostEventDetailAction) -> Unit,
    onBack: () -> Unit,
    onEditSchedule: () -> Unit,
    onExportAll: () -> Unit,
    onAttendeeClick: (AttendeeAttendance) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.event?.title
                                ?: stringResource(R.string.event_detail_title),
                            color = Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.event?.let { event ->
                            Text(
                                text = event.schedule.scheduleLine(),
                                color = Gray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = onExportAll, enabled = state.canExport) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = stringResource(R.string.action_export),
                            tint = if (state.canExport) Turquoise else Gray
                        )
                    }
                    IconButton(
                        onClick = { onAction(HostEventDetailAction.DeleteRequested) },
                        enabled = state.event != null && !state.isWorking
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.action_delete_event),
                            tint = Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            if (state.isWorking) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Turquoise
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                state.error?.let { error ->
                    item(key = "error") {
                        Text(text = error.message(), color = Red, fontSize = 16.sp)
                    }
                }

                item(key = "actions") {
                    HostActions(
                        onEditSchedule = onEditSchedule,
                        onExportAll = onExportAll,
                        exportEnabled = state.canExport
                    )
                }

                item(key = "overview-title") {
                    SectionTitle(
                        text = stringResource(R.string.section_overview),
                        icon = R.drawable.monitoring
                    )
                }

                item(key = "overview") {
                    EventOverviewCard(overview = state.overview)
                }

                item(key = "attendants-title") {
                    SectionTitle(
                        text = stringResource(R.string.section_attendants),
                        icon = R.drawable.group
                    )
                }

                if (!state.hasAttendees) {
                    item(key = "no-attendants") {
                        EmptyState(
                            title = stringResource(R.string.no_attendants),
                            description = stringResource(R.string.no_attendants_description),
                            icon = R.drawable.group
                        )
                    }
                    return@LazyColumn
                }

                item(key = "search") {
                    AttendeeSearchField(
                        query = state.query,
                        onQueryChange = { onAction(HostEventDetailAction.QueryChanged(it)) }
                    )
                }

                item(key = "sort") {
                    AttendeeSortRow(
                        selected = state.sort,
                        onSelect = { onAction(HostEventDetailAction.SortChanged(it)) }
                    )
                }

                if (state.isFilteredEmpty) {
                    item(key = "no-match") {
                        EmptyState(
                            title = stringResource(R.string.no_attendants_match),
                            icon = R.drawable.search
                        )
                    }
                    return@LazyColumn
                }

                items(
                    items = state.visibleAttendees,
                    key = { attendee -> attendee.key }
                ) { attendee ->
                    AttendeeRow(
                        attendee = attendee,
                        onClick = { onAttendeeClick(attendee) },
                        onRemove = {
                            onAction(HostEventDetailAction.RemoveRequested(attendee))
                        }
                    )
                }
            }
        }
    }

    state.pendingRemoval?.let { attendee ->
        ConfirmDialog(
            title = stringResource(R.string.remove_attendee),
            message = stringResource(
                R.string.remove_attendee_message,
                attendee.profile.displayName
            ),
            confirmLabel = stringResource(R.string.remove_attendee),
            onConfirm = { onAction(HostEventDetailAction.RemoveConfirmed) },
            onDismiss = { onAction(HostEventDetailAction.RemoveDismissed) },
            destructive = true
        )
    }

    if (state.isConfirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_event_title),
            message = stringResource(
                R.string.delete_event_message,
                state.event?.title.orEmpty()
            ),
            confirmLabel = stringResource(R.string.action_delete_event),
            onConfirm = { onAction(HostEventDetailAction.DeleteConfirmed) },
            onDismiss = { onAction(HostEventDetailAction.DeleteDismissed) },
            destructive = true
        )
    }
}

@Composable
private fun HostActions(
    onEditSchedule: () -> Unit,
    onExportAll: () -> Unit,
    exportEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        HostActionButton(
            label = stringResource(R.string.action_edit_schedule),
            icon = R.drawable.edit,
            onClick = onEditSchedule,
            modifier = Modifier.weight(weight = 1f)
        )
        HostActionButton(
            label = stringResource(R.string.action_export),
            icon = R.drawable.download,
            onClick = onExportAll,
            enabled = exportEnabled,
            modifier = Modifier.weight(weight = 1f)
        )
    }
}

@Composable
private fun HostActionButton(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Turquoise
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(size = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = if (enabled) tint else Gray,
                modifier = Modifier.size(size = 18.dp)
            )
            Text(
                text = label,
                color = if (enabled) Black else Gray,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "You are not allowed to manage this event."
    DataError.NOT_FOUND -> "This event no longer exists."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
fun HostEventDetailContentPreview() {
    QuickAttendTheme {
        HostEventDetailContent(
            state = previewHostState(),
            onAction = {},
            onBack = {},
            onEditSchedule = {},
            onExportAll = {},
            onAttendeeClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun HostEventDetailContentEmptyPreview() {
    QuickAttendTheme {
        HostEventDetailContent(
            state = HostEventDetailState(event = previewHostEvent()),
            onAction = {},
            onBack = {},
            onEditSchedule = {},
            onExportAll = {},
            onAttendeeClick = {}
        )
    }
}

private fun previewHostState(): HostEventDetailState {
    val attendees = listOf(
        previewAttendee(name = "Carlos Jiménez", onTime = 11, late = 1),
        previewAttendee(name = "Ana Reyes", onTime = 3, late = 2, absent = 7, signed = true),
        previewAttendee(name = "Luis Pérez", onTime = 8, late = 4)
    )
    return HostEventDetailState(
        event = previewHostEvent(),
        attendees = attendees,
        overview = EventAttendanceOverview(
            attendees = attendees.size,
            sessions = 12,
            summary = AttendanceSummary(onTime = 22, late = 7, absent = 7)
        )
    )
}
