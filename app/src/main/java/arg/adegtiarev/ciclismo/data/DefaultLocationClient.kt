package arg.adegtiarev.ciclismo.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import arg.adegtiarev.ciclismo.domain.LocationClient
import arg.adegtiarev.ciclismo.util.hasLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            // 1. Check permissions (simplified)
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permission")
            }

            // 2. Configure request
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(interval)
                .build()

            // 3. Create callback
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) } // Send location to flow
                    }
                }
            }

            // 4. Register listener
            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            // 5. Wait for close (important for cleanup!)
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): Location? {
        if(!context.hasLocationPermission()) {
            return null
        }
        return try {
            client.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}