package arg.adegtiarev.ciclismo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.RideWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long // Возвращает ID созданной поездки

    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    // Метод для получения поездки со всеми точками
    @Transaction
    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun getRideWithPoints(rideId: Int): Flow<RideWithPoints>

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides")
    suspend fun deleteAllRides()
}