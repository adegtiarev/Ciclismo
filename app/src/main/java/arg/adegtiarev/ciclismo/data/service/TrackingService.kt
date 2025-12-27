package arg.adegtiarev.ciclismo.data.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import arg.adegtiarev.ciclismo.data.local.mapper.toTrackingPoint
import arg.adegtiarev.ciclismo.data.service.notification.NotificationHelper
import arg.adegtiarev.ciclismo.domain.LocationClient
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.calculator.RideStatsCalculator
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.util.TrackingConstants
import arg.adegtiarev.ciclismo.util.TrackingConstants.HIDE_STOP_DIALOG
import arg.adegtiarev.ciclismo.util.TrackingConstants.SHOW_STOP_DIALOG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private var timerJob: Job? = null
    private var locationJob: Job? = null

    private var startLocation: Location? = null
    private var hasLeftStartThreshold = false

    // Флаг, указывающий, что сессия поездки активна (пользователь нажал Start и еще не нажал Stop)
    private var isServiceActive = false

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
                    // При явном старте активируем сессию
                    isServiceActive = true
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

        // 2. Foreground уведомление
        val notification = notificationHelper.createNotification()

        ServiceCompat.startForeground(
            this,
            TrackingConstants.NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
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
                // Обновляем скорость всегда, даже на паузе, чтобы видеть "0"
                val speed = location.speed * 3.6f
                currentSpeedKmh.value = speed

                // Определяем, должны ли мы стоять (скорость < порога)
                val shouldBePaused = RideStatsCalculator.shouldAutoPause(location.speed)

                if (isTracking.value) {
                    if (shouldBePaused) {
                        // Ехали, но остановились -> Ставим на паузу (UI покажет кнопки RESUME/STOP)
                        pauseService()
                    } else {
                        // Едем нормально -> Записываем данные
                        val newPoint = location.toTrackingPoint()
                        addPointAndCalculate(newPoint)
                        pathPoints.value += location
                        TrackingService.totalDistanceMetres.value = this.totalDistanceMetres
                    }
                } else {
                    // Мы на паузе (автоматической или ручной)
                    // АВТО-СТАРТ срабатывает ТОЛЬКО если сессия активна (isServiceActive == true)
                    if (!shouldBePaused && isServiceActive) {
                        // Начали движение -> Авто-старт
                        isTracking.value = true
                        startTimer()
                    }
                }

                // Логика диалога окончания поездки (работает независимо от паузы)
                if (isServiceActive) {
                    checkStopDialogConditions(location)
                }
            }
            .launchIn(serviceScope)
    }

    private fun checkStopDialogConditions(location: Location) {
        if (startLocation == null) {
            startLocation = location
        } else {
            val distanceToStart = location.distanceTo(startLocation!!)

            // 1. Проверяем, отъехали ли мы достаточно далеко
            if (distanceToStart > 100f) {
                hasLeftStartThreshold = true
            }

            // 2. Логика авто-стопа
            // Если отъехали (>100м) и вернулись (<20м) -> показать диалог
            if (hasLeftStartThreshold && distanceToStart < 20f) {
                events.value = SHOW_STOP_DIALOG
            }

            // Если снова отъехали от старта (> 50м), скрываем диалог
            if (hasLeftStartThreshold && distanceToStart > 50f) {
                events.value = HIDE_STOP_DIALOG
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            // Убеждаемся, что цикл видит актуальное значение
            while (isTracking.value) {
                delay(1000L)
                if (isTracking.value) {
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
            }
        }
    }

    private fun resetTrackingData() {
        // Сбрасываем статические поля, чтобы UI обновился
        isTracking.value = false
        pathPoints.value = emptyList()
        TrackingService.totalDistanceMetres.value = 0.0
        currentSpeedKmh.value = 0f
        TrackingService.durationInSeconds.value = 0L
        events.value = null
        
        // Сбрасываем локальные переменные
        allPoints.clear()
        totalDistanceMetres = 0.0
        maxSpeedKmh = 0f
        durationInSeconds.value = 0L
        startLocation = null
        hasLeftStartThreshold = false
    }

    private fun stopService() {
        // Деактивируем сессию, чтобы авто-старт не сработал
        isServiceActive = false
        
        // 1. Сохраняем (копию данных)
        saveRideToDb()
        
        // 2. Сбрасываем UI (сразу же, не дожидаясь БД)
        resetTrackingData()

        // 3. Останавливаем сервис
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        serviceScope.cancel() // Отменяем все корутины сервиса при его уничтожении
    }
}