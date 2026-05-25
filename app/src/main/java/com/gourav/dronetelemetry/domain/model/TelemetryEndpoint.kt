package com.gourav.dronetelemetry.domain.model

data class TelemetryEndpoint(
    val protocol: EndpointProtocol,
    val host: String,
    val port: Int
) {
    val displayValue: String
        get() = "${protocol.name.lowercase()}://$host:$port"
}
