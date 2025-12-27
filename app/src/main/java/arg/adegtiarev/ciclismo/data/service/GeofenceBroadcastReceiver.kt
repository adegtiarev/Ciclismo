package arg.adegtiarev.ciclismo.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import arg.adegtiarev.ciclismo.util.TrackingConstants
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            // Handle error
            return
        }

        // Check for the enter transition
        if (geofencingEvent?.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Send a command to our service to show the dialog
            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingConstants.ACTION_GEOFENCE_ENTER
            }
            context.startService(serviceIntent)
        }
    }
}