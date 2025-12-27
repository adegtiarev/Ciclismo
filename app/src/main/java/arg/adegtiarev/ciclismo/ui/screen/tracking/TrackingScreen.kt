package arg.adegtiarev.ciclismo.ui.screen.tracking

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import arg.adegtiarev.ciclismo.R
import arg.adegtiarev.ciclismo.data.service.TrackingService
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.ui.state.RideState
import arg.adegtiarev.ciclismo.util.TrackingConstants
import arg.adegtiarev.ciclismo.util.TrackingConstants.HIDE_STOP_DIALOG
import arg.adegtiarev.ciclismo.util.TrackingConstants.SHOW_STOP_DIALOG
import arg.adegtiarev.ciclismo.util.formatDistance
import arg.adegtiarev.ciclismo.util.formatDuration
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    navController: NavController,
    viewModel: TrackingViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val state by viewModel.rideState.collectAsState()
    val context = LocalContext.current

    KeepScreenOn()

    val showDialog = viewModel.showExitDialog
    val serviceEvent by viewModel.serviceEvents.collectAsState()

    // Следим за событиями от сервиса
    LaunchedEffect(serviceEvent) {
        val event = serviceEvent
        if (event != null) {
            when {
                event == SHOW_STOP_DIALOG -> viewModel.setDialogVisibility(true)
                event == HIDE_STOP_DIALOG -> {
                    viewModel.setDialogVisibility(false)
                    viewModel.clearServiceEvent()
                }
                event.startsWith("SAVED_") -> {
                    val rideId = event.substringAfter("SAVED_").toLongOrNull()
                    viewModel.clearServiceEvent()
                    if (rideId != null) {
                        onNavigateToDetail(rideId)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.setDialogVisibility(false)
                viewModel.clearServiceEvent()
            },
            title = { Text("Завершить поездку? 🏁") },
            text = { Text("Похоже, вы вернулись к старту. Хотите сохранить маршрут?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.setDialogVisibility(false)
                    viewModel.clearServiceEvent()
                    viewModel.sendCommand(TrackingConstants.ACTION_STOP_SERVICE)
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDialogVisibility(false)
                    viewModel.clearServiceEvent()
                }) { Text("Отмена") }
            }
        )
    }

    // Слушаем команды для сервиса
    LaunchedEffect(Unit) {
        viewModel.serviceCommand.collect { action ->
            val intent = Intent(context, TrackingService::class.java).apply {
                this.action = action
            }
            if (action == TrackingConstants.ACTION_START_OR_RESUME_SERVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest
    )

    if (permissionState.allPermissionsGranted) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Ride") },
                    navigationIcon = {
                        // Кнопка назад работает, только если запись НЕ идет
                        IconButton(
                            onClick = { navController.popBackStack() },
                            enabled = !state.isTracking // Блокируем кнопку, если идет запись
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                TrackingBottomPanel(
                    state = state,
                    onStartClick = { viewModel.sendCommand(TrackingConstants.ACTION_START_OR_RESUME_SERVICE) },
                    onPauseClick = { viewModel.sendCommand(TrackingConstants.ACTION_PAUSE_SERVICE) },
                    onResumeClick = { viewModel.sendCommand(TrackingConstants.ACTION_START_OR_RESUME_SERVICE) },
                    onStopClick = {
                        viewModel.setDialogVisibility(true) // Ручной стоп тоже вызывает диалог
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                CiclismoMap(points = state.points)
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Ride") },
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
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Приложению нужен доступ к GPS и уведомлениям для записи маршрута",
                        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    )
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("Разрешить доступ")
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

    // Авто-центрирование и следование за пользователем
    LaunchedEffect(path.size, isFollowing) {
        if (isFollowing && path.isNotEmpty()) {
            val lastPoint = path.last()
            
            // Если мы следуем за пользователем, обновляем и позицию, и азимут
            // Используем CameraPosition.Builder для сохранения зума и установки bearing
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(lastPoint)
                    .zoom(18f) // Держим зум
                    .bearing(0f) // Пока фиксированный север. Чтобы вращать карту по движению, нужно передавать bearing из Location
                    .tilt(45f) // Небольшой наклон для красоты
                    .build()
            )
            
            cameraPositionState.animate(cameraUpdate, 1000)
        }
    }

    // Отключение слежения только при жесте пользователя
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                isFollowing = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                compassEnabled = true // Включаем компас
            ),
            onMyLocationButtonClick = {
                isFollowing = true
                if (path.isNotEmpty()) {
                    true
                } else {
                    false
                }
            }
        ) {
            if (path.isNotEmpty()) {
                Polyline(points = path, color = Color.Blue, width = 12f)
            }
        }
    }
}

@Composable
fun TrackingBottomPanel(
    state: RideState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Если поездка ещё не начата (или сброшена в 0)
                if (state.durationSeconds == 0L && !state.isTracking) {
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("START RIDE")
                    }
                } 
                // Если поездка идет (активна)
                else if (state.isTracking) {
                    Button(
                        onClick = onPauseClick,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("PAUSE")
                    }
                } 
                // Если поездка на паузе
                else {
                    Button(
                        onClick = onResumeClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RESUME")
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalButton(
                        onClick = onStopClick,
                        modifier = Modifier.weight(1f)
                    ) {
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
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleLarge)
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
        ), onStartClick = {}, onPauseClick = {}, onResumeClick = {}, onStopClick = {})
}
