package arg.adegtiarev.ciclismo.data.local.mapper

import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.RideWithPoints
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint

// Ride -> RideEntity
fun Ride.toEntity(): RideEntity = RideEntity(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed
)

// RideEntity -> Ride (without points, for list view)
fun RideEntity.toDomain(): Ride = Ride(
    id = this.id,
    distance = this.distance,
    duration = this.duration,
    timestamp = this.timestamp,
    averageSpeed = this.averageSpeed,
    maxSpeed = this.maxSpeed,
    routePoints = emptyList()
)

// RideWithPoints -> Ride (with points, for detail view)
fun RideWithPoints.toDomain(): Ride = Ride(
    id = this.ride.id,
    distance = this.ride.distance,
    duration = this.ride.duration,
    timestamp = this.ride.timestamp,
    averageSpeed = this.ride.averageSpeed,
    maxSpeed = this.ride.maxSpeed,
    routePoints = this.points.map { it.toDomain() }
)

// TrackingPoint -> TrackingPointEntity
fun TrackingPoint.toEntity(): TrackingPointEntity = TrackingPointEntity(
    rideId = this.rideId,
    latitude = this.latitude,
    longitude = this.longitude,
    speed = this.speed,
    timestamp = this.timestamp
)

// TrackingPointEntity -> TrackingPoint
fun TrackingPointEntity.toDomain(): TrackingPoint = TrackingPoint(
    id = this.id,
    rideId = this.rideId,
    latitude = this.latitude,
    longitude = this.longitude,
    speed = this.speed,
    timestamp = this.timestamp
)
