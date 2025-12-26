package arg.adegtiarev.ciclismo.data.service

import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.lifecycle.LifecycleService
import arg.adegtiarev.ciclismo.data.service.notification.NotificationHelper
import arg.adegtiarev.ciclismo.domain.LocationClient
import arg.adegtiarev.ciclismo.util.TrackingConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var notificationHelper: NotificationHelper

    // Используем SupervisorJob, чтобы ошибка в одном процессе не убила весь скоуп
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val isTracking = MutableStateFlow(false)

        // Список точек текущего сегмента пути
        val pathPoints = MutableStateFlow<List<Location>>(emptyList())
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
    }

    private fun startLocationUpdates() {
        locationClient.getLocationUpdates(2000L) // 2 секунды
            .onEach { location ->
                if (isTracking.value) {
                    // Добавляем новую точку в список
                    pathPoints.value = pathPoints.value + location
                    Log.d(
                        "TrackingService",
                        "New location: ${location.latitude}, ${location.longitude}"
                    )
                }
            }
            .launchIn(serviceScope)
    }

    private fun pauseService() {
        isTracking.value = false
    }

    private fun stopService() {
        isTracking.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}