package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.repository.TelemetryRepository
import javax.inject.Inject

class GetCachedTelemetryUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {
    operator fun invoke() = repository.cachedTelemetry()
}
