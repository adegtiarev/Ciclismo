package arg.adegtiarev.ciclismo.data.local.mapper

import android.location.Location
import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint

fun Ride.toEntity(): RideEntity = RideEntity(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed
)

fun RideEntity.toDomain(points: List<TrackingPoint> = emptyList()): Ride = Ride(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed,
    routePoints = points
)

fun TrackingPoint.toEntity(): TrackingPointEntity = TrackingPointEntity(
    rideId = this.rideId,
    latitude = this.latitude,
    longitude = this.longitude,
    speed = this.speed,
    timestamp = this.timestamp
)

fun TrackingPointEntity.toDomain(): TrackingPoint = TrackingPoint(
    id = this.id,
    rideId = this.rideId,
    latitude = this.latitude,
    longitude = this.longitude,
    speed = this.speed,
    timestamp = this.timestamp
)

// Добавляем маппер для Location -> TrackingPoint
fun Location.toTrackingPoint(rideId: Long = 0): TrackingPoint {
    return TrackingPoint(
        id = 0,
        rideId = rideId,
        latitude = latitude,
        longitude = longitude,
        speed = speed,
        timestamp = time
    )
}