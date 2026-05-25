package com.gourav.dronetelemetry.domain.model

data class TelemetryUpdate(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val batteryPercent: Int? = null,
    val flightMode: String? = null,
    val armed: Boolean? = null,
    val targetSystemId: Int? = null,
    val targetComponentId: Int? = null
) {
    fun applyTo(current: TelemetryData): TelemetryData {
        return current.copy(
            latitude = latitude ?: current.latitude,
            longitude = longitude ?: current.longitude,
            altitudeMeters = altitudeMeters ?: current.altitudeMeters,
            batteryPercent = batteryPercent ?: current.batteryPercent,
            flightMode = flightMode ?: current.flightMode,
            armed = armed ?: current.armed,
            targetSystemId = targetSystemId ?: current.targetSystemId,
            targetComponentId = targetComponentId ?: current.targetComponentId
        )
    }
}
