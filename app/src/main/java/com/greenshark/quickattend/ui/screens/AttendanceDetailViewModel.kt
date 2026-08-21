/**
 * Created by Carlos Jiménez on 21/08/26.
 */
package com.greenshark.quickattend.ui.screens

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.greenshark.quickattend.data.AttendanceRecord
import com.greenshark.quickattend.data.AttendanceSummary
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.attendanceHistory
import com.greenshark.quickattend.data.repository.AttendanceRepository
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.summarize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the attendance detail screen draws. */
@Stable
data class AttendanceDetailState(
    val event: Event? = null,
    /** Every past session of the event, oldest first, absences included. */
    val records: List<AttendanceRecord> = emptyList(),
    val summary: AttendanceSummary = AttendanceSummary(),
    /** [AttendanceRecord.key] of the session shown in the card at the top. */
    val selectedKey: String? = null,
    val isLoading: Boolean = false,
    val error: DataError? = null
) {
    val selectedRecord: AttendanceRecord?
        get() = records.firstOrNull { it.key == selectedKey } ?: records.lastOrNull()

    /** Newest first, which is the order the history list reads in. */
    val recentFirst: List<AttendanceRecord> get() = records.asReversed()
}

sealed interface AttendanceDetailAction {
    data class RecordSelected(val key: String) : AttendanceDetailAction
    data object Refresh : AttendanceDetailAction
}

/**
 * Attendance for one event and the signed in user. The backend only stores the
 * check-ins, so the absences and the score are worked out here against the
 * event's own schedule.
 */
class AttendanceDetailViewModel(
    private val eventId: String,
    /** The session the user tapped; null falls back to the most recent one. */
    private val date: LocalDate? = null,
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val attendanceRepository: AttendanceRepository = ServiceLocator.attendanceRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceDetailState(isLoading = true))
    val state: StateFlow<AttendanceDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: AttendanceDetailAction) {
        when (action) {
            is AttendanceDetailAction.RecordSelected ->
                _state.update { it.copy(selectedKey = action.key) }

            AttendanceDetailAction.Refresh -> load()
        }
    }

    private fun load() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
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

            val checkIns = when (
                val result = attendanceRepository.getCheckIns(eventId, userId)
            ) {
                is DataResult.Success -> result.data
                is DataResult.Failure -> {
                    _state.update {
                        it.copy(isLoading = false, event = event, error = result.error)
                    }
                    return@launch
                }
            }

            val records = event.attendanceHistory(
                checkIns = checkIns,
                now = LocalDateTime.now()
            )
            _state.update {
                it.copy(
                    isLoading = false,
                    event = event,
                    records = records,
                    summary = records.summarize(),
                    selectedKey = records.firstOrNull { record -> record.date == date }?.key
                        ?: records.lastOrNull()?.key
                )
            }
        }
    }

    companion object {
        fun factory(eventId: String, date: LocalDate?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AttendanceDetailViewModel(eventId = eventId, date = date) }
            }
    }
}
