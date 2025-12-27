package arg.adegtiarev.ciclismo.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import arg.adegtiarev.ciclismo.R
import arg.adegtiarev.ciclismo.domain.model.Ride
import arg.adegtiarev.ciclismo.util.formatDate
import arg.adegtiarev.ciclismo.util.formatDistance
import arg.adegtiarev.ciclismo.util.formatDuration
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    navController: NavController,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val ride by viewModel.ride.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            when (it) {
                is NavigationEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ride") },
            text = { Text("Are you sure you want to delete this ride? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteRide()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ride?.let { formatDate(it.timestamp) } ?: "Ride Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Delete ride")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (ride != null) {
                val currentRide = ride!!
                
                // Map
                DetailMap(ride = currentRide)

                // Statistics (at the bottom, on top of the map)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    RideDetailStats(ride = currentRide)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading ride details...")
                }
            }
        }
    }
}

@Composable
fun DetailMap(ride: Ride) {
    val cameraPositionState = rememberCameraPositionState()
    
    // Use remember for point transformation to avoid recalculation on every recomposition
    val routePoints = remember(ride) { 
        ride.routePoints.map { LatLng(it.latitude, it.longitude) } 
    }
    
    // Map loaded flag. CameraUpdateFactory can only be used when the map is ready.
    var isMapLoaded by remember { mutableStateOf(false) }

    // Center the map on the route ONLY when the map is loaded and there are points
    LaunchedEffect(routePoints, isMapLoaded) {
        if (routePoints.isNotEmpty() && isMapLoaded) {
            val boundsBuilder = LatLngBounds.builder()
            routePoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            
            // 100px padding so the route doesn't stick to the edges
            try {
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        ),
        onMapLoaded = {
            isMapLoaded = true
        }
    ) {
        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 12f
            )
        }
    }
}

@Composable
fun RideDetailStats(ride: Ride) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DetailStatItem(
                    label = "Distance",
                    value = formatDistance(ride.distance)
                )
                DetailStatItem(
                    label = "Duration",
                    value = formatDuration(ride.duration)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                 DetailStatItem(
                    label = "Avg Speed",
                    value = "${ride.averageSpeed.toInt()} km/h"
                )
                 DetailStatItem(
                    label = "Max Speed",
                    value = "${ride.maxSpeed.toInt()} km/h"
                )
            }
        }
    }
}

@Composable
fun DetailStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
