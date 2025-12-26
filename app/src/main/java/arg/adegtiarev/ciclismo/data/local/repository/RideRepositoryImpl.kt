package arg.adegtiarev.ciclismo.data.local.repository

import arg.adegtiarev.ciclismo.data.local.dao.RideDao
import arg.adegtiarev.ciclismo.data.local.dao.TrackingPointDao
import arg.adegtiarev.ciclismo.data.local.mapper.toDomain
import arg.adegtiarev.ciclismo.data.local.mapper.toEntity
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val rideDao: RideDao,
    private val trackingPointDao: TrackingPointDao
) : RideRepository {
    override suspend fun saveRide(ride: Ride): Long {
        return rideDao.insertRide(ride.toEntity())
    }

    override suspend fun getRideById(id: Long): Ride? {
        // Здесь мы можем использовать getRideWithPoints из DAO
        // и собрать объект Ride вместе со списком точек
        return null // Реализуем детально, когда дойдем до экрана деталей
    }

    override suspend fun deleteRide(ride: Ride) {
        rideDao.deleteRide(ride.toEntity())
    }

    override suspend fun deleteAllRides() {
        rideDao.deleteAllRides()
    }

    override fun getAllRides(): Flow<List<Ride>> {
        return rideDao.getAllRides().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTrackingPoints(points: List<TrackingPoint>) {
        val entities = points.map { it.toEntity() }
        trackingPointDao.insertTrackingPoints(entities)
    }

    override suspend fun getTrackingPointsForRide(rideId: Long): List<TrackingPoint> {
        return trackingPointDao.getTrackingPointsForRide(rideId).map { it.toDomain() }
    }

    override fun getTrackingPointsForRideFlow(rideId: Long): Flow<List<TrackingPoint>> {
        return trackingPointDao.getTrackingPointsForRideFlow(rideId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveFullRide(ride: Ride, points: List<TrackingPoint>) {
        rideDao.saveFullRide(
            ride.toEntity(),
            points.map { it.toEntity() }
        )
    }
}