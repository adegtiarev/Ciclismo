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

    // Use SupervisorJob so one child failure doesn't cancel the whole scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Local variables for current ride data
    private var totalDistanceMetres = 0.0
    private var maxSpeedKmh = 0f
    private val allPoints = mutableListOf<TrackingPoint>() // List for DB

    private var durationInSeconds = MutableStateFlow(0L)

    private var timerJob: Job? = null
    private var locationJob: Job? = null

    private var startLocation: Location? = null
    private var hasLeftStartThreshold = false

    // Flag indicating that ride session is active (user pressed Start and hasn't pressed Stop)
    private var isServiceActive = false

    companion object {
        val isTracking = MutableStateFlow(false)

        // List of points for current path segment
        val pathPoints = MutableStateFlow<List<Location>>(emptyList())

        // Exposed fields for ViewModel
        val currentSpeedKmh = MutableStateFlow(0f)
        val totalDistanceMetres = MutableStateFlow(0.0)
        val durationInSeconds = MutableStateFlow(0L)

        val events = MutableStateFlow<String?>(null) // For UI events
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                TrackingConstants.ACTION_START_OR_RESUME_SERVICE -> {
                    // Activate session on explicit start
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
        // 1. Set status
        isTracking.value = true

        // 2. Foreground notification
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

        // 3. Start location updates if not running
        if (locationJob == null || locationJob?.isActive == false) {
            startLocationUpdates()
        }

        // 4. Start timer (it cancels old one itself)
        startTimer()
    }

    private fun startLocationUpdates() {
        locationJob?.cancel() // Just in case
        locationJob = locationClient.getLocationUpdates(2000L)
            .onEach { location ->
                // Always update speed to show "0" on pause
                val speed = location.speed * 3.6f
                currentSpeedKmh.value = speed

                // Determine if we should be paused (speed < threshold)
                val shouldBePaused = RideStatsCalculator.shouldAutoPause(location.speed)

                if (isTracking.value) {
                    if (shouldBePaused) {
                        // Moving but stopped -> Pause (UI shows RESUME/STOP)
                        pauseService()
                    } else {
                        // Moving normally -> Record data
                        val newPoint = location.toTrackingPoint()
                        addPointAndCalculate(newPoint)
                        pathPoints.value += location
                        TrackingService.totalDistanceMetres.value = this.totalDistanceMetres
                    }
                } else {
                    // We are paused (auto or manual)
                    // AUTO-START triggers ONLY if session is active (isServiceActive == true)
                    if (!shouldBePaused && isServiceActive) {
                        // Started moving -> Auto-start
                        isTracking.value = true
                        startTimer()
                    }
                }

                // Stop dialog logic (independent of pause)
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

            // 1. Check if we left the start area far enough
            if (distanceToStart > 100f) {
                hasLeftStartThreshold = true
            }

            // 2. Auto-stop logic
            // If left (>100m) and returned (<20m) -> show dialog
            if (hasLeftStartThreshold && distanceToStart < 20f) {
                events.value = SHOW_STOP_DIALOG
            }

            // If left start again (> 50m), hide dialog
            if (hasLeftStartThreshold && distanceToStart > 50f) {
                events.value = HIDE_STOP_DIALOG
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            // Ensure loop sees actual value
            while (isTracking.value) {
                delay(1000L)
                if (isTracking.value) {
                    // Directly update static field
                    TrackingService.durationInSeconds.value += 1
                    // And local for DB saving
                    durationInSeconds.value = TrackingService.durationInSeconds.value
                }
            }
        }
    }

    private fun pauseService() {
        isTracking.value = false
        // Timer stops itself as while(isTracking.value) becomes false
    }

    private fun addPointAndCalculate(newPoint: TrackingPoint) {
        // 1. Calculate distance if not first point
        if (allPoints.isNotEmpty()) {
            val lastPoint = allPoints.last()
            val distanceBetween = RideStatsCalculator.calculateDistance(listOf(lastPoint, newPoint))
            totalDistanceMetres += distanceBetween
        }

        // 2. Add to list for DB
        allPoints.add(newPoint)

        // 3. Update max speed
        val currentSpeedKmh = newPoint.speed * 3.6f
        if (currentSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = currentSpeedKmh
        }
    }

    // Call this at the end of ride:
    private fun saveRideToDb() {
        val finalDistance = totalDistanceMetres
        val finalDuration = durationInSeconds.value
        val pointsToSave = allPoints.toList() // Copy list

        if (finalDistance > 10.0 && pointsToSave.isNotEmpty()) { // Min 10 meters to save
            serviceScope.launch {
                val avgSpeed = if (finalDuration > 0) (finalDistance / finalDuration) * 3.6 else 0.0

                val finalRide = Ride(
                    id = 0, // Generated by Room
                    distance = finalDistance,
                    duration = finalDuration,
                    timestamp = System.currentTimeMillis(),
                    averageSpeed = avgSpeed,
                    maxSpeed = maxSpeedKmh.toDouble(),
                    routePoints = pointsToSave
                )
                // Now we get ID to send to UI
                val rideId = repository.saveFullRide(finalRide, pointsToSave)

                // Send event to UI
                events.emit("SAVED_$rideId")
            }
        }
    }

    private fun resetTrackingData() {
        // Reset static fields for UI update
        isTracking.value = false
        pathPoints.value = emptyList()
        TrackingService.totalDistanceMetres.value = 0.0
        currentSpeedKmh.value = 0f
        TrackingService.durationInSeconds.value = 0L
        events.value = null

        // Reset local variables
        allPoints.clear()
        totalDistanceMetres = 0.0
        maxSpeedKmh = 0f
        durationInSeconds.value = 0L
        startLocation = null
        hasLeftStartThreshold = false
    }

    private fun stopService() {
        // Deactivate session so auto-start won't trigger
        isServiceActive = false

        // 1. Save (copy of data)
        saveRideToDb()

        // 2. Reset UI (immediately, not waiting for DB)
        resetTrackingData()

        // 3. Stop service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        serviceScope.cancel() // Cancel all service coroutines on destroy
    }
}