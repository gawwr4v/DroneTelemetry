package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.model.TelemetryEndpoint
import com.gourav.dronetelemetry.domain.repository.TelemetryRepository
import javax.inject.Inject

class ObserveTelemetryUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {
    operator fun invoke(endpoint: TelemetryEndpoint) = repository.stream(endpoint)
}
