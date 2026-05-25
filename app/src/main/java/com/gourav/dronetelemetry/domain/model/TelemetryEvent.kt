package com.gourav.dronetelemetry.domain.model

sealed interface TelemetryEvent {
    data class Status(val status: TelemetryConnectionStatus, val message: String? = null) : TelemetryEvent
    data class Telemetry(val data: TelemetryData) : TelemetryEvent
    data class Error(val message: String) : TelemetryEvent
    // Drone acknowledged a command we sent. Result tells us accepted, denied, etc.
    data class CommandAck(val ack: com.gourav.dronetelemetry.domain.model.CommandAck) : TelemetryEvent
}

