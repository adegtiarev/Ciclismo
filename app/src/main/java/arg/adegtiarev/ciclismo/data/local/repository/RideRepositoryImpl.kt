package arg.adegtiarev.ciclismo.data.local.repository

import arg.adegtiarev.ciclismo.data.local.dao.RideDao
import arg.adegtiarev.ciclismo.data.local.mapper.toDomain
import arg.adegtiarev.ciclismo.data.local.mapper.toEntity
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val rideDao: RideDao
) : RideRepository {

    override suspend fun getRideById(id: Long): Ride? {
        // Now using the efficient @Transaction method
        return rideDao.getRideWithPoints(id)?.toDomain()
    }

    override suspend fun deleteRide(ride: Ride) {
        rideDao.deleteRide(ride.toEntity())
    }

    override fun getAllRides(): Flow<List<Ride>> {
        return rideDao.getAllRides().map { entities ->
            entities.map { it.toDomain() } // Uses default emptyList() for points
        }
    }

    override suspend fun saveFullRide(ride: Ride, points: List<TrackingPoint>): Long {
        return rideDao.saveFullRide(
            ride.toEntity(),
            points.map { it.toEntity() }
        )
    }

    override fun getTotalDistance(): Flow<Double> {
        return rideDao.getTotalDistance().map { it ?: 0.0 }
    }

    override fun getTotalDuration(): Flow<Long> {
        return rideDao.getTotalDuration().map { it ?: 0L }
    }

    override fun getTotalRidesCount(): Flow<Int> {
        return rideDao.getTotalRidesCount()
    }
}