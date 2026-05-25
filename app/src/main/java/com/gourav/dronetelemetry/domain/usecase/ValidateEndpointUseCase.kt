package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import com.gourav.dronetelemetry.domain.model.TelemetryEndpoint
import javax.inject.Inject

class ValidateEndpointUseCase @Inject constructor() {
    operator fun invoke(
        protocol: EndpointProtocol,
        hostInput: String,
        portInput: String
    ): EndpointValidationResult {
        val host = hostInput.trim()
        val port = portInput.trim().toIntOrNull()
        val hostError = when {
            host.isBlank() -> "Host is required"
            !isValidHost(host) -> "Enter a valid IPv4 address or host"
            else -> null
        }
        val portError = when {
            port == null -> "Port must be a number"
            port !in 1..65535 -> "Port must be 1 to 65535"
            else -> null
        }

        return if (hostError == null && portError == null && port != null) {
            EndpointValidationResult(endpoint = TelemetryEndpoint(protocol, host, port))
        } else {
            EndpointValidationResult(hostError = hostError, portError = portError)
        }
    }

    private fun isValidHost(host: String): Boolean {
        if (host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            return host.split(".").all { it.toInt() in 0..255 }
        }
        return host.matches(Regex("""[a-zA-Z0-9][a-zA-Z0-9.-]{0,251}[a-zA-Z0-9]"""))
    }
}
