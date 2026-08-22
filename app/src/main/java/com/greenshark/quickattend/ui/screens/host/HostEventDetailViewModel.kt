/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens.host

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.AttendeeSort
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventAttendanceOverview
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.attendeeAttendance
import com.greenshark.quickattend.data.filteredBy
import com.greenshark.quickattend.data.overview
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.data.sortedBy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Everything the host's event detail screen draws. */
@Stable
data class HostEventDetailState(
    val event: Event? = null,
    /** Every attendant with their whole history, before search and sorting. */
    val attendees: List<AttendeeAttendance> = emptyList(),
    val overview: EventAttendanceOverview = EventAttendanceOverview(),
    val query: String = "",
    val sort: AttendeeSort = AttendeeSort.NAME,
    /** Attendant the host is about to remove, held while the dialog is up. */
    val pendingRemoval: AttendeeAttendance? = null,
    val isConfirmingDelete: Boolean = false,
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val error: DataError? = null
) {
    /** What the list actually shows, once the search box and chips are applied. */
    val visibleAttendees: List<AttendeeAttendance>
        get() = attendees.filteredBy(query).sortedBy(sort)

    val hasAttendees: Boolean get() = attendees.isNotEmpty()

    /** True when a search hid everything there is, rather than there being none. */
    val isFilteredEmpty: Boolean get() = hasAttendees && visibleAttendees.isEmpty()

    val canExport: Boolean get() = event != null && !isLoading
}

sealed interface HostEventDetailAction {
    data class QueryChanged(val value: String) : HostEventDetailAction
    data class SortChanged(val value: AttendeeSort) : HostEventDetailAction
    data class RemoveRequested(val attendee: AttendeeAttendance) : HostEventDetailAction
    data object RemoveConfirmed : HostEventDetailAction
    data object RemoveDismissed : HostEventDetailAction
    data object DeleteRequested : HostEventDetailAction
    data object DeleteConfirmed : HostEventDetailAction
    data object DeleteDismissed : HostEventDetailAction
    data object Refresh : HostEventDetailAction
}

sealed interface HostEventDetailEvent {
    data class AttendeeRemoved(val name: String) : HostEventDetailEvent
    data object EventDeleted : HostEventDetailEvent
    data class Error(val error: DataError) : HostEventDetailEvent
}

/**
 * One event as its host sees it.
 *
 * The roll is assembled here rather than stored: the backend only holds sign-ups
 * and check-ins, so who was absent on which session is worked out against the
 * event's own schedule, the same way the attendant's own screen does it.
 */
class HostEventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(HostEventDetailState(isLoading = true))
    val state: StateFlow<HostEventDetailState> = _state.asStateFlow()

    private val _events = Channel<HostEventDetailEvent>(Channel.BUFFERED)
    val events: Flow<HostEventDetailEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: HostEventDetailAction) {
        when (action) {
            is HostEventDetailAction.QueryChanged ->
                _state.update { it.copy(query = action.value) }

            is HostEventDetailAction.SortChanged ->
                _state.update { it.copy(sort = action.value) }

            is HostEventDetailAction.RemoveRequested ->
                _state.update { it.copy(pendingRemoval = action.attendee) }

            HostEventDetailAction.RemoveDismissed ->
                _state.update { it.copy(pendingRemoval = null) }

            HostEventDetailAction.RemoveConfirmed -> removePendingAttendee()

            HostEventDetailAction.DeleteRequested ->
                _state.update { it.copy(isConfirmingDelete = true) }

            HostEventDetailAction.DeleteDismissed ->
                _state.update { it.copy(isConfirmingDelete = false) }

            HostEventDetailAction.DeleteConfirmed -> deleteEvent()

            HostEventDetailAction.Refresh -> load()
        }
    }

    private fun load() {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val event = when (val result = eventRepository.getEvent(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }
            _state.update { it.copy(event = event) }

            val attendeeIds = when (val result = eventRepository.getAttendeeIds(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            if (attendeeIds.isEmpty()) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        attendees = emptyList(),
                        overview = EventAttendanceOverview()
                    )
                }
                return@launch
            }

            val profiles = when (val result = userRepository.getProfiles(attendeeIds)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            val checkIns = when (val result = attendanceRepository.getEventCheckIns(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            val attendees = event.attendeeAttendance(
                profiles = profiles,
                checkIns = checkIns,
                now = LocalDateTime.now()
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    attendees = attendees,
                    overview = attendees.overview()
                )
            }
        }
    }

    private fun removePendingAttendee() {
        val attendee = _state.value.pendingRemoval ?: return
        _state.update { it.copy(pendingRemoval = null, isWorking = true) }

        viewModelScope.launch {
            val result = eventRepository.removeAttendee(
                eventId = eventId,
                attendeeId = attendee.profile.id
            )
            when (result) {
                is DataResult.Success -> {
                    // Dropped locally as well as remotely, so the list settles
                    // straight away instead of after a round trip.
                    val remaining = _state.value.attendees
                        .filterNot { it.profile.id == attendee.profile.id }
                    _state.update {
                        it.copy(
                            isWorking = false,
                            attendees = remaining,
                            overview = remaining.overview()
                        )
                    }
                    _events.send(
                        HostEventDetailEvent.AttendeeRemoved(attendee.profile.displayName)
                    )
                }

                is DataResult.Failure -> {
                    _state.update { it.copy(isWorking = false) }
                    _events.send(HostEventDetailEvent.Error(result.error))
                }
            }
        }
    }

    private fun deleteEvent() {
        _state.update { it.copy(isConfirmingDelete = false, isWorking = true) }
        viewModelScope.launch {
            when (val result = eventRepository.deleteEvent(eventId)) {
                is DataResult.Success -> {
                    _state.update { it.copy(isWorking = false) }
                    _events.send(HostEventDetailEvent.EventDeleted)
                }

                is DataResult.Failure -> {
                    _state.update { it.copy(isWorking = false) }
                    _events.send(HostEventDetailEvent.Error(result.error))
                }
            }
        }
    }

    companion object {
        fun factory(eventId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { HostEventDetailViewModel(eventId = eventId) }
        }
    }
}
