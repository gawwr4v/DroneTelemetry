package com.gourav.dronetelemetry.domain.model

enum class CommandAckResult(val label: String) {
    ACCEPTED("Accepted"),
    TEMPORARILY_REJECTED("Temporarily Rejected"),
    DENIED("Denied"),
    UNSUPPORTED("Unsupported"),
    FAILED("Failed"),
    IN_PROGRESS("In Progress"),
    UNKNOWN("Unknown");

    companion object {
        fun fromCode(code: Int): CommandAckResult = when (code) {
            0 -> ACCEPTED
            1 -> TEMPORARILY_REJECTED
            2 -> DENIED
            3 -> UNSUPPORTED
            4 -> FAILED
            5 -> IN_PROGRESS
            else -> UNKNOWN
        }
    }
}
