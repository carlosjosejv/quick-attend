/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.summarize
import com.greenshark.quickattend.ui.commons.NavigationItem
import com.greenshark.quickattend.ui.component.attendance.AttendanceBreakdown
import com.greenshark.quickattend.ui.component.attendance.AttendanceCheckInCard
import com.greenshark.quickattend.ui.component.attendance.AttendanceHistoryChart
import com.greenshark.quickattend.ui.component.attendance.AttendanceRecordRow
import com.greenshark.quickattend.ui.component.attendance.AttendanceScoreRing
import com.greenshark.quickattend.ui.component.attendance.previewRecords
import com.greenshark.quickattend.ui.component.common.EmptyState
import com.greenshark.quickattend.ui.component.common.LoadingBox
import com.greenshark.quickattend.ui.component.common.SectionTitle
import com.greenshark.quickattend.ui.component.host.AttendeeAvatar
import com.greenshark.quickattend.ui.component.host.previewHostEvent
import com.greenshark.quickattend.ui.component.signature.SignatureView
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Ghost
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * One attendant's record on one event, as the host sees it: every session they
 * were scheduled for, how they did, and the signature that goes on the export.
 */
@Composable
fun HostAttendeeDetailScreen(
    navController: NavController,
    eventId: String,
    userId: String,
    viewModel: HostAttendeeDetailViewModel = viewModel(
        factory = HostAttendeeDetailViewModel.factory(eventId = eventId, userId = userId)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HostAttendeeDetailContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() },
        onExport = {
            navController.navigate(
                NavigationItem.ExportPreview.createRoute(eventId = eventId, userId = userId)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostAttendeeDetailContent(
    state: HostAttendeeDetailState,
    onAction: (HostAttendeeDetailAction) -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.profile?.displayName
                                ?: stringResource(R.string.attendee_detail_title),
                            color = Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.event?.title?.takeIf { it.isNotBlank() }?.let { title ->
                            Text(text = title, color = Gray, fontSize = 13.sp, maxLines = 1)
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
                    IconButton(onClick = onExport, enabled = state.canExport) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = stringResource(R.string.export_this_attendant),
                            tint = if (state.canExport) Turquoise else Gray
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

        val attendee = state.attendee
        if (attendee == null) {
            EmptyState(
                title = state.error?.message() ?: stringResource(R.string.no_attendance_records),
                icon = R.drawable.group,
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            state.error?.let { error ->
                item(key = "error") {
                    Text(text = error.message(), color = Red, fontSize = 16.sp)
                }
            }

            item(key = "identity") {
                AttendeeIdentity(profile = attendee.profile)
            }

            state.selectedRecord?.let { record ->
                item(key = "record") {
                    AttendanceCheckInCard(record = record)
                }
            }

            item(key = "signature-title") {
                SectionTitle(text = stringResource(R.string.signature), icon = R.drawable.draw)
            }

            item(key = "signature") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Ghost, shape = RoundedCornerShape(size = 16.dp))
                        .padding(all = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 6.dp)
                ) {
                    SignatureView(signature = state.selectedSignature)
                    if (state.selectedSignature is AttendanceSignature.Code) {
                        Text(
                            text = stringResource(R.string.signature_code_explained),
                            color = Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item(key = "score-title") {
                SectionTitle(
                    text = stringResource(R.string.attendance_score),
                    icon = R.drawable.monitoring
                )
            }

            item(key = "ring") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AttendanceScoreRing(summary = attendee.summary)
                }
            }

            item(key = "breakdown") {
                AttendanceBreakdown(summary = attendee.summary)
            }

            if (attendee.records.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.no_attendance_records),
                        color = Gray,
                        fontSize = 15.sp
                    )
                }
                return@LazyColumn
            }

            item(key = "chart") {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    SectionTitle(
                        text = stringResource(R.string.attendee_sessions),
                        icon = R.drawable.list_alt_check
                    )
                    AttendanceHistoryChart(records = attendee.records)
                }
            }

            items(
                items = state.records,
                key = { record -> record.key }
            ) { record ->
                AttendanceRecordRow(
                    record = record,
                    onClick = { onAction(HostAttendeeDetailAction.RecordSelected(record.key)) },
                    selected = record.key == state.selectedRecord?.key
                )
            }
        }
    }
}

@Composable
private fun AttendeeIdentity(profile: UserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        AttendeeAvatar(profile = profile, size = 52.dp)
        Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
            Text(
                text = profile.displayName,
                color = Black,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    color = Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "You are not allowed to see this attendant."
    DataError.NOT_FOUND -> "This attendant is no longer on the event."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
fun HostAttendeeDetailContentPreview() {
    val records = previewRecords()
    QuickAttendTheme {
        HostAttendeeDetailContent(
            state = HostAttendeeDetailState(
                event = previewHostEvent(),
                attendee = AttendeeAttendance(
                    profile = UserProfile(
                        id = "1",
                        name = "Ana Reyes",
                        email = "ana.reyes@example.com"
                    ),
                    records = records,
                    summary = records.summarize()
                )
            ),
            onAction = {},
            onBack = {},
            onExport = {}
        )
    }
}
