package arg.adegtiarev.ciclismo.ui.screen

import android.Manifest
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import arg.adegtiarev.ciclismo.data.service.TrackingService
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.ui.state.RideState
import arg.adegtiarev.ciclismo.ui.viewmodel.MainViewModel
import arg.adegtiarev.ciclismo.util.TrackingConstants
import arg.adegtiarev.ciclismo.util.formatDistance
import arg.adegtiarev.ciclismo.util.formatDuration
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier.statusBarsPadding(),
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.rideState.collectAsState()
    val context = LocalContext.current

    // Слушаем команды для сервиса
    LaunchedEffect(Unit) {
        viewModel.serviceCommand.collect { action ->
            val intent = Intent(context, TrackingService::class.java).apply {
                this.action = action
            }
            context.startService(intent)
        }
    }


    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (permissionState.allPermissionsGranted) {
        // Показываем карту и панель
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Тут будет панель со статистикой (скорость, время) и кнопками
                TrackingBottomPanel(
                    state = state,
                    onStartClick = { viewModel.sendCommand(TrackingConstants.ACTION_START_OR_RESUME_SERVICE) },
                    onPauseClick = { viewModel.sendCommand(TrackingConstants.ACTION_PAUSE_SERVICE) },
                    onStopClick = { viewModel.sendCommand(TrackingConstants.ACTION_STOP_SERVICE) }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Место для нашей карты
                CiclismoMap(points = state.points)
            }
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center // Центрируем содержимое
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Приложению нужен доступ к GPS для записи маршрута",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("Разрешить GPS")
                    }
                }
            }
        }
    }
}

@Composable
fun CiclismoMap(points: List<TrackingPoint>) {
    val cameraPositionState = rememberCameraPositionState()
    var isFollowing by remember { mutableStateOf(true) }

    val path = remember(points) {
        points.map { LatLng(it.latitude, it.longitude) }
    }

    // Слушатель движения карты
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            isFollowing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // ВАЖНО: isMyLocationEnabled заставляет карту саму найти тебя
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            if (path.isNotEmpty()) {
                Polyline(points = path, color = Color.Blue, width = 12f)
            }
        }

        // ЭФФЕКТ ЦЕНТРИРОВАНИЯ
        LaunchedEffect(path.size, isFollowing) {
            if (isFollowing && path.isNotEmpty()) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(path.last(), 16f)
                )
            }
        }

        if (!isFollowing) {
            Button(
                onClick = { isFollowing = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).statusBarsPadding()
            ) {
                Text("Center")
            }
        }
    }
}

@Composable
fun TrackingBottomPanel(
    state: RideState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        tonalElevation = 8.dp, // Немного приподнимем панель над картой
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem("Speed", "${state.currentSpeedKmh.toInt()} km/h")
                StatisticItem("Distance", formatDistance(state.distanceMetres))
                StatisticItem("Time", formatDuration(state.durationSeconds))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!state.isTracking) {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.fillMaxWidth(0.8f) // Широкая кнопка Старт
                    ) {
                        Text("START RIDE")
                    }
                } else {
                    Button(onClick = onPauseClick) { Text("PAUSE") }
                    Spacer(Modifier.width(16.dp))
                    androidx.compose.material3.FilledTonalButton(onClick = onStopClick) {
                        Text("STOP", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        Text(text = value, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    }
}

@Preview
@Composable
fun TrackingBottomPanelPreview() {
    TrackingBottomPanel(
        state = RideState(
            isTracking = true,
            distanceMetres = 1000.0,
            durationSeconds = 120L,
            currentSpeedKmh = 10f,
            isAutoPaused = false,
            points = emptyList()
        ), onStartClick = {}, onPauseClick = {}, onStopClick = {})
}

//@Preview
//@Composable
//fun TrackingScreenPreview() {
//    TrackingScreen()
//}