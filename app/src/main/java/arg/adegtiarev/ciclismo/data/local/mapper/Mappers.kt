package arg.adegtiarev.ciclismo.data.local.mapper

import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity
import arg.adegtiarev.ciclismo.domain.Ride
import arg.adegtiarev.ciclismo.domain.TrackingPoint

fun Ride.toEntity(): RideEntity = RideEntity(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed
)

fun RideEntity.toDomain(): Ride = Ride(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed,
    routePoints = emptyList() // В общем списке точки не нужны
)

// И аналогично для TrackingPoint
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