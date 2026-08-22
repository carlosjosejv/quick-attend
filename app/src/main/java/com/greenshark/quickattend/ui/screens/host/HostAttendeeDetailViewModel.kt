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
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceSignature
import com.greenshark.quickattend.data.AttendeeAttendance
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.UserProfile
import com.greenshark.quickattend.data.attendanceHistory
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.repository.UserRepository
import com.greenshark.quickattend.data.signatureFor
import com.greenshark.quickattend.data.summarize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Everything the host's per-attendant screen draws. */
@Stable
data class HostAttendeeDetailState(
    val event: Event? = null,
    val attendee: AttendeeAttendance? = null,
    /** [AttendanceRecord.key] of the session featured at the top. */
    val selectedKey: String? = null,
    val isLoading: Boolean = false,
    val error: DataError? = null
) {
    val profile: UserProfile? get() = attendee?.profile

    val records: List<AttendanceRecord> get() = attendee?.recentFirst.orEmpty()

    /**
     * Defaults to the most recent session the attendant actually turned up for,
     * because that is the one carrying a signature worth looking at.
     */
    val selectedRecord: AttendanceRecord?
        get() = attendee?.let { found ->
            found.records.firstOrNull { it.key == selectedKey }
                ?: found.lastAttended
                ?: found.records.lastOrNull()
        }

    /** What the featured session is signed with: an image or a generated code. */
    val selectedSignature: AttendanceSignature
        get() {
            val record = selectedRecord ?: return AttendanceSignature.None
            val signer = profile ?: return AttendanceSignature.None
            return signatureFor(record = record, profile = signer)
        }

    val canExport: Boolean get() = event != null && attendee != null
}

sealed interface HostAttendeeDetailAction {
    data class RecordSelected(val key: String) : HostAttendeeDetailAction
    data object Refresh : HostAttendeeDetailAction
}

/**
 * One attendant's whole record on one event, as the host sees it. Reads the
 * same aggregates the roll does, so a number here never disagrees with the row
 * that led to this screen.
 */
class HostAttendeeDetailViewModel(
    private val eventId: String,
    private val userId: String,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HostAttendeeDetailState(isLoading = true))
    val state: StateFlow<HostAttendeeDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: HostAttendeeDetailAction) {
        when (action) {
            is HostAttendeeDetailAction.RecordSelected ->
                _state.update { it.copy(selectedKey = action.key) }

            HostAttendeeDetailAction.Refresh -> load()
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val event = when (val result = eventRepository.getEvent(eventId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error) }
                    return@launch
                }
            }

            val profile = when (val result = userRepository.getProfile(userId)) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, event = event, error = result.error)
                    }
                    return@launch
                }
            }

            val checkIns = when (
                val result = attendanceRepository.getCheckIns(eventId = eventId, userId = userId)
            ) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, event = event, error = result.error)
                    }
                    return@launch
                }
            }

            val records = event.attendanceHistory(checkIns = checkIns, now = LocalDateTime.now())
            _state.update {
                it.copy(
                    isLoading = false,
                    event = event,
                    attendee = AttendeeAttendance(
                        profile = profile,
                        records = records,
                        summary = records.summarize()
                    )
                )
            }
        }
    }

    companion object {
        fun factory(eventId: String, userId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HostAttendeeDetailViewModel(eventId = eventId, userId = userId) }
            }
    }
}
