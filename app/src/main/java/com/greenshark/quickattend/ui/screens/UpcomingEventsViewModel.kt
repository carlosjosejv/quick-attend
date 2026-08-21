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
import com.greenshark.quickattend.data.EventOccurrence
import com.greenshark.quickattend.data.ServiceLocator
import com.greenshark.quickattend.data.repository.EventRepository
import com.greenshark.quickattend.data.upcomingOccurrences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Everything the upcoming events screen draws. */
@Stable
data class UpcomingEventsState(
    val occurrences: List<EventOccurrence> = emptyList(),
    val isLoading: Boolean = false,
    val error: DataError? = null
)

/** The full list behind the home carousel's "view all". */
class UpcomingEventsViewModel(
    private val eventRepository: EventRepository = ServiceLocator.eventRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(UpcomingEventsState())
    val state: StateFlow<UpcomingEventsState> = _state.asStateFlow()

    init {
        loadEvents()
    }

    fun refresh() = loadEvents()

    private fun loadEvents() {
        val attendeeId = auth.currentUser?.uid
        if (attendeeId.isNullOrBlank()) {
            _state.update { it.copy(isLoading = false, error = DataError.UNAUTHORIZED) }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = eventRepository.getEventsByAttendee(attendeeId)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        occurrences = result.data.upcomingOccurrences(now = LocalDateTime.now())
                    )
                }

                is DataResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { UpcomingEventsViewModel() }
        }
    }
}
