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
    suspend fun insertRide(ride: RideEntity): Long // Возвращает ID созданной поездки

    @Query("SELECT * FROM rides ORDER BY timestamp DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    // Метод для получения поездки со всеми точками
    @Transaction
    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun getRideWithPoints(rideId: Long): Flow<RideWithPoints>

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides")
    suspend fun deleteAllRides()

    @Transaction
    suspend fun saveFullRide(ride: RideEntity, points: List<TrackingPointEntity>) {
        val rideId = insertRide(ride) // Получаем сгенерированный ID
        val pointsWithId = points.map { it.copy(rideId = rideId) } // Привязываем точки к ID поездки
        insertTrackingPoints(pointsWithId)
    }

    // Добавляем этот метод сюда, чтобы saveFullRide мог его вызвать
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackingPoints(points: List<TrackingPointEntity>)
}