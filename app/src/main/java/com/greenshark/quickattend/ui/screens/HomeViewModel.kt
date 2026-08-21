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
import com.greenshark.quickattend.data.DataError
import com.greenshark.quickattend.data.DataResult
import com.greenshark.quickattend.data.Event
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.mainOccurrenceOn
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.upcomingOccurrences
import com.greenshark.quickattend.ui.component.UPCOMING_EVENTS_PREVIEW_COUNT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the home screen draws. */
@Stable
data class HomeState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val monthDays: List<LocalDate> = emptyList(),
    /** Running or next event on [selectedDate]; null means the day is free. */
    val mainOccurrence: EventOccurrence? = null,
    val upcoming: List<EventOccurrence> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null
)

sealed interface HomeAction {
    data class DateSelected(val date: LocalDate) : HomeAction
    data object Refresh : HomeAction
}

class HomeViewModel(
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** Kept out of the state because the UI only ever reads the derived lists. */
    private var events: List<Event> = emptyList()

    init {
        val today = LocalDate.now()
        _state.update {
            it.copy(
                today = today,
                selectedDate = today,
                monthDays = today.monthDays(),
                isLoading = true
            )
        }
        loadEvents()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.DateSelected -> _state.update {
                it.copy(selectedDate = action.date).withOccurrences(events)
            }

            HomeAction.Refresh -> loadEvents()
        }
    }

    private fun loadEvents() {
        val attendeeId = auth.currentUser?.uid
        if (attendeeId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = eventRepository.getEventsByAttendee(attendeeId)) {
                is DataResult.Success -> {
                    events = result.data
                    _state.update {
                        it.copy(isLoading = false).withOccurrences(events)
                    }
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel() }
        }
    }
}

/** Recomputes both event slots against the clock so their status stays honest. */
private fun HomeState.withOccurrences(events: List<Event>): HomeState {
    val now = LocalDateTime.now()
    return copy(
        mainOccurrence = events.mainOccurrenceOn(date = selectedDate, now = now),
        upcoming = events.upcomingOccurrences(now = now).take(UPCOMING_EVENTS_PREVIEW_COUNT)
    )
}

private fun LocalDate.monthDays(): List<LocalDate> = (1..lengthOfMonth()).map(::withDayOfMonth)
