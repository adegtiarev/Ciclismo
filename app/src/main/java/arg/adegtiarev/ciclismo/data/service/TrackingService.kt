
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
import arg.adegtiarev.ciclismo.util.TrackingConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    @Inject
    lateinit var geofenceManager: GeofenceManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var maxSpeedKmh = 0f
    private var timerJob: Job? = null
    private var locationJob: Job? = null
    private var isSessionActive = false

    companion object {
        private val _isTracking = MutableStateFlow(false)
        val isTracking = _isTracking.asStateFlow()

        private val _pathPoints = MutableStateFlow<List<Location>>(emptyList())
        val pathPoints = _pathPoints.asStateFlow()

        private val _totalDistance = MutableStateFlow(0.0)
        val totalDistance = _totalDistance.asStateFlow()

        private val _durationInSeconds = MutableStateFlow(0L)
        val durationInSeconds = _durationInSeconds.asStateFlow()

        private val _currentSpeedKmh = MutableStateFlow(0f)
        val currentSpeedKmh = _currentSpeedKmh.asStateFlow()

        private val _events = MutableStateFlow<String?>(null)
        val events = _events.asStateFlow()

        fun clearEvent() {
            _events.value = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when (it) {
                TrackingConstants.ACTION_START_OR_RESUME_SERVICE -> startRide()
                TrackingConstants.ACTION_PAUSE_SERVICE -> pauseService()
                TrackingConstants.ACTION_STOP_SERVICE -> stopRide()
                TrackingConstants.ACTION_GEOFENCE_ENTER -> _events.value = TrackingConstants.SHOW_STOP_DIALOG
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRide() {
        if (!isSessionActive) {
            resetData()
        }

        isSessionActive = true
        _isTracking.value = true

        val notification = notificationHelper.createNotification()
        ServiceCompat.startForeground(
            this,
            TrackingConstants.NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        )

        if (locationJob == null || locationJob?.isActive == false) {
            startLocationUpdates()
        }
        startTimer()
    }

    private fun pauseService() {
        _isTracking.value = false
    }

    private fun stopRide() {
        _isTracking.value = false
        isSessionActive = false
        geofenceManager.removeGeofence()

        serviceScope.launch {
            saveRideToDb()?.let { rideId ->
                _events.value = "SAVED_$rideId"
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = locationClient.getLocationUpdates(2000L).onEach { location ->
            if (_pathPoints.value.isEmpty()) {
                // This is the first location, set up the geofence
                geofenceManager.addGeofence(location.latitude, location.longitude)
            }

            _currentSpeedKmh.value = location.speed * 3.6f

            if (_isTracking.value) {
                val newPoints = _pathPoints.value + location
                _pathPoints.value = newPoints

                if (newPoints.size > 1) {
                    val lastTwo = newPoints.takeLast(2)
                    _totalDistance.value += lastTwo[0].distanceTo(lastTwo[1])
                }

                if (_currentSpeedKmh.value > maxSpeedKmh) {
                    maxSpeedKmh = _currentSpeedKmh.value
                }

                if (RideStatsCalculator.shouldAutoPause(location.speed) && _pathPoints.value.size > 1) {
                    pauseService()
                }
            } else {
                if (!RideStatsCalculator.shouldAutoPause(location.speed) && isSessionActive) {
                    _isTracking.value = true
                    startTimer()
                }
            }
        }.launchIn(serviceScope)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_isTracking.value) {
                delay(1000L)
                if (_isTracking.value) {
                    _durationInSeconds.value += 1
                }
            }
        }
    }

    private suspend fun saveRideToDb(): Long? {
        val currentPath = _pathPoints.value
        if (_totalDistance.value < 10.0 || currentPath.isEmpty()) return null

        val ride = Ride(
            id = 0, 
            distance = _totalDistance.value,
            duration = _durationInSeconds.value,
            timestamp = System.currentTimeMillis(),
            averageSpeed = if (_durationInSeconds.value > 0) (_totalDistance.value / _durationInSeconds.value) * 3.6 else 0.0,
            maxSpeed = maxSpeedKmh.toDouble(),
            routePoints = currentPath.map { it.toTrackingPoint() }
        )
        return repository.saveFullRide(ride, ride.routePoints)
    }

    private fun resetData() {
        _isTracking.value = false
        _pathPoints.value = emptyList()
        _totalDistance.value = 0.0
        _currentSpeedKmh.value = 0f
        _durationInSeconds.value = 0L
        maxSpeedKmh = 0f
        _events.value = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure the geofence is removed if the service is destroyed unexpectedly
        if (isSessionActive) {
            geofenceManager.removeGeofence()
        }
        serviceScope.cancel()
    }
}