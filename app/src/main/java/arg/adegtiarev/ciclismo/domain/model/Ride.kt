package arg.adegtiarev.ciclismo.domain.model

data class Ride(
    val id: Long,
    val distance: Double,
    val duration: Long,
    val timestamp: Long,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val routePoints: List<TrackingPoint>
)
