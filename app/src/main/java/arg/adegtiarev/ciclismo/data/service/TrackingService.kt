package arg.adegtiarev.ciclismo.data.service

import android.content.Intent
import android.location.Location
import androidx.lifecycle.LifecycleService
import arg.adegtiarev.ciclismo.data.local.mapper.toTrackingPoint
import arg.adegtiarev.ciclismo.data.service.notification.NotificationHelper
import arg.adegtiarev.ciclismo.domain.LocationClient
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.calculator.RideStatsCalculator
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.util.TrackingConstants
import arg.adegtiarev.ciclismo.util.TrackingConstants.SHOW_STOP_DIALOG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var repository: RideRepository

    // Используем SupervisorJob, чтобы ошибка в одном процессе не убила весь скоуп
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Локальные переменные для накопления данных за текущую поездку
    private var totalDistanceMetres = 0.0
    private var maxSpeedKmh = 0f
    private val allPoints = mutableListOf<TrackingPoint>() // Список для сохранения в БД

    private var durationInSeconds = MutableStateFlow(0L)
    private var isAutoPaused = false

    private var timerJob: Job? = null
    private var locationJob: Job? = null

    private var startLocation: Location? = null
    private var hasLeftStartThreshold = false

    companion object {
        val isTracking = MutableStateFlow(false)

        // Список точек текущего сегмента пути
        val pathPoints = MutableStateFlow<List<Location>>(emptyList())

        // Добавим эти поля, чтобы ViewModel могла их подхватить
        val currentSpeedKmh = MutableStateFlow(0f)
        val totalDistanceMetres = MutableStateFlow(0.0)
        val durationInSeconds = MutableStateFlow(0L)

        val events = MutableStateFlow<String?>(null) // Для передачи событий в UI
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                TrackingConstants.ACTION_START_OR_RESUME_SERVICE -> {
                    startForegroundService()
                }

                TrackingConstants.ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }

                TrackingConstants.ACTION_STOP_SERVICE -> {
                    stopService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        // 1. Устанавливаем статус
        isTracking.value = true
        isAutoPaused = false // Сбрасываем автопаузу при ручном старте

        // 2. Foreground уведомление
        startForeground(
            TrackingConstants.NOTIFICATION_ID,
            notificationHelper.createNotification()
        )

        // 3. Запускаем сбор локации, если он еще не запущен
        if (locationJob == null || locationJob?.isActive == false) {
            startLocationUpdates()
        }

        // 4. Запускаем таймер (он сам отменит старый, если есть)
        startTimer()
    }

    private fun startLocationUpdates() {
        locationJob?.cancel() // На всякий случай
        locationJob = locationClient.getLocationUpdates(2000L)
            .onEach { location ->
                if (isTracking.value) {
                    // Твоя логика обработки координат...
                    val speed = location.speed * 3.6f
                    currentSpeedKmh.value = speed

                    isAutoPaused = RideStatsCalculator.shouldAutoPause(location.speed)

                    if (!isAutoPaused) {
                        val newPoint = location.toTrackingPoint()
                        addPointAndCalculate(newPoint)
                        pathPoints.value += location
                        TrackingService.totalDistanceMetres.value = this.totalDistanceMetres
                    } else {
                        currentSpeedKmh.value = 0f
                    }

                    if (startLocation == null) {
                        startLocation = location
                    } else {
                        val distanceToStart = location.distanceTo(startLocation!!)

                        // 1. Проверяем, отъехали ли мы достаточно далеко
                        if (distanceToStart > 100f) {
                            hasLeftStartThreshold = true
                        }

                        // 2. Если уже отъезжали и вернулись ближе 20 метров — стоп
                        if (hasLeftStartThreshold && distanceToStart < 20f) {
                            stopService()
                            events.value = SHOW_STOP_DIALOG
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            // Убеждаемся, что цикл видит актуальное значение
            while (isTracking.value) {
                delay(1000L)
                if (!isAutoPaused && isTracking.value) {
                    // Прямое обновление статического поля
                    TrackingService.durationInSeconds.value += 1
                    // И локального для сохранения в БД
                    durationInSeconds.value = TrackingService.durationInSeconds.value
                }
            }
        }
    }

    private fun pauseService() {
        isTracking.value = false
        // Таймер сам остановится, так как while (isTracking.value) станет false
    }

    private fun addPointAndCalculate(newPoint: TrackingPoint) {
        // 1. Считаем дистанцию, если это не первая точка
        if (allPoints.isNotEmpty()) {
            val lastPoint = allPoints.last()
            val distanceBetween = RideStatsCalculator.calculateDistance(listOf(lastPoint, newPoint))
            totalDistanceMetres += distanceBetween
        }

        // 2. Добавляем в общий список для БД
        allPoints.add(newPoint)

        // 3. Обновляем максимальную скорость
        val currentSpeedKmh = newPoint.speed * 3.6f
        if (currentSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = currentSpeedKmh
        }
    }

    // В конце поездки вызываем это:
    private fun saveRideToDb() {
        val finalDistance = totalDistanceMetres
        val finalDuration = durationInSeconds.value
        val pointsToSave = allPoints.toList() // Копируем список

        if (finalDistance > 10.0 && pointsToSave.isNotEmpty()) { // Минимум 10 метров для сохранения
            serviceScope.launch {
                val avgSpeed = if (finalDuration > 0) (finalDistance / finalDuration) * 3.6 else 0.0

                val finalRide = Ride(
                    id = 0, // Room сгенерирует сам
                    distance = finalDistance,
                    duration = finalDuration,
                    timestamp = System.currentTimeMillis(),
                    averageSpeed = avgSpeed,
                    maxSpeed = maxSpeedKmh.toDouble(),
                    routePoints = pointsToSave
                )
                repository.saveFullRide(finalRide, pointsToSave)

                // После сохранения очищаем локальные данные
                resetTrackingData()
            }
        }
    }

    private fun resetTrackingData() {
        allPoints.clear()
        pathPoints.value = emptyList()
        totalDistanceMetres = 0.0
        maxSpeedKmh = 0f
        durationInSeconds.value = 0L
    }


    private fun stopService() {
        saveRideToDb() // Сначала сохраняем
        isTracking.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

}