package com.gourav.dronetelemetry.domain.model

enum class DroneCommand(val label: String) {
    ARM("Arm"),
    DISARM("Disarm"),
    TAKEOFF("Takeoff"),
    RTL("RTL")
}
