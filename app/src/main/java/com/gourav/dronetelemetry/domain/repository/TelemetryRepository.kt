package com.gourav.dronetelemetry.domain.repository

import com.gourav.dronetelemetry.domain.model.CommandResult
import com.gourav.dronetelemetry.domain.model.DroneCommand
import com.gourav.dronetelemetry.domain.model.TelemetryData
import com.gourav.dronetelemetry.domain.model.TelemetryEndpoint
import com.gourav.dronetelemetry.domain.model.TelemetryEvent
import kotlinx.coroutines.flow.Flow

interface TelemetryRepository {
    fun stream(endpoint: TelemetryEndpoint): Flow<TelemetryEvent>
    suspend fun sendCommand(command: DroneCommand): CommandResult
    fun cachedTelemetry(): TelemetryData?
}
