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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Локальные переменные для накопления данных за текущую поездку
    private var totalDistanceMetres = 0.0
    private var maxSpeedKmh = 0f
    private val allPoints = mutableListOf<TrackingPoint>() // Список для сохранения в БД

    private var durationInSeconds = MutableStateFlow(0L)
    private var isAutoPaused = false

    companion object {
        val isTracking = MutableStateFlow(false)

        // Список точек текущего сегмента пути
        val pathPoints = MutableStateFlow<List<Location>>(emptyList())

        // Добавим эти поля, чтобы ViewModel могла их подхватить
        val currentSpeedKmh = MutableStateFlow(0f)
        val totalDistanceMetres = MutableStateFlow(0.0)
        val durationInSeconds = MutableStateFlow(0L)
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
        isTracking.value = true

        // Запускаем сервис в режиме Foreground
        // Для Android 14+ тип location обязателен (мы прописали его в манифесте)
        startForeground(
            TrackingConstants.NOTIFICATION_ID, notificationHelper.createNotification()
        )

        // Здесь мы будем создавать уведомление (Notification)
        // И запускать сбор координат
        startLocationUpdates()
        startTimer()
    }

    private fun startLocationUpdates() {
        locationClient.getLocationUpdates(2000L)
            .onEach { location ->
                if (isTracking.value) {
                    pathPoints.value += location

                    // Используем скорость для UI
                    val speed = location.speed * 3.6f
                    currentSpeedKmh.value = speed

                    isAutoPaused = RideStatsCalculator.shouldAutoPause(location.speed)

                    if (!isAutoPaused) {
                        val newPoint = location.toTrackingPoint()
                        addPointAndCalculate(newPoint)

                        // Обновляем дистанцию для UI
                        TrackingService.totalDistanceMetres.value = this.totalDistanceMetres
                    } else {
                        currentSpeedKmh.value = 0f // Если на паузе - скорость 0
                    }
                }
            }
            .launchIn(serviceScope)
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

    private fun pauseService() {
        isTracking.value = false
    }

    private fun stopService() {
        saveRideToDb() // Сначала сохраняем
        isTracking.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        serviceScope.launch {
            while (isTracking.value) {
                if (!isAutoPaused) {
                    delay(1000L)
                    durationInSeconds.value += 1
                    TrackingService.durationInSeconds.value = durationInSeconds.value;
                }
            }
        }
    }
}