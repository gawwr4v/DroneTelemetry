package com.gourav.dronetelemetry.presentation

import com.gourav.dronetelemetry.domain.model.DroneCommand

enum class DroneAction(val label: String) {
    ARM("Arm"),
    DISARM("Disarm"),
    TAKEOFF("Takeoff"),
    RTL("RTL"); // Return to launch

    fun toCommand(): DroneCommand {
        return when (this) {
            ARM -> DroneCommand.ARM
            DISARM -> DroneCommand.DISARM
            TAKEOFF -> DroneCommand.TAKEOFF
            RTL -> DroneCommand.RTL
        }
    }
}
