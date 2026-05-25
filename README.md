# DroneTelemetry

DroneTelemetry is a native Android telemetry dashboard built with Kotlin and Jetpack Compose. It connects to UDP or TCP MAVLink streams, shows live aircraft state, and keeps the UI stable during dropped packets, timeouts, and reconnects.

[![Build Status](https://img.shields.io/badge/Build-Passing-success?style=flat-square&logo=android)](https://github.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-1.7-green?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/DI-Hilt-blue?style=flat-square)](https://developer.android.com/training/dependency-injection/hilt-android)
[![MAVLink](https://img.shields.io/badge/MAVLink-1.2.15-orange?style=flat-square)](https://github.com/divpundir/mavlink-kotlin)

## Tech Stack

| Component | Library / Tool |
| :--- | :--- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Dependency Injection | Hilt |
| Async | Coroutines, Flow |
| MAVLink | mavlink-kotlin 1.2.15 (`com.divpundir.mavlink`) |
| Min SDK | 29 (Android 10) |

### Why mavlink-kotlin lib?
`io.dronefleet.mavlink` is abandoned, the author has stated publicly they cannot maintain it. `MAVSDK` uses a gRPC companion process and is heavier than this scope requires. `mavlink-kotlin` is actively maintained, Kotlin-first, uses code generation instead of reflection, and integrates directly with Kotlin Flow via its coroutines adapter.

## Features

* Connection form for protocol (UDP, TCP), host, and port with inline validation.
* UDP server mode: binds to port 14550, receives telemetry from any drone that sends to our IP.
* TCP client mode: connects outward to a remote MAVLink TCP server.
* Live dashboard showing latitude, longitude, altitude, battery, flight mode, armed state, and connection status.
* Automatic reconnection with CONNECTING, CONNECTED, RECONNECTING, and ERROR states.
* Real MAVLink `COMMAND_LONG` frames for Arm, Disarm, Takeoff, and RTL via `sendV2`.
* Real `COMMAND_ACK` parsing. The UI shows the drone's actual response (Accepted, Denied, etc.).
* Joystick-style flick gesture pad for drone commands with a 56dp drag threshold to prevent accidental taps.
* Last known telemetry cache so the dashboard does not wipe between reconnects.
* Dark mode through the Material 3 theme.

## Architecture

The project uses a package-based Clean Architecture shape. Dependencies flow strictly inward: `presentation` depends on `domain`, `data` depends on `domain`, nothing depends on `presentation` or `data`.

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer                                     │
│                                                         │
│  DroneTelemetryScreen (Compose)                         │
│       │ collects StateFlow<TelemetryUiState>            │
│  TelemetryViewModel                                     │
│       │ invokes use cases                               │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│  Domain Layer  (no Android dependencies)                │
│                                                         │
│  ObserveTelemetryUseCase                                │
│  SendDroneCommandUseCase                                │
│  ValidateEndpointUseCase                                │
│  GetCachedTelemetryUseCase                              │
│                                                         │
│  TelemetryRepository  ◄── interface only                │
│                                                         │
│  Models: TelemetryData, TelemetryUpdate,                │
│          TelemetryEvent, TelemetryEndpoint,             │
│          DroneCommand, CommandAck, CommandAckResult     │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│  Data Layer                                             │
│                                                         │
│  SocketTelemetryRepository                              │
│   ├── UdpServerMavConnection  (mavlink-kotlin)          │
│   ├── TcpClientMavConnection  (mavlink-kotlin)          │
│   ├── CoroutineMavConnection.mavFrame  Flow             │
│   │   dispatches: Heartbeat, GlobalPositionInt,         │
│   │               SysStatus, CommandAck, CommandLong    │
│   └── TelemetryLocalStore  (SharedPreferences + CSV)    │
│                                                         │
│  DI: TelemetryModule (@Binds interface to impl)         │
└─────────────────────────────────────────────────────────┘
```

### Data Flow (telemetry, one direction)

```
Drone / Simulator
      │  UDP packet (MAVLink v1 or v2)
      ▼
UdpServerMavConnection
      │  CoroutineMavConnection.mavFrame Flow
      ▼
SocketTelemetryRepository
      │  when (frame.message) is Heartbeat, GlobalPositionInt, SysStatus, CommandAck
      │  maps to TelemetryUpdate.applyTo(lastTelemetry)
      │  emits TelemetryEvent.Telemetry or TelemetryEvent.CommandAck
      ▼
ObserveTelemetryUseCase  (thin delegate)
      ▼
TelemetryViewModel
      │  _uiState.update { it.copy(telemetry = event.data) }
      ▼
DroneTelemetryScreen (Compose, collectAsStateWithLifecycle)
      ▼
TelemetryDashboard tiles update with new values
```

### Key Design Decisions

| Decision | Reason |
| :--- | :--- |
| Retry loop in repository, not ViewModel | Transport behavior belongs in the data layer. The ViewModel only sees state events. |
| `flowOn(Dispatchers.IO)` on the stream Flow | Socket reads must never run on the main thread. |
| `TelemetryUpdate` partial merge pattern | Each parsed message only carries the fields it knows about. `applyTo()` merges into the running snapshot, so unrelated fields are never wiped. |
| `@Singleton` repository | The socket and retry state must be shared across all callers. |
| Optimistic ARM/DISARM state | Gives instant visual feedback. The real `COMMAND_ACK` updates `actionMessage` with the drone's actual response when it arrives. |

## Setup

1. Open the project in Android Studio Hedgehog or later.
2. Run on a physical device or emulator with API 29+.
3. Start the Python simulator on a machine on the same WiFi network (see simulator script in README).
4. In the app, select **UDP**, enter the phone IP as host in the simulator (`PHONE_IP`), and connect on port `14550`.

### Example endpoint (UDP listen mode)

```
Protocol: UDP
Host:     0.0.0.0
Port:     14550
```

For TCP, enter the MAVLink server host and port directly.

### Python Simulator

An interactive, bidirectional Python simulator is provided in the project root: [telemetry_sim.py](file:///C:/Users/goura/AndroidStudioProjects/DroneTelemetry/telemetry_sim.py).

To use it:
1. Replace `PHONE_IP` in `telemetry_sim.py` with your Android device's actual IP address.
2. Install `pymavlink` on your testing machine:
   ```bash
   pip install pymavlink
   ```
3. Run the script:
   ```bash
   python telemetry_sim.py
   ```
The script broadcasts mock telemetry (Heartbeat, GPS, Battery) at 1Hz and simultaneously listens for incoming MAVLink `COMMAND_LONG` packets from the app, updating its state (armed/disarmed/modes) and returning `COMMAND_ACK` acknowledgements.


## Limitations & Future Scope

While the core functionality of this GCS (Ground Control Station) telemetry viewer is fully implemented and validated, there is significant potential for expansion:

### 1. Live Maps & Spatial Tracking
* **Feature:** Integration of OpenStreetMap (via `osmdroid`) to show the drone's position marker in real-time.
* **Details:** The domain model already caches and parses `latitude` and `longitude` fields from the `GlobalPositionInt` frame. The next step is adding a map fragment, plotting breadcrumbs of the drone's flight path, and showing telemetry info boxes when clicking the aircraft marker.

### 2. Room Database for Flight Logs & Analytics
* **Feature:** Upgrade from simple CSV logging to a structured SQLite database using Jetpack Room.
* **Details:** This would allow the app to store, filter, and review historical flight logs. We can add a "Flight History" dashboard with graphs (using `MPAndroidChart`) to plot altitude, speed, and battery drainage over time.

### 3. Mission Planning & Waypoint Navigation
* **Feature:** Uploading autonomous flight plans to the drone.
* **Details:** By adding MAVLink mission protocol support (using messages like `MISSION_ITEM_INT`, `MISSION_COUNT`, and `MISSION_ACK`), the user could draw waypoints on the map and upload them. Clicking a map location could also trigger "Go-To" instructions (`MAV_CMD_DO_REPOSITION`).

### 4. Connection & Command UX Robustness
* **Feature:** Command timeout safeguards.
* **Details:** Currently, commands sent to the drone update the UI message state optimistically. If a packet is lost and no `COMMAND_ACK` returns, the status remains indefinitely. We can add a 3-second coroutine timeout that flags `"No Response from Drone / Command Timeout"`.
