package com.gourav.dronetelemetry.data.local

import android.content.Context
import com.gourav.dronetelemetry.domain.model.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryLocalStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("telemetry_cache", Context.MODE_PRIVATE)
    private val logFile = File(appContext.filesDir, "telemetry_log.csv")
    private var lastLoggedLine: String? = null

    fun save(telemetry: TelemetryData) {
        preferences.edit()
            .putNullableDouble(KEY_LATITUDE, telemetry.latitude)
            .putNullableDouble(KEY_LONGITUDE, telemetry.longitude)
            .putNullableDouble(KEY_ALTITUDE, telemetry.altitudeMeters)
            .putInt(KEY_BATTERY, telemetry.batteryPercent ?: UNKNOWN_INT)
            .putString(KEY_FLIGHT_MODE, telemetry.flightMode)
            .putBoolean(KEY_ARMED, telemetry.armed)
            .putInt(KEY_TARGET_SYSTEM, telemetry.targetSystemId)
            .putInt(KEY_TARGET_COMPONENT, telemetry.targetComponentId)
            .apply()
        appendLog(telemetry)
    }

    fun read(): TelemetryData? {
        if (!preferences.contains(KEY_FLIGHT_MODE)) return null
        val battery = preferences.getInt(KEY_BATTERY, UNKNOWN_INT).takeIf { it != UNKNOWN_INT }
        return TelemetryData(
            latitude = preferences.getNullableDouble(KEY_LATITUDE),
            longitude = preferences.getNullableDouble(KEY_LONGITUDE),
            altitudeMeters = preferences.getNullableDouble(KEY_ALTITUDE),
            batteryPercent = battery,
            flightMode = preferences.getString(KEY_FLIGHT_MODE, "Unknown") ?: "Unknown",
            armed = preferences.getBoolean(KEY_ARMED, false),
            targetSystemId = preferences.getInt(KEY_TARGET_SYSTEM, 1),
            targetComponentId = preferences.getInt(KEY_TARGET_COMPONENT, 1)
        )
    }

    @Synchronized
    private fun appendLog(telemetry: TelemetryData) {
        val latitude = telemetry.latitude ?: return
        val longitude = telemetry.longitude ?: return
        val line = listOf(
            System.currentTimeMillis().toString(),
            latitude.toString(),
            longitude.toString(),
            (telemetry.altitudeMeters ?: "").toString(),
            (telemetry.batteryPercent ?: "").toString(),
            telemetry.flightMode,
            telemetry.armed.toString()
        ).joinToString(",")
        if (line == lastLoggedLine) return
        if (!logFile.exists()) {
            logFile.writeText("timestamp,latitude,longitude,altitude,battery,flightMode,armed\n")
        }
        logFile.appendText("$line\n")
        lastLoggedLine = line
    }

    private fun android.content.SharedPreferences.Editor.putNullableDouble(
        key: String,
        value: Double?
    ): android.content.SharedPreferences.Editor {
        return if (value == null) remove(key) else putLong(key, value.toRawBits())
    }

    private fun android.content.SharedPreferences.getNullableDouble(key: String): Double? {
        return if (contains(key)) Double.fromBits(getLong(key, 0L)) else null
    }

    private companion object {
        const val UNKNOWN_INT = -1
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_ALTITUDE = "altitude"
        const val KEY_BATTERY = "battery"
        const val KEY_FLIGHT_MODE = "flight_mode"
        const val KEY_ARMED = "armed"
        const val KEY_TARGET_SYSTEM = "target_system"
        const val KEY_TARGET_COMPONENT = "target_component"
    }
}
