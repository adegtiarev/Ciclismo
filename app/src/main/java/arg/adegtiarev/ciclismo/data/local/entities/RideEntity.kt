package arg.adegtiarev.ciclismo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val distance: Double,
    val duration: Long,
    val timestamp: Long,
    val averageSpeed: Double,
    val maxSpeed: Double
)