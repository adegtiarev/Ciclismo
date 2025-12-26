package arg.adegtiarev.ciclismo.domain.model

data class TrackingPoint(
    val id: Long = 0,
    val rideId: Long, // Ссылка на ID поездки
    val latitude: Double,
    val longitude: Double,
    val speed: Float,     // Мгновенная скорость
    val timestamp: Long
)
