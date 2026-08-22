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
import com.greenshark.quickattend.data.JoinRequest
import com.greenshark.quickattend.data.JoinRequestStatus
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.attendeeAttendance
import com.greenshark.quickattend.data.filteredBy
import com.greenshark.quickattend.data.overview
import com.greenshark.quickattend.data.pending
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.JoinRequestRepository
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
    /** People who scanned the QR without being on the roll, newest first. */
    val requests: List<JoinRequest> = emptyList(),
    /** Requests being answered right now, so their row stops taking taps. */
    val answeringRequestIds: Set<String> = emptySet(),
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

    val pendingRequests: List<JoinRequest> get() = requests.pending()

    val hasPendingRequests: Boolean get() = pendingRequests.isNotEmpty()
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
    data class RequestAccepted(val request: JoinRequest) : HostEventDetailAction
    data class RequestIgnored(val request: JoinRequest) : HostEventDetailAction
    data object Refresh : HostEventDetailAction
}

sealed interface HostEventDetailEvent {
    data class AttendeeRemoved(val name: String) : HostEventDetailEvent
    data object EventDeleted : HostEventDetailEvent
    data class RequestAccepted(val name: String) : HostEventDetailEvent
    data class RequestIgnored(val name: String) : HostEventDetailEvent
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
    private val joinRequestRepository: JoinRequestRepository = ServiceLocator.joinRequestRepository,
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

            is HostEventDetailAction.RequestAccepted -> answer(
                request = action.request,
                status = JoinRequestStatus.APPROVED
            )

            is HostEventDetailAction.RequestIgnored -> answer(
                request = action.request,
                status = JoinRequestStatus.REJECTED
            )

            HostEventDetailAction.Refresh -> load()
        }
    }

    private fun load() {
        if (auth.currentUser?.uid.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        // Requests are fetched on their own, because the roll has several early
        // exits — an event with nobody signed up still has people asking to be.
        loadRequests()

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

    private fun loadRequests() {
        viewModelScope.launch {
            when (val result = joinRequestRepository.getRequestsForEvent(eventId)) {
                is DataResult.Success -> _state.update { it.copy(requests = result.data) }
                // The roll is the point of this screen; failing to read the
                // requests should not paint an error over all of it.
                is DataResult.Failure -> Unit
            }
        }
    }

    /**
     * Answers one request. Approving writes the sign-up as well as the status,
     * because it is the sign-up that puts the person on the roll and their
     * already recorded check-ins into every export.
     */
    private fun answer(request: JoinRequest, status: JoinRequestStatus) {
        if (request.id in _state.value.answeringRequestIds) return
        _state.update { it.copy(answeringRequestIds = it.answeringRequestIds + request.id) }

        viewModelScope.launch {
            val result = joinRequestRepository.setStatus(
                requestId = request.id,
                status = status
            )
            if (result is DataResult.Failure) {
                clearAnswering(request)
                _events.send(HostEventDetailEvent.Error(result.error))
                return@launch
            }

            if (status == JoinRequestStatus.APPROVED) {
                val joined = eventRepository.joinEvent(
                    eventId = eventId,
                    attendeeId = request.userId
                )
                if (joined is DataResult.Failure) {
                    // The status moved but the sign-up did not, so the request
                    // is put back to pending rather than vanishing unanswered.
                    joinRequestRepository.setStatus(request.id, JoinRequestStatus.PENDING)
                    clearAnswering(request)
                    _events.send(HostEventDetailEvent.Error(joined.error))
                    return@launch
                }
            }

            _state.update { state ->
                state.copy(
                    requests = state.requests.map { existing ->
                        if (existing.id == request.id) existing.copy(status = status) else existing
                    },
                    answeringRequestIds = state.answeringRequestIds - request.id
                )
            }

            _events.send(
                if (status == JoinRequestStatus.APPROVED) {
                    HostEventDetailEvent.RequestAccepted(request.displayName)
                } else {
                    HostEventDetailEvent.RequestIgnored(request.displayName)
                }
            )

            // A new attendant brings their whole history onto the roll with
            // them, which only a reload can work out.
            if (status == JoinRequestStatus.APPROVED) load()
        }
    }

    private fun clearAnswering(request: JoinRequest) {
        _state.update { it.copy(answeringRequestIds = it.answeringRequestIds - request.id) }
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
