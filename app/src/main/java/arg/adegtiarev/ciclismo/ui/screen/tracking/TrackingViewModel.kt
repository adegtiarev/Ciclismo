package arg.adegtiarev.ciclismo.ui.screen.tracking

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.data.local.mapper.toTrackingPoint
import arg.adegtiarev.ciclismo.data.service.TrackingService
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
class TrackingViewModel @Inject constructor() : ViewModel() {

    private val _serviceCommand = Channel<String>()
    val serviceCommand = _serviceCommand.receiveAsFlow()

    var showExitDialog by mutableStateOf(false)
        private set

    fun setDialogVisibility(visible: Boolean) {
        showExitDialog = visible
    }

    val serviceEvents = TrackingService.events

    fun clearServiceEvent() {
        TrackingService.clearEvent()
    }

    val rideState = combine(
        TrackingService.isTracking,
        TrackingService.currentSpeedKmh,
        TrackingService.totalDistance,
        TrackingService.durationInSeconds,
        TrackingService.pathPoints
    ) { isTracking, speed, distance, duration, points ->
        RideState(
            isTracking = isTracking,
            distanceMetres = distance,
            durationSeconds = duration,
            currentSpeedKmh = speed,
            isAutoPaused = false,
            points = points.map { it.toTrackingPoint() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RideState())

    fun sendCommand(action: String) {
        viewModelScope.launch {
            _serviceCommand.send(action)
        }
    }
}
