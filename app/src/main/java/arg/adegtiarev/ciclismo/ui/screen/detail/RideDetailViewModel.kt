package arg.adegtiarev.ciclismo.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arg.adegtiarev.ciclismo.domain.RideRepository
import arg.adegtiarev.ciclismo.domain.model.Ride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NavigationEvent {
    object NavigateBack : NavigationEvent()
}

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    private val repository: RideRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // rideId is retrieved from the navigation arguments
    private val rideId: Long = checkNotNull(savedStateHandle["rideId"])

    private val _ride = MutableStateFlow<Ride?>(null)
    val ride: StateFlow<Ride?> = _ride

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadRide()
    }

    private fun loadRide() {
        viewModelScope.launch {
            _ride.value = repository.getRideById(rideId)
        }
    }

    fun deleteRide() {
        viewModelScope.launch {
            _ride.value?.let {
                repository.deleteRide(it)
                _navigationEvent.emit(NavigationEvent.NavigateBack)
            }
        }
    }
}