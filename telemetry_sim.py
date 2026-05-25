import time
import os

# Force MAVLink v1 (Highly compatible with custom/library parsers)
os.environ['MAVLINK10'] = '1'

from pymavlink import mavutil

PHONE_IP = "192.168.X.X"  # Replace with your Android device's actual IP, e.g., "192.168.1.100"
PORT = 14550

print(f"Broadcasting telemetry & listening for commands on {PHONE_IP}:{PORT}...")

# Set up connection: udpout sends packets and allows receiving replies on the source socket
master = mavutil.mavlink_connection(f'udpout:{PHONE_IP}:{PORT}', source_system=1)

# FORCE BIND: Send an initial heartbeat packet immediately.
# This forces the Windows OS to assign an ephemeral source port and bind the socket,
# preventing WSAEINVAL (WinError 10022) on the subsequent recv_match calls.
master.mav.heartbeat_send(
    mavutil.mavlink.MAV_TYPE_QUADROTOR,
    mavutil.mavlink.MAV_AUTOPILOT_ARDUPILOTMEGA,
    0,
    0,
    mavutil.mavlink.MAV_STATE_STANDBY
)

# Drone simulation state
armed = False
flight_mode = 0  # Stabilize mode
altitude = 15000  # 15.0m
battery = 98

boot_time = time.time()
last_send_time = 0

while True:
    current_time = time.time()
    time_boot_ms = int((current_time - boot_time) * 1000)

    # 1. Check for incoming messages (commands sent from the phone) safely
    msg = None
    try:
        msg = master.recv_match(blocking=False)
    except OSError as e:
        # Gracefully catch WinError 10022 (unbound) or WinError 10054 (connection reset/port closed)
        pass

    if msg is not None:
        msg_type = msg.get_type()
        if msg_type == 'COMMAND_LONG':
            target_sys = getattr(msg, 'target_system', 0)
            if target_sys in (0, 1):
                cmd_id = msg.command
                print(f"-> Received COMMAND_LONG (ID: {cmd_id}) from phone.")

                # Default ACK result: ACCEPTED (0)
                ack_result = mavutil.mavlink.MAV_RESULT_ACCEPTED

                # Handle commands to update local drone simulation state
                if cmd_id == mavutil.mavlink.MAV_CMD_COMPONENT_ARM_DISARM:
                    param1 = getattr(msg, 'param1', 0.0)
                    if param1 == 1.0:
                        armed = True
                        print("   Drone State: ARMED")
                    elif param1 == 0.0:
                        armed = False
                        print("   Drone State: DISARMED")
                elif cmd_id == mavutil.mavlink.MAV_CMD_NAV_TAKEOFF:
                    if armed:
                        flight_mode = 4  # Guided mode
                        altitude = 25000  # Ascend to takeoff altitude
                        print("   Drone State: TAKEOFF (Guided mode)")
                    else:
                        print("   Drone State: DENIED (Drone must be armed first)")
                        ack_result = mavutil.mavlink.MAV_RESULT_DENIED
                elif cmd_id == mavutil.mavlink.MAV_CMD_NAV_RETURN_TO_LAUNCH:
                    flight_mode = 6  # RTL mode
                    print("   Drone State: RTL")

                # Send COMMAND_ACK packet back to the phone
                master.mav.command_ack_send(cmd_id, ack_result)
                print(f"<- Sent COMMAND_ACK (Result: {ack_result})")

    # 2. Rate-limit telemetry sending to 1Hz
    if current_time - last_send_time >= 1.0:
        base_mode = mavutil.mavlink.MAV_MODE_FLAG_SAFETY_ARMED if armed else 0

        # Heartbeat message
        master.mav.heartbeat_send(
            mavutil.mavlink.MAV_TYPE_QUADROTOR,
            mavutil.mavlink.MAV_AUTOPILOT_ARDUPILOTMEGA,
            base_mode,
            flight_mode,
            mavutil.mavlink.MAV_STATE_ACTIVE
        )

        # GPS & Altitude simulation (drift upward if taking off)
        if armed and flight_mode == 4:
            altitude += 100
        master.mav.global_position_int_send(
            time_boot_ms,
            int(22.7643 * 1e7),
            int(88.3697 * 1e7),
            altitude,
            0, 0, 0, 0, 0
        )

        # Battery remaining status
        battery = max(10, battery - 1)
        master.mav.sys_status_send(
            0, 0, 0, 0, 14800, -1, battery, 0, 0, 0, 0, 0, 0
        )

        mode_label = "Guided" if flight_mode == 4 else "RTL" if flight_mode == 6 else "Stabilize"
        print(f"Packet Sent -> Alt: {altitude/1000}m | Batt: {battery}% | Armed: {armed} | Mode: {mode_label}")
        last_send_time = current_time

    # Yield control to prevent high CPU utilization
    time.sleep(0.01)