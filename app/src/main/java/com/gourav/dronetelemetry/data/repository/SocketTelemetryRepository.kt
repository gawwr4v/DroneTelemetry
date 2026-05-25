package com.gourav.dronetelemetry.data.repository

import com.divpundir.mavlink.adapters.coroutines.CoroutinesMavConnection
import com.divpundir.mavlink.adapters.coroutines.asCoroutine
import com.divpundir.mavlink.api.AbstractMavDialect
import com.divpundir.mavlink.api.MavEnumValue
import com.divpundir.mavlink.connection.tcp.TcpClientMavConnection
import com.divpundir.mavlink.connection.udp.UdpServerMavConnection
import com.divpundir.mavlink.definitions.common.CommandAck as MavCommandAck
import com.divpundir.mavlink.definitions.common.CommandLong
import com.divpundir.mavlink.definitions.common.CommonDialect
import com.divpundir.mavlink.definitions.standard.StandardDialect
import com.divpundir.mavlink.definitions.standard.GlobalPositionInt
import com.divpundir.mavlink.definitions.minimal.Heartbeat
import com.divpundir.mavlink.definitions.common.MavCmd
import com.divpundir.mavlink.definitions.common.SysStatus
import com.gourav.dronetelemetry.data.local.TelemetryLocalStore
import com.gourav.dronetelemetry.domain.model.CommandAck
import com.gourav.dronetelemetry.domain.model.CommandAckResult
import com.gourav.dronetelemetry.domain.model.CommandResult
import com.gourav.dronetelemetry.domain.model.DroneCommand
import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import com.gourav.dronetelemetry.domain.model.TelemetryConnectionStatus
import com.gourav.dronetelemetry.domain.model.TelemetryData
import com.gourav.dronetelemetry.domain.model.TelemetryEndpoint
import com.gourav.dronetelemetry.domain.model.TelemetryEvent
import com.gourav.dronetelemetry.domain.model.TelemetryUpdate
import com.gourav.dronetelemetry.domain.repository.TelemetryRepository
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class SocketTelemetryRepository @Inject constructor(
    private val localStore: TelemetryLocalStore
) : TelemetryRepository {

    // Holds the active connection so sendCommand can reach it outside the stream coroutine.
    private val activeConnection = AtomicReference<CoroutinesMavConnection?>(null)
    private val latestTelemetry = AtomicReference(TelemetryData())

    override fun stream(endpoint: TelemetryEndpoint): Flow<TelemetryEvent> = flow {
        var retryCount = 0
        var lastTelemetry = TelemetryData()

        while (currentCoroutineContext().isActive) {
            emit(
                TelemetryEvent.Status(
                    status = if (retryCount == 0) TelemetryConnectionStatus.CONNECTING
                             else TelemetryConnectionStatus.RECONNECTING,
                    message = endpoint.displayValue
                )
            )

            // asCoroutine wraps the raw MavConnection in a coroutines adapter.
            // We hold a reference so sendCommand can use it without a separate socket.
            val conn = endpoint.toMavConnection().asCoroutine(Dispatchers.IO)
            activeConnection.set(conn)

            try {
                // connect() launches the internal frame-reading loop on this scope.
                // coroutineScope ties the loop lifetime to the current coroutine,
                // so cancellation propagates cleanly and we avoid detached scopes.
                coroutineScope {
                    conn.connect(this)
                    emit(TelemetryEvent.Status(TelemetryConnectionStatus.CONNECTED, endpoint.displayValue))

                    conn.mavFrame.collect { frame ->
                        val sysId = frame.systemId.toInt()
                        val compId = frame.componentId.toInt()

                        // Dispatch on message type. Unsupported types fall to else and
                        // the stream continues without interruption.
                        val update: TelemetryUpdate? = when (val msg = frame.message) {
                            is Heartbeat -> msg.toUpdate(sysId, compId)
                            is GlobalPositionInt -> msg.toUpdate()
                            is SysStatus -> msg.toUpdate()
                            is MavCommandAck -> {
                                emit(TelemetryEvent.CommandAck(msg.toDomainAck()))
                                null
                            }
                            else -> null
                        }

                        if (update != null) {
                            lastTelemetry = update.applyTo(lastTelemetry)
                            latestTelemetry.set(lastTelemetry)
                            localStore.save(lastTelemetry)
                            emit(TelemetryEvent.Telemetry(lastTelemetry))
                        }
                    }
                }
            } catch (e: IOException) {
                emit(TelemetryEvent.Error(e.message ?: "Stream interrupted. Retrying..."))
            } finally {
                activeConnection.set(null)
                // we release socket here when flow cancels, so we dont lock port and cause address already in use error on reconnect. we wrap in noncancellable so it runs even if connection gets cancelled.
                withContext(NonCancellable) {
                    try {
                        conn.close()
                    } catch (closeEx: IOException) {
                        // ignore close error on cleanup
                    }
                }
            }

            retryCount++
            delay(RETRY_DELAY_MS)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun sendCommand(command: DroneCommand): CommandResult = withContext(Dispatchers.IO) {
        val conn = activeConnection.get()
            ?: return@withContext CommandResult(false, "Connect before sending ${command.label}")

        val telemetry = latestTelemetry.get()
        val msg = CommandLong(
            targetSystem = telemetry.targetSystemId.toUByte(),
            targetComponent = telemetry.targetComponentId.toUByte(),
            command = command.toMavCmd(),
            confirmation = 0u,
            param1 = if (command == DroneCommand.ARM) 1f else 0f,
            param2 = 0f,
            param3 = 0f,
            param4 = 0f,
            param5 = 0f,
            param6 = 0f,
            param7 = if (command == DroneCommand.TAKEOFF) TAKEOFF_ALT_METERS else 0f
        )

        return@withContext try {
            conn.sendUnsignedV2(APP_SYSTEM_ID.toUByte(), APP_COMPONENT_ID.toUByte(), msg)
            CommandResult(true, "${command.label} command sent")
        } catch (e: IOException) {
            CommandResult(false, e.message ?: "Failed to send ${command.label}")
        }
    }

    override fun cachedTelemetry(): TelemetryData? = localStore.read()

    // udp binds to port and tcp connects. we pass combined dialect so it registers heartbeat, globalpositionint, sysstatus, and commandack messages correctly, standard dialect lacks common messages.
    private fun TelemetryEndpoint.toMavConnection() = when (protocol) {
        EndpointProtocol.UDP -> UdpServerMavConnection(port, CombinedDialect)
        EndpointProtocol.TCP -> TcpClientMavConnection(host, port, CombinedDialect)
    }

    // we combine standard and common dialects here, standard only has globalpositionint and minimal, common has sysstatus and commandack. combineddialect merges both so we receive all messages.
    private object CombinedDialect : AbstractMavDialect(
        setOf(StandardDialect, CommonDialect),
        emptyMap()
    )

    // baseMode is a bitmask. Bit 7 (value 128) is MAV_MODE_FLAG_SAFETY_ARMED.
    private fun Heartbeat.toUpdate(sysId: Int, compId: Int) = TelemetryUpdate(
        flightMode = customMode.toInt().toFlightModeLabel(),
        armed = (baseMode.value.toInt() and MAV_MODE_FLAG_SAFETY_ARMED) != 0,
        targetSystemId = sysId,
        targetComponentId = compId
    )

    // alt is MSL altitude in millimeters per MAVLink spec (field at byte offset 12).
    private fun GlobalPositionInt.toUpdate() = TelemetryUpdate(
        latitude = lat / 10_000_000.0,
        longitude = lon / 10_000_000.0,
        altitudeMeters = alt / 1000.0
    )

    // batteryRemaining is int8. Value -1 means the hardware cannot report it.
    private fun SysStatus.toUpdate(): TelemetryUpdate? {
        val battery = batteryRemaining.toInt()
        return if (battery in 0..100) TelemetryUpdate(batteryPercent = battery) else null
    }

    // MavEnumValue.value is the raw MAVLink integer (UInt). toInt() is safe for codes 0-5.
    private fun MavCommandAck.toDomainAck() = CommandAck(
        commandId = command.value.toInt(),
        result = CommandAckResult.fromCode(result.value.toInt())
    )

    private fun DroneCommand.toMavCmd(): MavEnumValue<MavCmd> = when (this) {
        DroneCommand.ARM, DroneCommand.DISARM -> MavEnumValue.of(MavCmd.COMPONENT_ARM_DISARM)
        DroneCommand.TAKEOFF -> MavEnumValue.of(MavCmd.NAV_TAKEOFF)
        DroneCommand.RTL -> MavEnumValue.of(MavCmd.NAV_RETURN_TO_LAUNCH)
    }

    private fun Int.toFlightModeLabel(): String = ardupilotCopterModes[this] ?: "Mode $this"

    private companion object {
        const val RETRY_DELAY_MS = 1500L
        // Non-const: Kotlin IR const-evaluator has no handler for toUByte(Int),
        // so using const here triggers an internal compiler crash.
        val APP_SYSTEM_ID = 255
        val APP_COMPONENT_ID = 190
        const val MAV_MODE_FLAG_SAFETY_ARMED = 128
        const val TAKEOFF_ALT_METERS = 10f

        // ArduPilot Copter custom mode numbers. Source: ArduPilot firmware enum AP_Vehicle.
        val ardupilotCopterModes = mapOf(
            0 to "Stabilize",
            2 to "Alt Hold",
            3 to "Auto",
            4 to "Guided",
            5 to "Loiter",
            6 to "RTL",
            9 to "Land",
            11 to "Drift",
            13 to "Sport",
            16 to "Pos Hold",
            17 to "Brake",
            20 to "Guided No GPS",
            21 to "Smart RTL"
        )
    }
}
