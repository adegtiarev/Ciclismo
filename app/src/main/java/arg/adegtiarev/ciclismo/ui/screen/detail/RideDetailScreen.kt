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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ride?.let { formatDate(it.timestamp) } ?: "Ride Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
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
                
                // Карта
                DetailMap(ride = currentRide)

                // Статистика (снизу поверх карты)
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
    
    // Используем remember для трансформации точек, чтобы не пересчитывать при каждой рекомпозиции
    val routePoints = remember(ride) { 
        ride.routePoints.map { LatLng(it.latitude, it.longitude) } 
    }
    
    // Флаг загрузки карты. CameraUpdateFactory можно использовать только когда карта готова.
    var isMapLoaded by remember { mutableStateOf(false) }

    // Центрируем карту на маршруте ТОЛЬКО когда карта загрузилась и есть точки
    LaunchedEffect(routePoints, isMapLoaded) {
        if (routePoints.isNotEmpty() && isMapLoaded) {
            val boundsBuilder = LatLngBounds.builder()
            routePoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            
            // Отступ 100px, чтобы маршрут не прилипал к краям
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
