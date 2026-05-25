package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.model.DroneCommand
import com.gourav.dronetelemetry.domain.repository.TelemetryRepository
import javax.inject.Inject

class SendDroneCommandUseCase @Inject constructor(
    private val repository: TelemetryRepository
) {
    suspend operator fun invoke(command: DroneCommand) = repository.sendCommand(command)
}
