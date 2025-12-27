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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import arg.adegtiarev.ciclismo.R
import arg.adegtiarev.ciclismo.data.service.TrackingService
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.ui.state.RideState
import arg.adegtiarev.ciclismo.util.TrackingConstants
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
        onDispose { currentView.keepScreenOn = false }
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

    LaunchedEffect(serviceEvent) {
        serviceEvent?.let {
            when {
                it.startsWith("SAVED_") -> {
                    val rideId = it.substringAfter("SAVED_").toLong()
                    TrackingService.clearEvent()
                    onNavigateToDetail(rideId)
                }
                it == TrackingConstants.SHOW_STOP_DIALOG -> {
                    viewModel.setDialogVisibility(true)
                    TrackingService.clearEvent()
                }
                it == TrackingConstants.HIDE_STOP_DIALOG -> {
                    viewModel.setDialogVisibility(false)
                    TrackingService.clearEvent()
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDialogVisibility(false) },
            title = { Text("Finish ride? 🏁") },
            text = { Text("Looks like you returned to the start. Do you want to save the route?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.sendCommand(TrackingConstants.ACTION_STOP_SERVICE)
                    viewModel.setDialogVisibility(false)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDialogVisibility(false) }) { Text("Cancel") }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.serviceCommand.collect { action ->
            val intent = Intent(context, TrackingService::class.java).apply { this.action = action }
            if (action == TrackingConstants.ACTION_START_OR_RESUME_SERVICE) {
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

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    if (permissionState.allPermissionsGranted) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Ride") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            enabled = !state.isTracking
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
                    onStopClick = { viewModel.sendCommand(TrackingConstants.ACTION_STOP_SERVICE) }
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
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "The app needs access to GPS and notifications to record the route",
                        modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    )
                    Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                        Text("Grant permission")
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

    LaunchedEffect(path.size, isFollowing) {
        if (isFollowing && path.isNotEmpty()) {
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(path.last())
                    .zoom(18f)
                    .bearing(0f)
                    .tilt(45f)
                    .build()
            )
            cameraPositionState.animate(cameraUpdate, 1000)
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isFollowing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                compassEnabled = true
            ),
            onMyLocationButtonClick = {
                isFollowing = true
                path.isNotEmpty()
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
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem("Speed", "${state.currentSpeedKmh.toInt()} km/h")
                StatisticItem("Distance", formatDistance(state.distanceMetres))
                StatisticItem("Time", formatDuration(state.durationSeconds))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                when {
                    state.durationSeconds == 0L && !state.isTracking -> {
                        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth(0.8f)) {
                            Text("START RIDE")
                        }
                    }
                    state.isTracking -> {
                        Button(onClick = onPauseClick, modifier = Modifier.fillMaxWidth(0.8f)) {
                            Text("PAUSE")
                        }
                    }
                    else -> {
                        Row {
                            Button(onClick = onResumeClick, modifier = Modifier.weight(1f)) {
                                Text("RESUME")
                            }
                            Spacer(Modifier.width(16.dp))
                            FilledTonalButton(onClick = onStopClick, modifier = Modifier.weight(1f)) {
                                Text("STOP", color = Color.Red)
                            }
                        }
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
        ), {}, {}, {}, {}
    )
}
