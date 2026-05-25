package com.gourav.dronetelemetry.domain.model

data class CommandAck(
    val commandId: Int,
    val result: CommandAckResult
)
