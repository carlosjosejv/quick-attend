/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceStatus
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.summarize
import com.greenshark.quickattend.ui.component.attendance.AttendanceBreakdown
import com.greenshark.quickattend.ui.component.attendance.AttendanceCheckInCard
import com.greenshark.quickattend.ui.component.attendance.AttendanceHistoryChart
import com.greenshark.quickattend.ui.component.attendance.AttendanceRecordRow
import com.greenshark.quickattend.ui.component.attendance.AttendanceScoreRing
import com.greenshark.quickattend.ui.component.attendance.previewAttendanceEvent
import com.greenshark.quickattend.ui.component.attendance.previewRecords
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Red
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White
import java.time.LocalDate

/**
 * The attendance the signed in user built up on one event: the session they
 * tapped, how punctual they were on it, and the score every past session of the
 * event adds up to.
 */
@Composable
fun AttendanceDetailScreen(
    navController: NavController,
    eventId: String,
    date: LocalDate? = null,
    viewModel: AttendanceDetailViewModel = viewModel(
        factory = AttendanceDetailViewModel.factory(eventId = eventId, date = date)
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AttendanceDetailContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailContent(
    state: AttendanceDetailState,
    onAction: (AttendanceDetailAction) -> Unit,
    onBack: () -> Unit,
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
                            text = state.event?.title
                                ?: stringResource(R.string.attendance_detail_title),
                            color = Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.event?.location?.takeIf { it.isNotBlank() }?.let { location ->
                            Text(text = location, color = Gray, fontSize = 13.sp, maxLines = 1)
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            state.error?.let { error ->
                item {
                    Text(text = error.message(), color = Red, fontSize = 16.sp)
                }
            }

            state.selectedRecord?.let { record ->
                item(key = "record") {
                    AttendanceCheckInCard(record = record)
                }
            }

            item(key = "score") {
                SectionTitle(text = stringResource(R.string.attendance_score))
            }

            item(key = "ring") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    AttendanceScoreRing(summary = state.summary)
                    Text(
                        text = stringResource(R.string.attendance_score_description),
                        color = Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item(key = "breakdown") {
                AttendanceBreakdown(summary = state.summary)
            }

            if (state.records.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.no_attendance_records),
                        color = Gray,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                return@LazyColumn
            }

            item(key = "chart") {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    SectionTitle(text = stringResource(R.string.attendance_history))
                    Text(
                        text = stringResource(R.string.attendance_history_chart_description),
                        color = Gray,
                        fontSize = 13.sp
                    )
                    AttendanceHistoryChart(records = state.records)
                }
            }

            items(
                items = state.recentFirst,
                key = { record -> record.key }
            ) { record ->
                AttendanceRecordRow(
                    record = record,
                    onClick = { onAction(AttendanceDetailAction.RecordSelected(record.key)) },
                    selected = record.key == state.selectedRecord?.key
                )
            }
        }
    }
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Your attendance may not be up to date."
    DataError.UNAUTHORIZED -> "Sign in again to see your attendance."
    DataError.NOT_FOUND -> "This event no longer exists."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.monitoring),
            contentDescription = null,
            tint = Turquoise,
            modifier = Modifier.size(size = 20.dp)
        )
        Text(text = text, color = Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
fun AttendanceDetailContentPreview() {
    QuickAttendTheme {
        AttendanceDetailContent(
            state = previewAttendanceState(),
            onAction = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
fun AttendanceDetailContentAbsentPreview() {
    QuickAttendTheme {
        AttendanceDetailContent(
            state = previewAttendanceState { it.status == AttendanceStatus.ABSENT },
            onAction = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
fun AttendanceDetailContentEmptyPreview() {
    QuickAttendTheme {
        AttendanceDetailContent(
            state = AttendanceDetailState(event = previewAttendanceEvent()),
            onAction = {},
            onBack = {}
        )
    }
}

private fun previewAttendanceState(
    select: (AttendanceRecord) -> Boolean = { false }
): AttendanceDetailState {
    val records = previewRecords()
    return AttendanceDetailState(
        event = previewAttendanceEvent(),
        records = records,
        summary = records.summarize(),
        selectedKey = (records.firstOrNull(select) ?: records.last()).key
    )
}
