package arg.adegtiarev.ciclismo.domain

import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import kotlinx.coroutines.flow.Flow

interface RideRepository {
    suspend fun getRideById(id: Long): Ride?
    suspend fun deleteRide(ride: Ride)
    fun getAllRides(): Flow<List<Ride>>
    suspend fun saveFullRide(
        ride: Ride,
        points: List<TrackingPoint>
    ): Long

    // Statistics methods
    fun getTotalDistance(): Flow<Double>
    fun getTotalDuration(): Flow<Long>
    fun getTotalRidesCount(): Flow<Int>
}
