package arg.adegtiarev.ciclismo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.RideWithPoints
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long // Returns the ID of the created ride

    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    // Method to get a ride with all its points
    @Transaction
    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideWithPoints(rideId: Long): RideWithPoints?

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Transaction
    suspend fun saveFullRide(ride: RideEntity, points: List<TrackingPointEntity>): Long {
        val rideId = insertRide(ride) // Get the generated ID
        val pointsWithId = points.map { it.copy(rideId = rideId) } // Link points to the ride ID
        insertTrackingPoints(pointsWithId)
        return rideId
    }

    // Add this method here so saveFullRide can call it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingPoints(points: List<TrackingPointEntity>)

    @Query("SELECT SUM(distance) FROM rides")
    fun getTotalDistance(): Flow<Double?>

    @Query("SELECT SUM(duration) FROM rides")
    fun getTotalDuration(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM rides")
    fun getTotalRidesCount(): Flow<Int>
}