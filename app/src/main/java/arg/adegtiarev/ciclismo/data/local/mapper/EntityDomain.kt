package arg.adegtiarev.ciclismo.data.local.mapper

import android.location.Location
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint

fun Location.toTrackingPoint(): TrackingPoint {
    return TrackingPoint(
        latitude = latitude,
        longitude = longitude,
        speed = speed,
        timestamp = System.currentTimeMillis(),
        rideId = 0
    )
}