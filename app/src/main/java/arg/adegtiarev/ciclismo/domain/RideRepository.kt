package arg.adegtiarev.ciclismo.domain

import kotlinx.coroutines.flow.Flow

interface RideRepository {
    suspend fun saveRide(ride: Ride): Long
    suspend fun getRideById(id: Long): Ride?
    suspend fun deleteRide(ride: Ride)
    suspend fun deleteAllRides()
    fun getAllRides(): Flow<List<Ride>>
    suspend fun addTrackingPoints(points: List<TrackingPoint>)
    suspend fun getTrackingPointsForRide(rideId: Long): List<TrackingPoint>
    fun getTrackingPointsForRideFlow(rideId: Long): Flow<List<TrackingPoint>>
}