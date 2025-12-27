package arg.adegtiarev.ciclismo.domain.model

data class TrackingPoint(
    val id: Long = 0,
    val rideId: Long, // Foreign key to the Ride
    val latitude: Double,
    val longitude: Double,
    val speed: Float,     // Instantaneous speed
    val timestamp: Long
)
