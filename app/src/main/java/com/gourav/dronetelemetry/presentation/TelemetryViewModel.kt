package com.gourav.dronetelemetry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import com.gourav.dronetelemetry.domain.model.TelemetryConnectionStatus
import com.gourav.dronetelemetry.domain.model.TelemetryData
import com.gourav.dronetelemetry.domain.model.TelemetryEvent
import com.gourav.dronetelemetry.domain.usecase.GetCachedTelemetryUseCase
import com.gourav.dronetelemetry.domain.usecase.ObserveTelemetryUseCase
import com.gourav.dronetelemetry.domain.usecase.SendDroneCommandUseCase
import com.gourav.dronetelemetry.domain.usecase.ValidateEndpointUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TelemetryViewModel @Inject constructor(
    private val validateEndpoint: ValidateEndpointUseCase,
    private val observeTelemetry: ObserveTelemetryUseCase,
    private val sendDroneCommand: SendDroneCommandUseCase,
    getCachedTelemetry: GetCachedTelemetryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(createInitialState(getCachedTelemetry()))
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    private var connectionJob: Job? = null

    fun onProtocolChange(protocol: EndpointProtocol) {
        _uiState.update { it.copy(protocol = protocol, lastError = null) }
    }

    fun onHostChange(host: String) {
        _uiState.update { it.copy(host = host, hostError = null, lastError = null) }
    }

    fun onPortChange(port: String) {
        _uiState.update { it.copy(port = port, portError = null, lastError = null) }
    }

    fun connect() {
        val state = _uiState.value
        val validation = validateEndpoint(state.protocol, state.host, state.port)
        if (!validation.isValid || validation.endpoint == null) {
            _uiState.update {
                it.copy(
                    hostError = validation.hostError,
                    portError = validation.portError,
                    connectionStatus = TelemetryConnectionStatus.ERROR,
                    statusMessage = "Fix connection details",
                    isLoading = false,
                    lastError = "Invalid endpoint"
                )
            }
            return
        }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            observeTelemetry(validation.endpoint).collect { event ->
                when (event) {
                    is TelemetryEvent.Status -> applyStatus(event)
                    is TelemetryEvent.Telemetry -> _uiState.update {
                        it.copy(
                            telemetry = event.data,
                            connectionStatus = TelemetryConnectionStatus.CONNECTED,
                            statusMessage = "Connected",
                            isLoading = false,
                            lastError = null
                        )
                    }
                    is TelemetryEvent.Error -> _uiState.update {
                        it.copy(
                            connectionStatus = TelemetryConnectionStatus.RECONNECTING,
                            statusMessage = "Reconnecting",
                            isLoading = true,
                            lastError = event.message
                        )
                    }
                    // Real acknowledgement from the drone, overrides the optimistic message.
                    is TelemetryEvent.CommandAck -> _uiState.update {
                        it.copy(actionMessage = "${event.ack.commandId.toCommandLabel()}: ${event.ack.result.label}")
                    }
                }
            }
        }
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        _uiState.update {
            it.copy(
                connectionStatus = TelemetryConnectionStatus.DISCONNECTED,
                statusMessage = "Disconnected",
                isLoading = false,
                lastError = null
            )
        }
    }

    fun onAction(action: DroneAction) {
        viewModelScope.launch {
            // we check if drone is armed before sending takeoff. if it is not armed we block takeoff and show warning directly, safety first.
            if (action == DroneAction.TAKEOFF && !_uiState.value.telemetry.armed) {
                _uiState.update { it.copy(actionMessage = "Takeoff Denied: Drone must be armed first") }
                return@launch
            }
            val result = sendDroneCommand(action.toCommand())
            _uiState.update { state ->
                val telemetry = if (result.success) {
                    when (action) {
                        DroneAction.ARM -> state.telemetry.copy(armed = true)
                        DroneAction.DISARM -> state.telemetry.copy(armed = false)
                        DroneAction.TAKEOFF -> state.telemetry.copy(flightMode = "Takeoff")
                        DroneAction.RTL -> state.telemetry.copy(flightMode = "RTL")
                    }
                } else {
                    state.telemetry
                }
                state.copy(
                    telemetry = telemetry,
                    actionMessage = result.message,
                    lastError = if (result.success) null else result.message
                )
            }
        }
    }

    private fun applyStatus(event: TelemetryEvent.Status) {
        _uiState.update {
            it.copy(
                connectionStatus = event.status,
                statusMessage = event.message ?: event.status.label,
                isLoading = event.status == TelemetryConnectionStatus.CONNECTING ||
                    event.status == TelemetryConnectionStatus.RECONNECTING,
                lastError = null
            )
        }
    }

    // Maps raw MAVLink command IDs to readable labels for the ACK message display.
    private fun Int.toCommandLabel(): String = when (this) {
        400 -> "Arm/Disarm"
        22 -> "Takeoff"
        20 -> "RTL"
        else -> "Command $this"
    }

    private fun createInitialState(cachedTelemetry: TelemetryData?): TelemetryUiState {
        return TelemetryUiState(
            telemetry = cachedTelemetry ?: TelemetryUiState().telemetry,
            actionMessage = if (cachedTelemetry != null) "Loaded last known telemetry" else null
        )
    }
}
