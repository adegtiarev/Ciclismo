package arg.adegtiarev.ciclismo.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.model.Ride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val repository: RideRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rideId: Long = checkNotNull(savedStateHandle["rideId"])

    private val _ride = MutableStateFlow<Ride?>(null)
    val ride: StateFlow<Ride?> = _ride

    init {
        loadRide()
    }

    private fun loadRide() {
        viewModelScope.launch {
            _ride.value = repository.getRideById(rideId)
        }
    }
}