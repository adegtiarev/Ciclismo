package arg.adegtiarev.ciclismo.domain

import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
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
    suspend fun saveFullRide(ride: Ride, points: List<TrackingPoint>): Long // Теперь возвращает Long
    
    // Методы статистики
    fun getTotalDistance(): Flow<Double>
    fun getTotalDuration(): Flow<Long>
    fun getTotalRidesCount(): Flow<Int>
}