/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventSchedule
import com.greenshark.quickattend.data.MapLocation
import com.greenshark.quickattend.data.Recurrence
import com.greenshark.quickattend.data.ScheduleDay
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.TimeRange
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.qr.QrCodeGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the form screen draws. */
data class EventFormState(
    val eventId: String = "",
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val hostName: String = "",
    val location: String = "",
    val address: String = "",
    val mapLocation: MapLocation? = null,
    val locationValidationEnabled: Boolean = false,
    val schedule: EventSchedule = EventSchedule(),
    val organizerId: String = "",
    val organizerName: String = "",
    val isProUser: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val hostNameError: String? = null,
    val locationError: String? = null,
    val scheduleError: String? = null
) {
    val qrPayload: String get() = QrCodeGenerator.payloadFor(eventId)
}

sealed interface EventFormAction {
    data class TitleChanged(val value: String) : EventFormAction
    data class DescriptionChanged(val value: String) : EventFormAction
    data class HostNameChanged(val value: String) : EventFormAction
    data class LocationChanged(val value: String) : EventFormAction
    data class AddressChanged(val value: String) : EventFormAction
    data class RecurrenceChanged(val value: Recurrence) : EventFormAction
    data class DayToggled(val dayKey: Int) : EventFormAction
    data class RangeChanged(val dayKey: Int, val range: TimeRange) : EventFormAction
    data class LocationValidationToggled(val enabled: Boolean) : EventFormAction
    data object Save : EventFormAction
}

sealed interface EventFormEvent {
    data class Saved(val event: Event) : EventFormEvent
    data class Error(val error: DataError) : EventFormEvent
}

class EventFormViewModel(
    private val eventId: String?,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(EventFormState())
    val state: StateFlow<EventFormState> = _state.asStateFlow()

    private val _events = Channel<EventFormEvent>(Channel.BUFFERED)
    val events: Flow<EventFormEvent> = _events.receiveAsFlow()

    init {
        val user = auth.currentUser
        _state.update {
            it.copy(
                // A brand new event reserves its id now so the QR can be previewed
                // before anything is written.
                eventId = eventId ?: eventRepository.newEventId(),
                isEditing = eventId != null,
                organizerId = user?.uid.orEmpty(),
                organizerName = user?.displayName ?: user?.email.orEmpty(),
                hostName = user?.displayName.orEmpty(),
                schedule = defaultSchedule(Recurrence.WEEKLY),
                isLoading = eventId != null
            )
        }
        loadProStatus()
        if (eventId != null) loadEvent(eventId)
    }

    fun onAction(action: EventFormAction) {
        when (action) {
            is EventFormAction.TitleChanged ->
                _state.update { it.copy(title = action.value, titleError = null) }

            is EventFormAction.DescriptionChanged ->
                _state.update { it.copy(description = action.value) }

            is EventFormAction.HostNameChanged ->
                _state.update { it.copy(hostName = action.value, hostNameError = null) }

            is EventFormAction.LocationChanged ->
                _state.update { it.copy(location = action.value, locationError = null) }

            is EventFormAction.AddressChanged ->
                _state.update { it.copy(address = action.value) }

            is EventFormAction.RecurrenceChanged -> _state.update {
                it.copy(schedule = defaultSchedule(action.value), scheduleError = null)
            }

            is EventFormAction.DayToggled -> _state.update {
                it.copy(schedule = it.schedule.toggleDay(action.dayKey), scheduleError = null)
            }

            is EventFormAction.RangeChanged -> _state.update {
                it.copy(
                    schedule = it.schedule.withRange(action.dayKey, action.range),
                    scheduleError = null
                )
            }

            is EventFormAction.LocationValidationToggled -> _state.update {
                // Guarded here as well as in the UI so a stale screen cannot enable it.
                it.copy(locationValidationEnabled = action.enabled && it.isProUser)
            }

            EventFormAction.Save -> save()
        }
    }

    private fun loadProStatus() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = userRepository.isProUser(userId)
            if (result is DataResult.Success) {
                _state.update { it.copy(isProUser = result.data) }
            }
        }
    }

    private fun loadEvent(id: String) {
        viewModelScope.launch {
            when (val result = eventRepository.getEvent(id)) {
                is DataResult.Success -> _state.update { it.fromEvent(result.data) }
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(EventFormEvent.Error(result.error))
                }
            }
        }
    }

    private fun save() {
        val validated = _state.value.validated()
        _state.value = validated
        if (validated.hasErrors) return

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val event = validated.toEvent()
            val result = if (validated.isEditing) {
                eventRepository.updateEvent(event)
            } else {
                eventRepository.createEvent(event)
            }
            _state.update { it.copy(isSaving = false) }
            when (result) {
                is DataResult.Success -> _events.send(EventFormEvent.Saved(result.data))
                is DataResult.Failure -> _events.send(EventFormEvent.Error(result.error))
            }
        }
    }

    companion object {
        fun factory(eventId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { EventFormViewModel(eventId = eventId) }
        }
    }
}

private val EventFormState.hasErrors: Boolean
    get() = titleError != null || hostNameError != null ||
            locationError != null || scheduleError != null

private fun EventFormState.validated(): EventFormState = copy(
    titleError = "A title is required".takeIf { title.isBlank() },
    hostNameError = "A host name is required".takeIf { hostName.isBlank() },
    locationError = "A location is required".takeIf { location.isBlank() },
    scheduleError = when {
        schedule.days.isEmpty() -> "Pick at least one day"
        schedule.days.any { !it.range.isValid } -> "Every end time must be after its start time"
        else -> null
    }
)

private fun EventFormState.toEvent(): Event = Event(
    id = eventId,
    title = title.trim(),
    description = description.trim(),
    hostName = hostName.trim(),
    organizerId = organizerId,
    organizerName = organizerName,
    location = location.trim(),
    address = address.trim(),
    mapLocation = mapLocation,
    locationValidationEnabled = locationValidationEnabled && isProUser,
    schedule = schedule
)

private fun EventFormState.fromEvent(event: Event): EventFormState = copy(
    eventId = event.id,
    title = event.title,
    description = event.description,
    hostName = event.hostName,
    location = event.location,
    address = event.address,
    mapLocation = event.mapLocation,
    locationValidationEnabled = event.locationValidationEnabled,
    schedule = event.schedule,
    organizerId = event.organizerId,
    organizerName = event.organizerName,
    isLoading = false
)

/** Daily has one implicit day; the others start empty so the user picks them. */
private fun defaultSchedule(recurrence: Recurrence): EventSchedule = EventSchedule(
    recurrence = recurrence,
    days = if (recurrence == Recurrence.DAILY) {
        listOf(ScheduleDay(dayKey = ScheduleDay.DAILY_KEY, range = TimeRange()))
    } else {
        emptyList()
    }
)

private fun EventSchedule.toggleDay(dayKey: Int): EventSchedule =
    if (isSelected(dayKey)) {
        copy(days = days.filterNot { it.dayKey == dayKey })
    } else {
        // Seed a new day with the hours already in use so ranges rarely need retyping.
        val seed = days.lastOrNull()?.range ?: TimeRange()
        copy(days = days + ScheduleDay(dayKey = dayKey, range = seed))
    }

private fun EventSchedule.withRange(dayKey: Int, range: TimeRange): EventSchedule =
    copy(days = days.map { if (it.dayKey == dayKey) it.copy(range = range) else it })
