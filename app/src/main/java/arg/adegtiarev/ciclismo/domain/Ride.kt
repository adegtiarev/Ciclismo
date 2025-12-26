package arg.adegtiarev.ciclismo.domain

data class Ride(
    val id: Int,
    val distance: Double,
    val duration: Long,
    val timestamp: Long,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val routePoints: List<TrackingPoint>
)
