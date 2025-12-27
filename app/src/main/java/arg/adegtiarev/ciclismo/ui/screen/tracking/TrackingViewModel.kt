package arg.adegtiarev.ciclismo.ui.screen.tracking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.data.local.mapper.toTrackingPoint
import arg.adegtiarev.ciclismo.data.service.TrackingService
import arg.adegtiarev.ciclismo.domain.LocationClient
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.ui.state.RideState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val locationClient: LocationClient
) : ViewModel() {

    // Channel for one-time events (service commands)
    private val _serviceCommand = Channel<String>()
    val serviceCommand = _serviceCommand.receiveAsFlow()

    var showExitDialog by mutableStateOf(false)
        private set

    fun setDialogVisibility(visible: Boolean) {
        showExitDialog = visible
    }

    // Subscribe to service events
    val serviceEvents = TrackingService.events

    fun clearServiceEvent() {
        TrackingService.events.value = null
    }

    // Combine data from the service into a single state for the UI
    val rideState = combine(
        TrackingService.isTracking,
        TrackingService.currentSpeedKmh,
        TrackingService.totalDistanceMetres,
        TrackingService.durationInSeconds,
        TrackingService.pathPoints
    ) { tracking, speed, distance, duration, points ->
        RideState(
            isTracking = tracking,
            distanceMetres = distance,
            durationSeconds = duration,
            currentSpeedKmh = speed,
            isAutoPaused = false, // Logic can be added from the service if needed
            points = points.map { it.toTrackingPoint() } // Convert for the UI
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RideState())

    fun sendCommand(action: String) {
        viewModelScope.launch {
            _serviceCommand.send(action)
        }
    }

    init {
        // On start, try to get the last location to avoid showing Africa on the map
        viewModelScope.launch {
             locationClient.getLastLocation()?.let { location ->
                 // If there are no points yet, add an initial one just for map display
                 if (TrackingService.pathPoints.value.isEmpty()) {
                     TrackingService.pathPoints.value = listOf(location)
                 }
             }
        }
    }
}