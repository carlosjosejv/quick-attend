/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.greenshark.quickattend.R
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.daysSummary
import com.greenshark.quickattend.data.timeSummary
import com.greenshark.quickattend.ui.component.QrCodePreview
import com.greenshark.quickattend.ui.component.form.FormSection
import com.greenshark.quickattend.ui.component.form.FormTextField
import com.greenshark.quickattend.ui.component.form.MapLocationField
import com.greenshark.quickattend.ui.component.form.ProFeatureToggle
import com.greenshark.quickattend.ui.component.form.ScheduleEditor
import com.greenshark.quickattend.ui.theme.Black
import com.greenshark.quickattend.ui.theme.Gray
import com.greenshark.quickattend.ui.theme.QuickAttendTheme
import com.greenshark.quickattend.ui.theme.Turquoise
import com.greenshark.quickattend.ui.theme.White

/**
 * Create or edit an event. Pass an [eventId] to edit an existing one, or null
 * to create a new one.
 */
@Composable
fun EventFormScreen(
    navController: NavController,
    eventId: String? = null,
    viewModel: EventFormViewModel = viewModel(factory = EventFormViewModel.factory(eventId))
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // LocalResources rather than LocalContext, so the message follows a locale
    // change instead of going stale on the one the form opened in.
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EventFormEvent.Saved -> {
                    snackbarHostState.showMessage(resources.savedMessage(event.attendeesNotified))
                    navController.popBackStack()
                }

                is EventFormEvent.Error -> snackbarHostState.showMessage(event.error.message())
            }
        }
    }

    EventFormContent(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.popBackStack() },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormContent(
    state: EventFormState,
    onAction: (EventFormAction) -> Unit,
    onBack: () -> Unit,
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
                    Text(
                        text = stringResource(
                            if (state.isEditing) R.string.edit_event else R.string.create_event
                        ),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp)
        ) {
            FormSection(
                title = stringResource(R.string.section_details),
                icon = R.drawable.list_alt_check
            ) {
                FormTextField(
                    value = state.title,
                    onValueChange = { onAction(EventFormAction.TitleChanged(it)) },
                    label = stringResource(R.string.event_title),
                    placeholder = stringResource(R.string.event_title_hint),
                    errorText = state.titleError
                )
                FormTextField(
                    value = state.hostName,
                    onValueChange = { onAction(EventFormAction.HostNameChanged(it)) },
                    label = stringResource(R.string.host_name),
                    placeholder = stringResource(R.string.host_name_hint),
                    supportingText = stringResource(R.string.host_name_support),
                    errorText = state.hostNameError
                )
                FormTextField(
                    value = state.description,
                    onValueChange = { onAction(EventFormAction.DescriptionChanged(it)) },
                    label = stringResource(R.string.event_description),
                    singleLine = false,
                    minLines = 3,
                    imeAction = ImeAction.Default
                )
            }

            FormSection(
                title = stringResource(R.string.section_schedule),
                icon = R.drawable.schedule
            ) {
                ScheduleEditor(
                    schedule = state.schedule,
                    onRecurrenceChange = { onAction(EventFormAction.RecurrenceChanged(it)) },
                    onToggleDay = { onAction(EventFormAction.DayToggled(it)) },
                    onRangeChange = { dayKey, range ->
                        onAction(EventFormAction.RangeChanged(dayKey, range))
                    },
                    errorText = state.scheduleError
                )
            }

            FormSection(
                title = stringResource(R.string.section_location),
                icon = R.drawable.location_on
            ) {
                FormTextField(
                    value = state.location,
                    onValueChange = { onAction(EventFormAction.LocationChanged(it)) },
                    label = stringResource(R.string.location),
                    placeholder = stringResource(R.string.location_hint),
                    errorText = state.locationError
                )
                FormTextField(
                    value = state.address,
                    onValueChange = { onAction(EventFormAction.AddressChanged(it)) },
                    label = stringResource(R.string.address),
                    placeholder = stringResource(R.string.address_hint),
                    singleLine = false,
                    minLines = 2,
                    imeAction = ImeAction.Default
                )
                MapLocationField(mapLocation = state.mapLocation)
                ProFeatureToggle(
                    title = stringResource(R.string.location_validation),
                    description = stringResource(R.string.location_validation_description),
                    checked = state.locationValidationEnabled,
                    onCheckedChange = {
                        onAction(EventFormAction.LocationValidationToggled(it))
                    },
                    isProUser = state.isProUser
                )
            }

            FormSection(
                title = stringResource(R.string.section_qr),
                icon = R.drawable.qr_code_scanner
            ) {
                Text(
                    text = stringResource(R.string.qr_hint),
                    color = Gray,
                    fontSize = 14.sp
                )
                QrCodePreview(
                    payload = state.qrPayload,
                    caption = state.title.ifBlank { null }
                )
                ScheduleSummary(schedule = state.schedule)
            }

            Button(
                onClick = { onAction(EventFormAction.Save) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 56.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(size = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Turquoise,
                    contentColor = White
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        color = White,
                        modifier = Modifier.height(height = 24.dp)
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (state.isEditing) R.string.save_changes else R.string.create_event
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSummary(schedule: EventSchedule, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        Text(text = schedule.daysSummary(), color = Black, fontSize = 14.sp)
        Text(text = schedule.timeSummary(), color = Gray, fontSize = 14.sp)
    }
}

/**
 * Says whether the attendants heard about it, but only when the schedule was
 * what changed; an ordinary save just says it saved.
 */
private fun Resources.savedMessage(attendeesNotified: Boolean?): String = getString(
    when (attendeesNotified) {
        null -> R.string.event_saved
        true -> R.string.schedule_change_notified
        false -> R.string.schedule_change_not_notified
    }
)

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

private fun DataError.message(): String = when (this) {
    DataError.NETWORK -> "No connection. Check your network and try again."
    DataError.UNAUTHORIZED -> "You are not allowed to save this event."
    DataError.NOT_FOUND -> "That event no longer exists."
    DataError.UNKNOWN -> "Something went wrong. Please try again."
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
fun EventFormContentPreview() {
    QuickAttendTheme {
        EventFormContent(
            state = EventFormState(
                eventId = "SBcH2mA1xQpLd0Rt",
                title = "Clase de Yoga",
                description = "Sesión de yoga para principiantes",
                hostName = "Carlos Jiménez",
                location = "Salon C201",
                address = "Av. Máximo Gómez 72, Santo Domingo",
                isProUser = true,
                locationValidationEnabled = true,
                schedule = EventSchedule(
                    recurrence = Recurrence.WEEKLY,
                    days = listOf(
                        ScheduleDay(1, TimeRange(13 * 60, 17 * 60)),
                        ScheduleDay(3, TimeRange(13 * 60, 17 * 60))
                    )
                )
            ),
            onAction = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
fun EventFormContentFreeUserPreview() {
    QuickAttendTheme {
        EventFormContent(
            state = EventFormState(
                eventId = "SBcH2mA1xQpLd0Rt",
                titleError = "A title is required",
                scheduleError = "Pick at least one day",
                schedule = EventSchedule(recurrence = Recurrence.MONTHLY)
            ),
            onAction = {},
            onBack = {}
        )
    }
}
