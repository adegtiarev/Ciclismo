package arg.adegtiarev.ciclismo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import arg.adegtiarev.ciclismo.data.local.dao.RideDao
import arg.adegtiarev.ciclismo.data.local.dao.TrackingPointDao
import arg.adegtiarev.ciclismo.data.local.entities.RideEntity
import arg.adegtiarev.ciclismo.data.local.entities.TrackingPointEntity

@Database(entities = [RideEntity::class, TrackingPointEntity::class], version = 1, exportSchema = false)
abstract class CiclismoDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun trackingPointDao(): TrackingPointDao
}
