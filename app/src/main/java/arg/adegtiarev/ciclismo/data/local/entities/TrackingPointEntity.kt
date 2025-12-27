package arg.adegtiarev.ciclismo.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_points",
    foreignKeys = [
        ForeignKey(
            entity = RideEntity::class,
            parentColumns = ["id"],
            childColumns = ["rideId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rideId")] // It's a good practice to add an index for foreign keys
)
data class TrackingPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rideId: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val timestamp: Long
)