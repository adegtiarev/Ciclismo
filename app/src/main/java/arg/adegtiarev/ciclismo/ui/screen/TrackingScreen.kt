package arg.adegtiarev.ciclismo.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import arg.adegtiarev.ciclismo.data.service.TrackingService
import arg.adegtiarev.ciclismo.domain.model.TrackingPoint
import arg.adegtiarev.ciclismo.ui.state.RideState
import arg.adegtiarev.ciclismo.ui.viewmodel.MainViewModel
import arg.adegtiarev.ciclismo.util.TrackingConstants

@Composable
fun TrackingScreen(
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

    Scaffold(
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
}

@Composable
fun CiclismoMap(points: List<TrackingPoint>) {

}

@Composable
fun TrackingBottomPanel(
    state: RideState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Speed: ${state.currentSpeedKmh}")
            Text(text = "Distance: ${state.distanceMetres}")
            Text(text = "Duration: ${state.durationSeconds}")
        }
        // Логика кнопок
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (!state.isTracking) {
                Button(onClick = onStartClick) { Text("Start") }
            } else {
                Button(onClick = onPauseClick) { Text("Pause") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onStopClick) { Text("Stop") }
            }
        }
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