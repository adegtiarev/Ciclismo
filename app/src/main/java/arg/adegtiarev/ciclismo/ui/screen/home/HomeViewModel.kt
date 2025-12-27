package arg.adegtiarev.ciclismo.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.model.Ride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    val totalRides: Int = 0,
    val totalDistance: Double = 0.0,
    val totalTimeSeconds: Long = 0L,
    val rides: List<Ride> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: RideRepository
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repository.getTotalRidesCount(),
        repository.getTotalDistance(),
        repository.getTotalDuration(),
        repository.getAllRides()
    ) { count, distance, duration, rides ->
        HomeState(
            totalRides = count,
            totalDistance = distance,
            totalTimeSeconds = duration,
            rides = rides.sortedByDescending { it.timestamp } // Sort from newest to oldest
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
    )
}