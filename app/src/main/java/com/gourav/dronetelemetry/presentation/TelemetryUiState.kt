package com.gourav.dronetelemetry.presentation

import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import com.gourav.dronetelemetry.domain.model.TelemetryConnectionStatus
import com.gourav.dronetelemetry.domain.model.TelemetryData

data class TelemetryUiState(
    val protocol: EndpointProtocol = EndpointProtocol.UDP,
    val host: String = "0.0.0.0",
    val port: String = "14550",
    val hostError: String? = null,
    val portError: String? = null,
    val connectionStatus: TelemetryConnectionStatus = TelemetryConnectionStatus.DISCONNECTED,
    val statusMessage: String = "Disconnected",
    val isLoading: Boolean = false,
    val telemetry: TelemetryData = TelemetryData(),
    val lastError: String? = null,
    val actionMessage: String? = null
)
