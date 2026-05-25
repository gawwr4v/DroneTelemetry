package com.gourav.dronetelemetry.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import com.gourav.dronetelemetry.domain.model.TelemetryConnectionStatus
import com.gourav.dronetelemetry.domain.model.TelemetryData
import com.gourav.dronetelemetry.presentation.DroneAction
import com.gourav.dronetelemetry.presentation.TelemetryUiState
import com.gourav.dronetelemetry.ui.theme.DroneTelemetryTheme

@Composable
fun DroneTelemetryScreen(
    uiState: TelemetryUiState,
    onProtocolChange: (EndpointProtocol) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onAction: (DroneAction) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(uiState)
            ConnectionPanel(
                uiState = uiState,
                onProtocolChange = onProtocolChange,
                onHostChange = onHostChange,
                onPortChange = onPortChange,
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )
            TelemetryDashboard(uiState.telemetry)
            ActionPanel(onAction = onAction, actionMessage = uiState.actionMessage)
        }
    }
}

@Composable
private fun Header(uiState: TelemetryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "DroneTelemetry",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                status = uiState.connectionStatus,
                label = uiState.statusMessage,
                lowBattery = (uiState.telemetry.batteryPercent ?: 100) < 20
            )
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        uiState.lastError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatusIndicator(
    status: TelemetryConnectionStatus,
    label: String,
    lowBattery: Boolean
) {
    val color = when {
        lowBattery -> LowBatteryOrange
        status == TelemetryConnectionStatus.CONNECTED -> ConnectedGreen
        status == TelemetryConnectionStatus.CONNECTING -> WarningAmber
        status == TelemetryConnectionStatus.RECONNECTING -> WarningAmber
        else -> DisconnectedRed
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConnectionPanel(
    uiState: TelemetryUiState,
    onProtocolChange: (EndpointProtocol) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EndpointProtocol.entries.forEach { protocol ->
                    FilterChip(
                        selected = uiState.protocol == protocol,
                        onClick = { onProtocolChange(protocol) },
                        label = { Text(protocol.label) }
                    )
                }
            }
            OutlinedTextField(
                value = uiState.host,
                onValueChange = onHostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("IP Address") },
                singleLine = true,
                isError = uiState.hostError != null,
                supportingText = { uiState.hostError?.let { Text(it) } }
            )
            OutlinedTextField(
                value = uiState.port,
                onValueChange = onPortChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.portError != null,
                supportingText = { uiState.portError?.let { Text(it) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onConnect,
                    enabled = !uiState.isLoading,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Connect")
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun TelemetryDashboard(telemetry: TelemetryData) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Live Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        listOf(
            "Latitude" to telemetry.latitude.formatCoordinate(),
            "Longitude" to telemetry.longitude.formatCoordinate(),
            "Altitude" to telemetry.altitudeMeters.formatMeters(),
            "Battery" to telemetry.batteryPercent.formatPercent(),
            "Flight Mode" to telemetry.flightMode,
            "Armed" to if (telemetry.armed) "Armed" else "Disarmed"
        ).chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value) ->
                    TelemetryTile(
                        label = label,
                        value = value,
                        warning = label == "Battery" && (telemetry.batteryPercent ?: 100) < 20,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    warning: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (warning) LowBatteryOrange else MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = modifier
            .height(92.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionPanel(
    onAction: (DroneAction) -> Unit,
    actionMessage: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Drone Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        DirectionActionPad(onAction = onAction)
        actionMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DirectionActionPad(onAction: (DroneAction) -> Unit) {
    val triggerDistance = with(LocalDensity.current) { 56.dp.toPx() }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val selectedAction = dragOffset.toDroneAction(triggerDistance)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(204.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .pointerInput(triggerDistance) {
                    detectDragGestures(
                        onDragStart = { dragOffset = Offset.Zero },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        },
                        onDragEnd = {
                            dragOffset.toDroneAction(triggerDistance)?.let(onAction)
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = { dragOffset = Offset.Zero }
                    )
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DirectionHint(
                    label = "Takeoff",
                    color = TakeoffBlue,
                    selected = selectedAction == DroneAction.TAKEOFF,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                DirectionHint(
                    label = "Disarm",
                    color = DisconnectedRed,
                    selected = selectedAction == DroneAction.DISARM,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                DirectionHint(
                    label = "Arm",
                    color = ConnectedGreen,
                    selected = selectedAction == DroneAction.ARM,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                DirectionHint(
                    label = "RTL",
                    color = LowBatteryOrange,
                    selected = selectedAction == DroneAction.RTL,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                CommandCenter()
            }
        }
    }
}

@Composable
private fun DirectionHint(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.padding(10.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.22f) else Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (selected) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommandCenter() {
    Surface(
        modifier = Modifier.size(74.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Command",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun Offset.toDroneAction(triggerDistance: Float): DroneAction? {
    if (getDistance() < triggerDistance) return null
    return if (abs(x) > abs(y)) {
        if (x > 0) DroneAction.RTL else DroneAction.ARM
    } else {
        if (y > 0) DroneAction.DISARM else DroneAction.TAKEOFF
    }
}

private fun Double?.formatCoordinate(): String {
    return this?.let { "%.6f".format(it) } ?: "--"
}

private fun Double?.formatMeters(): String {
    return this?.let { "%.1f m".format(it) } ?: "--"
}

private fun Int?.formatPercent(): String {
    return this?.let { "$it%" } ?: "--"
}

private val ConnectedGreen = Color(0xFF2E7D32)
private val DisconnectedRed = Color(0xFFC62828)
private val WarningAmber = Color(0xFFFFA000)
private val LowBatteryOrange = Color(0xFFF57C00)
private val TakeoffBlue = Color(0xFF1976D2)

@Preview(showBackground = true)
@Composable
private fun DroneTelemetryScreenPreview() {
    DroneTelemetryTheme {
        DroneTelemetryScreen(
            uiState = previewState(),
            onProtocolChange = {},
            onHostChange = {},
            onPortChange = {},
            onConnect = {},
            onDisconnect = {},
            onAction = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DroneTelemetryScreenDarkPreview() {
    DroneTelemetryTheme {
        DroneTelemetryScreen(
            uiState = previewState().copy(connectionStatus = TelemetryConnectionStatus.RECONNECTING),
            onProtocolChange = {},
            onHostChange = {},
            onPortChange = {},
            onConnect = {},
            onDisconnect = {},
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionPanelPreview() {
    DroneTelemetryTheme {
        ConnectionPanel(
            uiState = previewState(),
            onProtocolChange = {},
            onHostChange = {},
            onPortChange = {},
            onConnect = {},
            onDisconnect = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TelemetryDashboardPreview() {
    DroneTelemetryTheme {
        TelemetryDashboard(previewState().telemetry)
    }
}

@Preview(showBackground = true)
@Composable
private fun TelemetryTilePreview() {
    DroneTelemetryTheme {
        TelemetryTile(label = "Battery", value = "18%", warning = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionPanelPreview() {
    DroneTelemetryTheme {
        ActionPanel(onAction = {}, actionMessage = "Takeoff command sent over UDP")
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusIndicatorPreview() {
    DroneTelemetryTheme {
        StatusIndicator(
            status = TelemetryConnectionStatus.CONNECTED,
            label = "Connected",
            lowBattery = false
        )
    }
}

private fun previewState() = TelemetryUiState(
    connectionStatus = TelemetryConnectionStatus.CONNECTED,
    statusMessage = "udp://0.0.0.0:14550",
    telemetry = TelemetryData(
        latitude = 12.971598,
        longitude = 77.594566,
        altitudeMeters = 42.6,
        batteryPercent = 74,
        flightMode = "Guided",
        armed = true
    )
)
