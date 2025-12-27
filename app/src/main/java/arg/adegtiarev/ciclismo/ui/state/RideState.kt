package arg.adegtiarev.ciclismo.ui.state

import arg.adegtiarev.ciclismo.domain.model.TrackingPoint

data class RideState(
    val isTracking: Boolean = false,
    val distanceMetres: Double = 0.0,
    val durationSeconds: Long = 0L,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val isAutoPaused: Boolean = false,
    val points: List<TrackingPoint> = emptyList()
)
