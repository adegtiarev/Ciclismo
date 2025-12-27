package arg.adegtiarev.ciclismo.ui.viewmodel

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
class MainViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val locationClient: LocationClient
) : ViewModel() {

    // Канал для разовых событий (команд запуска сервиса)
    private val _serviceCommand = Channel<String>()
    val serviceCommand = _serviceCommand.receiveAsFlow()

    var showExitDialog by mutableStateOf(false)
        private set

    fun setDialogVisibility(visible: Boolean) {
        showExitDialog = visible
    }

    // Подпишемся на события сервиса (можно добавить в init или отдельный Flow)
    val serviceEvents = TrackingService.events

    fun clearServiceEvent() {
        TrackingService.events.value = null
    }

    // Объединяем данные из сервиса в один State для UI
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
            isAutoPaused = false, // Можно добавить логику из сервиса если нужно
            points = points.map { it.toTrackingPoint() } // Конвертируем для UI
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RideState())

    // Метод для запуска/остановки сервиса
    fun onAction(action: String) {
        // Здесь мы будем отправлять Intent в TrackingService
        sendCommand(action)
    }

    fun sendCommand(action: String) {
        viewModelScope.launch {
            _serviceCommand.send(action)
        }
    }
    
    init {
        // При старте пробуем получить последнюю локацию, чтобы карта не показывала Африку
        viewModelScope.launch {
             locationClient.getLastLocation()?.let { location ->
                 // Если точек еще нет, добавляем начальную только для отображения на карте
                 if (TrackingService.pathPoints.value.isEmpty()) {
                     TrackingService.pathPoints.value = listOf(location)
                 }
             }
        }
    }
}