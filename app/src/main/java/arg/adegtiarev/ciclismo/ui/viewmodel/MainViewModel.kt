package arg.adegtiarev.ciclismo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.data.local.mapper.toTrackingPoint
import arg.adegtiarev.ciclismo.data.service.TrackingService
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
    private val rideRepository: RideRepository
) : ViewModel() {

    // Канал для разовых событий (команд запуска сервиса)
    private val _serviceCommand = Channel<String>()
    val serviceCommand = _serviceCommand.receiveAsFlow()

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
}