package com.gourav.dronetelemetry.domain.model

data class TelemetryData(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val batteryPercent: Int? = null,
    val flightMode: String = "Unknown",
    val armed: Boolean = false,
    val targetSystemId: Int = 1,
    val targetComponentId: Int = 1
)
