package arg.adegtiarev.ciclismo.domain

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<Location>
    suspend fun getLastLocation(): Location?
    class LocationException(message: String) : Exception(message)
}