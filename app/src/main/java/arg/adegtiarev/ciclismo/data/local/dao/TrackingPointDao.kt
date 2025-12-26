package arg.adegtiarev.ciclismo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingPoints(points: List<TrackingPointEntity>)

    @Query("SELECT * FROM tracking_points WHERE rideId = :rideId")
    suspend fun getTrackingPointsForRide(rideId: Long): List<TrackingPointEntity>

    @Query("SELECT * FROM tracking_points WHERE rideId = :rideId")
    fun getTrackingPointsForRideFlow(rideId: Long): Flow<List<TrackingPointEntity>>

}