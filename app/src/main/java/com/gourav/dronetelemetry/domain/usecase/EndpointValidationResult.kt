package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.model.TelemetryEndpoint

data class EndpointValidationResult(
    val endpoint: TelemetryEndpoint? = null,
    val hostError: String? = null,
    val portError: String? = null
) {
    val isValid: Boolean = endpoint != null
}
