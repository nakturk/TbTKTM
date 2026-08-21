# KTM BLE & Bluetooth Classic Communication Protocol & TFT Dashboard Control Specification

This document provides the **authoritative, 100% reverse-engineered and hardware-verified technical specification** of the **KTMconnect (v4.0.2+)** communication protocol for the **KTM 1290 Super Adventure**, Duke, RC, and all Euro 5 BCCU/CUO-equipped motorcycles (KTM, Husqvarna, GASGAS).

With this specification, developers can build custom mobile applications (Android / iOS) or embedded devices (ESP32, Raspberry Pi) to **control Turn-by-Turn (TBT) navigation, stream full-screen map graphics, ingest high-frequency motorcycle telemetry (CAN-bus signals, lean angle, TPS, pressures), handle incoming dashboard commands, and interface with handlebar switch controls**.

---

## 1. General Architecture & Transport Model

The KTM Dashboard (TFT) and Bluetooth Connectivity Control Unit (BCCU / CUO) operate on a dual-stack connectivity model:

1. **Bluetooth Classic RFCOMM (SPP):** Primary high-throughput data bus for Turn-by-Turn navigation (`EXTENDED_TBT`), full-display map streaming (`MAPNAV_STREAMING`), device control, and telemetry streaming.
2. **Bluetooth Low Energy (BLE GATT):** Low-power channel for remote handlebar button presses (RCM/SCA), basic TBT indicators, and initial handshake.
3. **Wi-Fi & TCP/IP (Offroad / CUO):** High-speed socket connection (Port 52040 / 52070) for Offroad track telemetry, GPS lap timing, and engine ECU mapping.

* **RFCOMM Service Base UUID:** `XXXX1fb3-482e-4389-bdeb-57b7aac889ae`
* **Underlying Protocol:** **KMRC Framed UTF-8 JSON Tree** & **Binary pRPC (Portable RPC)**

---

## 2. KMRC Transport Framing Protocol (CRITICAL)

> [!IMPORTANT]
> The KTM cluster **does NOT accept raw JSON strings**. Every KMRC message sent over RFCOMM must be encapsulated within a **5-byte KMRC Transport Header**. Failing to include this header causes the cluster's socket reader (`readInt()`) to misinterpret the JSON text as a multi-gigabyte payload length, resulting in an immediate socket termination (`Broken pipe / read return: -1`).

### 2.1. Frame Structure

```
+--------------------------+--------------------+---------------------------------------+
|  Length (4 Bytes Int32)  | MsgType (1 Byte)   |          JSON Payload (UTF-8)         |
|       Big-Endian         |       0x01         |         {"RoutePlans": ...}           |
+--------------------------+--------------------+---------------------------------------+
|<----------------- 5 Bytes Header ------------>|<------------- N Bytes --------------->|
```

* **Length (4 Bytes, Big-Endian Int32):** Total size of the frame payload, which is `jsonBytes.length + 1` (accounting for the Message Type byte).
* **MsgType (1 Byte, UInt8):** Always `0x01` (`KMRC_MSG`).
* **JSON Payload (N Bytes):** The UTF-8 encoded KMRC JSON string.

### 2.2. Reference Implementation (Kotlin)

```kotlin
fun frameKmrcMessage(jsonString: String): ByteArray {
    val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
    val buffer = ByteBuffer.allocate(4 + 1 + jsonBytes.size)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer.putInt(jsonBytes.size + 1) // 4-byte Int: length + 1 (type byte)
    buffer.put(0x01.toByte())          // 1-byte: KMRC Message Type
    buffer.put(jsonBytes)
    return buffer.array()
}
```

### 2.3. Socket Receiver Decoding Loop

```kotlin
val dis = DataInputStream(socket.inputStream)
while (isActive) {
    val len = dis.readInt()
    val type = dis.readByte()
    val data = ByteArray(len - 1)
    dis.readFully(data)

    if (type == 0x01.toByte()) {
        val jsonText = String(data, Charsets.UTF_8)
        // Process incoming dashboard event (e.g. SkipWayPoint, POI request)
    }
}
```

---

## 3. Complete RFCOMM Transport Channel Map (`TransportMap`)

KTM motorcycles expose 18 dedicated RFCOMM services defined in `com.ktm.kmrc.TransportMap`. The service UUID is constructed dynamically as `Integer.toHexString(port) + "1fb3-482e-4389-bdeb-57b7aac889ae"`:

| Channel Name | Port (Dec) | Port (Hex) | RFCOMM Service UUID | Description & Role |
| :--- | :--- | :--- | :--- | :--- |
| **`EXTENDED_TBT`** | 52301 | `0xCC4D` | `cc4d1fb3-482e-4389-bdeb-57b7aac889ae` | **Primary Turn-by-Turn Navigation & Marquee Notification Channel** |
| **`NAVIGATION`** | 52300 | `0xCC4C` | `cc4c1fb3-482e-4389-bdeb-57b7aac889ae` | Legacy / Standard Navigation Data Stream |
| **`MAPNAV_SIGNALLING`**| 52400 | `0xCCC0` | `ccc01fb3-482e-4389-bdeb-57b7aac889ae` | Full Map Screen Control & Viewport Negotiation |
| **`MAPNAV_STREAMING`** | 52410 | `0xCCCA` | `ccca1fb3-482e-4389-bdeb-57b7aac889ae` | Full Map Video/Image Tile Streaming (CRLE / JPEG) |
| **`TELEMETRY`** | 52070 | `0xCB66` | `cb661fb3-482e-4389-bdeb-57b7aac889ae` | Real-time Sensor & CAN-Bus Telemetry Stream (pRPC) |
| **`CCUBASE`** | 52010 | `0xCB2A` | `cb2a1fb3-482e-4389-bdeb-57b7aac889ae` | Base HMI, Device State & Login Handshake |
| **`DEVICECONTROL`** | 52320 | `0xCC60` | `cc601fb3-482e-4389-bdeb-57b7aac889ae` | Remote Audio, Volume & Media Interception |
| **`CCULOGGING`** | 52030 | `0xCB3E` | `cb3e1fb3-482e-4389-bdeb-57b7aac889ae` | BCCU Diagnostic & System Logging |
| **`CCUWIFICONFIG`** | 52040 | `0xCB48` | `cb481fb3-482e-4389-bdeb-57b7aac889ae` | Wi-Fi Hotspot Provisioning & Password Claiming |
| **`CCUGATEKEEPER`** | 52041 | `0xCB49` | `cb491fb3-482e-4389-bdeb-57b7aac889ae` | Security Gatekeeper & Access Control |
| **`CCUUSERSETTINGS`**| 52050 | `0xCB52` | `cb521fb3-482e-4389-bdeb-57b7aac889ae` | Rider Profile & Dashboard Setting Sync |
| **`SMART_HELP_CALL`**| 52911 | `0xCEAF` | `ceaf1fb3-482e-4389-bdeb-57b7aac889ae` | **eCall / Crash & Emergency SOS Dispatcher** |
| **`SCA`** | 53060 | `0xCF44` | `cf441fb3-482e-4389-bdeb-57b7aac889ae` | Smart Control Access / Remote Switch Bus |
| **`KTELNET`** | 52000 | `0xCB20` | `cb201fb3-482e-4389-bdeb-57b7aac889ae` | Factory Diagnostic Shell (`kshell`/Telnet) |
| **`KSTREAM`** | 52001 | `0xCB21` | `cb211fb3-482e-4389-bdeb-57b7aac889ae` | High-speed Binary Data Stream |
| **`LOGGING`** | 62300 | `0xF35C` | `f35c1fb3-482e-4389-bdeb-57b7aac889ae` | Global System Trace Logging |

---

## 4. Hardware-Verified KMRC JSON Specification

### 4.1. Initial Handshake & Dashboard Wakeup (`MsgId: "Restore"`)

Upon connecting to `EXTENDED_TBT (cc4d)`, the client sends the initial state frame. The presence of `"RoutePlans"` and `"TbtGuidanceModeOn"` prompts the TFT dashboard to switch to navigation mode:

```json
{
  "RoutePlans": [
    {
      "Class": "Favorite",
      "Description": "Ev",
      "IsRoutable": true,
      "ID": "Fav#0",
      "SubTitle": "",
      "Title": "Home"
    },
    {
      "Class": "Favorite",
      "Description": "KTM Motohall",
      "IsRoutable": true,
      "ID": "Fav#1",
      "SubTitle": "",
      "Title": "KTM Motohall"
    }
  ],
  "GuidanceVolumeAdjusted": {
    "Level": 80
  },
  "TbtGuidanceModeOn": {},
  "NotificationText": {
    "Visibility": "off"
  },
  "NotificationIcon": {
    "Visibility": "off"
  },
  "GpsIcon": {
    "Visibility": "off"
  },
  "GuidanceUpdate": {
    "TurnIcon": {
      "Image": "QUITE_RIGHT",
      "Visibility": "full"
    },
    "TurnDist": {
      "Text": "350",
      "Visibility": "full"
    },
    "TurnDistUnit": {
      "Text": "m",
      "Visibility": "full"
    },
    "TurnInfo": {
      "Text": "Sağa Dönün",
      "Visibility": "full"
    },
    "TurnRoad": {
      "Text": "Bağdat Caddesi",
      "Visibility": "full"
    },
    "ETA": {
      "Text": "19:15",
      "Visibility": "full"
    },
    "Dist2Target": {
      "Text": "8.4 km",
      "Visibility": "full"
    }
  },
  "ActiveWayPoint": {},
  "MsgId": "Restore"
}
```

### 4.2. Dynamic Maneuver Updates (`MsgId: "mup"`)

Sent whenever the route direction changes (e.g. incoming Google Maps navigation instruction):

```json
{
  "GuidanceUpdate": {
    "TurnIcon": {
      "Image": "QUITE_LEFT",
      "Visibility": "full"
    },
    "TurnDist": {
      "Text": "150",
      "Visibility": "full"
    },
    "TurnDistUnit": {
      "Text": "m",
      "Visibility": "full"
    },
    "TurnInfo": {
      "Text": "Sola Dönün",
      "Visibility": "full"
    },
    "TurnRoad": {
      "Text": "Atatürk Bulvarı",
      "Visibility": "full"
    },
    "ETA": {
      "Text": "19:12",
      "Visibility": "full"
    },
    "Dist2Target": {
      "Text": "5.2 km",
      "Visibility": "full"
    }
  },
  "MsgId": "mup"
}
```

### 4.3. Location / Distance Decrement Updates (`MsgId: "lup"`)

Sent periodically while riding to refresh remaining distance and ETA:

```json
{
  "GuidanceUpdate": {
    "TurnDist": {
      "Text": "80",
      "Visibility": "full"
    },
    "TurnDistUnit": {
      "Text": "m",
      "Visibility": "full"
    },
    "ETA": {
      "Text": "19:12",
      "Visibility": "full"
    },
    "Dist2Target": {
      "Text": "5.1 km",
      "Visibility": "full"
    }
  },
  "MsgId": "lup"
}
```

### 4.4. Route Rerouting / Lost GPS Signal (`MsgId: "mup"`)

```json
{
  "GuidanceUpdate": {
    "TurnIcon": {
      "Image": "UNDEFINED",
      "Visibility": "off"
    },
    "TurnInfo": {
      "Text": "Rota Yeniden Hesaplanıyor...",
      "Visibility": "full"
    }
  },
  "GpsIcon": {
    "Visibility": "full"
  },
  "MsgId": "mup"
}
```

### 4.5. Stopping Navigation (`MsgId: "GuidanceModeOff"`)

```json
{
  "TbtGuidanceModeOff": {},
  "MsgId": "GuidanceModeOff"
}
```

---

## 5. Complete BLE GATT Services & Characteristics Map

KTM BCCU implements GATT services under base UUID `71ced1ac-XXXX-44f5-9454-806ff70b3e02`:

### 5.1. Turn-by-Turn GATT Service (`0x0700`)
`UUID: 71ced1ac-0700-44f5-9454-806ff70b3e02`

| Characteristic UUID | Property | Type | Description |
| :--- | :---: | :---: | :--- |
| `0x0701` | Write | `Byte[16]` | **TBT Authentication Request** (Handshake token) |
| `0x0702` | Read/Notify | `Byte[16]` | **TBT Authentication Response** (Auth challenge response) |
| `0x0703` | Read/Notify | `UInt8` | **TBT Navigation State** (`0x01`=Active, `0x00`=Idle) |
| `0x0704` | Write | `UInt8` | **Turn Icon ID** (KTM 57 Icon Enum Code) |
| `0x0705` | Write | `String` | **Turn Distance** (e.g. `"250 m"`) |
| `0x0706` | Write | `String` | **Turn Info** (Localized maneuver text) |
| `0x0707` | Write | `String` | **Turn Road** (Target street/road name) |
| `0x0708` | Write | `String` | **Estimated Time of Arrival (ETA)** (e.g. `"14:30"`) |
| `0x0709` | Write | `String` | **Distance to Destination** (e.g. `"12.4 km"`) |
| `0x070A` | Write | `String` | **TBT Notification** (Caller ID / Alert text) |
| `0x070B` | Read/Notify | `UInt8` | **Dashboard Navigation Command** (Skip Waypoint, Mute, Stop) |

### 5.2. Handlebar Remote Control Service (SCA / RCM `0x0100`)
`UUID: 71ced1ac-0100-44f5-9454-806ff70b3e02`

| Characteristic UUID | Property | Type | Description |
| :--- | :---: | :---: | :--- |
| `0x0101` | Read/Notify | `UInt8` | **SCA Connection Active** |
| `0x0103` | Notify | `Byte[20]` | **Handlebar Button Presses** (Bitmask & Long Press) |

---

## 6. Handlebar Button & Dashboard Command Bitmasks

### 6.1. Handlebar Switch Controls (`BccuKeyEvent`)
Received on Characteristic `0x0103` (Byte 17):

| Bit / Value | Constant | Physical Button | Description |
| :---: | :--- | :--- | :--- |
| `0x01` (`1`) | `SET` | **SET / CENTER** | Confirm / Play / Pause |
| `0x02` (`2`) | `BACK` | **BACK** | Back / Exit Menu |
| `0x04` (`4`) | `DOWN` | **DOWN** | Down Arrow / Volume Down |
| `0x08` (`8`) | `UP` | **UP** | Up Arrow / Volume Up |
| `0x10` (`16`) | `LEFT` | **LEFT** | Left Arrow / Previous Track |
| `0x20` (`32`) | `RIGHT` | **RIGHT** | Right Arrow / Next Track |
| `0x40` (`64`) | `C1` | **Custom Switch 1** | Voice Assistant / Shortcut 1 |
| `0x80` (`128`) | `C2` | **Custom Switch 2** | Recenter Map / Shortcut 2 |
| `0x12` (`18`) | `BACK_LONG` | **BACK (Long Press)** | Long press back |
| `0x13` (`19`) | `DOWN_LONG` | **DOWN (Long Press)** | Long press down |
| `0x14` (`20`) | `UP_LONG` | **UP (Long Press)** | Long press up |

### 6.2. Dashboard Navigation Feedback Commands (`NavigationCmd`)
Received on Characteristic `0x070B` or KMRC socket:

| Byte Value | Command Constant | Action Triggered on Phone |
| :---: | :--- | :--- |
| `0x00` | `NONE` | No action |
| `0x01` | `START` | Start / Resume Navigation |
| `0x02` | `STOP` | Cancel / End Route |
| `0x04` | `SKIP_WAYPOINT` | **Skip current waypoint / Go to next via-point** |
| `0x10` | `VOLUME_UP` | Increase Guidance Voice Volume |
| `0x20` | `VOLUME_DOWN` | Decrease Guidance Voice Volume |
| `0x40` | `MUTE` | Mute Navigation Audio Prompts |
| `0x80` | `UNMUTE` | Unmute Navigation Audio Prompts |

---

## 7. Full Hardware CAN-Bus Telemetry Datapoint Catalog (`CUKT_bCCU.json`)

The KTM BCCU streams real-time telemetry over RFCOMM port 52070 (`TELEMETRY`). Every datapoint maps to real vehicle CAN-Bus arbitration IDs:

| ID | Signal Name | CAN ID | Unit | Type | Sample Rate | Description & Route |
| :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| **`301`** | `ECU_Fuel_Consumption` | `0x540` | ml/s | `Float32` | 100ms - 5s | Instantaneous fuel consumption (ECU -> DASH) |
| **`302`** | `ECU_Water_Temperature` | `0x540` | °C | `Float32` | 500ms - 5s | Coolant temperature (ECU -> DASH) |
| **`303`** | `ECU_Engine_Rpm` | `0x120` | RPM | `UInt16` | **10ms - 1s** | **High-frequency Engine RPM (ECU -> DASH)** |
| **`304`** | `ECU_Accel_Position` | `0x120` | % | `Float32` | **10ms - 1s** | **Throttle Position (TPS %0.0 - %100.0) (ECU -> ABS)** |
| **`305`** | `ECU_Malfunction_Indica_LED`| `0x540`| - | `UInt8` | 1s - 60s | Check Engine / MIL Warning Light |
| **`306`** | `ECU_Side_Stand` | `0x540` | - | `UInt8` | 100ms - 5s | Side stand switch status (`1=Down, 0=Up`) |
| **`307`** | `ECU_Eng_Driver_Request_Torque`|`0x121`| Nm | `Float32`| 10ms - 1s | Rider demanded torque (ECU -> ABS) |
| **`308`** | `ECU_Eng_Actual_Torque` | `0x121` | Nm | `Float32` | 10ms - 1s | Actual delivered engine torque (ECU -> ABS) |
| **`309`** | `ECU_Quickshifter_Warning` | `0x129` | - | `UInt8` | 500ms - 10s| Quickshifter+ malfunction warning |
| **`310`** | `ECU_Quickshifter_Response`| `0x129` | - | `UInt8` | 100ms - 5s | Quickshifter active/ready status |
| **`311`** | `ECU_CC_LED` | `0x129` | - | `UInt8` | 200ms - 5s | Cruise Control indicator LED |
| **`312`** | `ECU_CC_Target_Speed` | `0x129` | km/h | `Float32` | 100ms - 5s | Cruise Control set target speed |
| **`313`** | `ECU_Clutch_Switch` | `0x129` | - | `UInt8` | 50ms - 2s | Clutch lever pulled switch (`1=Pulled`) |
| **`314`** | `ECU_Gear_Position` | `0x129` | - | `UInt8` | 50ms - 2s | **Selected Gear (`0=N`, `1..6`)** |
| **`317`** | `ECU_Roll_Over_Warning` | `0x540` | - | `UInt8` | 50ms - 1s | Tip-over / Crash detection warning |
| **`319`** | `ECU_Low_Oil_Level_Warning` | `0x540` | - | `UInt8` | 1s - 30s | Low engine oil level sensor |
| **`320`** | `ECU_Oil_Temp_Data` | `0x540` | °C | `Int16` | 500ms - 10s| Engine oil temperature |
| **`321`** | `ABS_Trajectory_Acceler`| `0x12F` | m/s² | `Float32` | **10ms - 1s** | 6-Axis IMU Trajectory Acceleration |
| **`322`** | `ABS_Lean_Angle` | `0x12F` | Deg (°) | `Float32` | **10ms - 500ms**| **Dynamic Lean Angle (-: Left, +: Right)** |
| **`323`** | `ABS_Pitch_Angle` | `0x12F` | Deg (°) | `Float32` | **10ms - 500ms**| **Pitch Angle (Wheelie / Stoppie Detection)** |
| **`324`** | `ABS_Roll_Over` | `0x12F` | - | `UInt8` | 10ms - 500ms| IMU Crash / Bank angle threshold exceeded |
| **`325`** | `ABS_Display_Rear_Wheel_Speed`|`0x12D`| km/h | `Float32`| **10ms - 1s** | Rear wheel speed sensor |
| **`326`** | `ABS_Display_Front_Wheel_Speed`|`0x12D`| km/h| `Float32`| **10ms - 1s** | Front wheel speed sensor |
| **`329`** | `ABS_MTC_Mode_Response` | `0x12A` | - | `UInt8` | 200ms - 5s | Traction Control Mode (Street/Rain/Sport/Rally) |
| **`330`** | `ABS_ABS_Warning` | `0x12A` | - | `UInt8` | 500ms - 10s| ABS System Warning |
| **`331`** | `ABS_CABS_Warning` | `0x12A` | - | `UInt8` | 500ms - 10s| Cornering ABS (C-ABS) Status |
| **`332`** | `ABS_LC_Warning` | `0x12A` | - | `UInt8` | 100ms - 5s | Launch Control Ready / Active |
| **`333`** | `ABS_MTC_Warning` | `0x12A` | - | `UInt8` | 100ms - 5s | Traction Control Intervening indicator |
| **`334`** | `ABS_Slip_Level_Response` | `0x12A` | - | `UInt8` | 50ms - 2s | Rally Mode Spin Adjust Level (1-9) |
| **`335`** | `TPMS_Temperature_Rear` | `0x310` | °C | `Int16` | 2s - 60s | Rear Tire Internal Air Temp |
| **`336`** | `TPMS_Temperature_Front`| `0x310` | °C | `Int16` | 2s - 60s | Front Tire Internal Air Temp |
| **`337`** | `TPMS_Pressure_Rear` | `0x310` | mbar | `UInt16` | 1s - 30s | **Rear Tire Pressure (mbar)** |
| **`338`** | `TPMS_Pressure_Front` | `0x310` | mbar | `UInt16` | 1s - 30s | **Front Tire Pressure (mbar)** |
| **`346`** | `TPMS_Puncture_FR_Warning` | `0x310` | - | `UInt8` | 500ms - 10s| Rapid front tire puncture alarm |
| **`347`** | `TPMS_Puncture_RE_Warning` | `0x310` | - | `UInt8` | 500ms - 10s| Rapid rear tire puncture alarm |
| **`349`** | `DASH_Odo_Meter_Extended` | `0x55A` | km | `Float32` | 1s - 60s | Total Lifetime Odometer |
| **`350`** | `DASH_Tank_Reserve` | `0x541` | - | `UInt8` | 1s - 30s | Low fuel warning trip triggered |
| **`351`** | `DASH_Service_Distance` | `0x553` | km | `UInt16` | 5s - 60s | Remaining distance until next service |
| **`355`** | `DASH_Fuel_Level` | `0x553` | Seg/% | `UInt8` | 1s - 30s | Fuel gauge reading (0..8 bars) |
| **`356`** | `DASH_Fuel_Range` | `0x553` | km | `UInt16` | 1s - 30s | Remaining estimated fuel range |
| **`357`** | `ABS_Front_Brake_Actuation` | `0x290` | - | `UInt8` | 20ms - 500ms| Front brake lever pulled |
| **`358`** | `ABS_Front_Wheel_Press` | `0x290` | Bar | `Float32` | **10ms - 500ms**| **Front Brake Hydraulic Pressure** |
| **`359`** | `ABS_Rear_Brake_Actuation` | `0x290` | - | `UInt8` | 20ms - 500ms| Rear brake pedal pressed |
| **`360`** | `ABS_ABS_Mode_Response` | `0x12A` | - | `UInt8` | 200ms - 5s | ABS Mode (`Road`, `Supermoto`, `Offroad`) |
| **`363`** | `HCU_General_Failure_LED`| `0x560` | - | `UInt8` | 2s - 60s | Heated Grips / Seat Controller Error |
| **`364`** | `IMMO_General_Failure_LED`| `0x4EA` | - | `UInt8` | 1s - 30s | Keyless Race-On / Transponder Status |

---

## 8. Binary pRPC (Portable RPC) Telemetry Streaming Protocol

When streaming telemetry via port `52070` (`TELEMETRY`), data is packaged as binary pRPC frames:

### 8.1. Request Header (Client to Bike)
```
+----------------+----------------+----------------+--------------------+
|  SessionID     |  Length        |  Command ID    |  Payload...        |
|  (1 Byte UInt8)|  (1 Byte UInt8)|  (2 Bytes BE)  |  (N Bytes)         |
+----------------+----------------+----------------+--------------------+
```
- **Command 2 (`RPCSetTime`):** Synchronizes Unix timestamp with motorcycle RTC.
- **Command 3 (`RPCConfigure`):** Sets sample rate for specified Datapoint ID: `[DatapointID: Short] + [SampleRateMs: Short]`.
- **Command 4 (`RPCControl`):** Start (`0`), Pause (`1`), or Stop & Clear (`2`) telemetry stream.

### 8.2. Telemetry Notification Frame (Bike to Client)
```
+----------------+----------------+----------------+-------------------------------------+
|  NotifyCode    |  Length        |  Stuffing      |  Datapoint Triples                  |
|  (2 Bytes BE)  |  (1 Byte UInt8)|  (1 Byte)      |  [ID: Short] + [Timestamp] + [Value]|
+----------------+----------------+----------------+-------------------------------------+
```

---

## 9. Full Map Navigation Screen Streaming & Codecs

On models supporting full-screen map projection (via `MAPNAV_STREAMING` / Port `52410`):
1. **CRLE (Compressed Run-Length Encoding):** `JniCrleIndexedColorKCodecImpl` encodes 8-bit paletted vector tiles for extreme bandwidth efficiency over Bluetooth SPP.
2. **JPEG Streamer:** `JpegAndroidCodec` streams live map raster tiles at 15-30 FPS.
3. **Viewport Negotiation:** Handled on `MAPNAV_SIGNALLING` (Port `52400`) providing display resolution, pixel depth, and zoom level.

---

## 10. Connectivity Unit Offroad (CUO / SX-F / FC) Master Specification

For motocross and enduro models equipped with the CUO unit (`CUKTCUO_masterv91.00.json`), the system supports:
- **10Hz High-Precision GNSS:** Lat/Lon (`Double`), DOP (`Float`), Satellites in use (`UInt8`), Speed (`m/s`).
- **WP Suspension Telemetry:** Front Fork Air Pressure (ID `45`, `mbar`), Rear Swingarm Sag Voltage (ID `46`, `mV`).
- **ECU Engine Mapping Adjustment:**
  - Fuel Injection Trims (`injcIdle`, `injcAll` - ID `23-24`)
  - Ignition Timing Trims (`igncIdle`, `igncAll` - ID `19-20`)
  - Traction Control Intervention Levels (`tcParamIgnitionTimingLevels` - ID `22`)
  - Launch Control Parameters (`lcParamsLevels` - ID `21`)
  - Quickshifter Kill Time (`qsIGCut` - ID `25`, `qsGPADJudgementWidth` - ID `26`)

---

## 11. Security, Pairing & CCU Claiming Handshake

1. **Factory Password Discovery:** The CCU starts in unclaimed mode with default factory password.
2. **Claiming Phase (`CCUWIFICONFIG` / Port `52040`):**
   ```
   TRY_LOGIN_WITH_FACTORYPASSWD ➔ SET_APPLICATION_PASSWORD ➔ LOGIN_WITH_APPLICATION_PASSWORD ➔ GET_OPERATING_CONDITION
   ```
3. **VIN Validation:** `VinChecker.check()` verifies that the motorcycle's VIN matches the registered displacement and model metadata.
4. **Native BCCU Auth:** Native library `libbccuauth.so` (`BccuAuthNativeWrapper`) performs mutual HMAC challenge-response verification before high-speed telemetry or ECU flash maps are unlocked.

---

## 12. Turn Maneuver & Icon Enum Mapping (`KtmTurnIcon`)

| Enum Identifier | Visual Maneuver | Icon Code |
| :--- | :--- | :---: |
| `UNDEFINED` | Undefined / Hidden | 1 |
| `GO_STRAIGHT` | Continue Straight | 2 |
| `UTURN_RIGHT` | U-Turn Right | 3 |
| `UTURN_LEFT` | U-Turn Left | 4 |
| `KEEP_RIGHT` | Keep Right | 5 |
| `LIGHT_RIGHT` | Slight Right Turn | 6 |
| `QUITE_RIGHT` | Normal Right Turn (90°) | 7 |
| `HEAVY_RIGHT` | Sharp Right Turn | 8 |
| `KEEP_MIDDLE` | Keep Middle | 9 |
| `KEEP_LEFT` | Keep Left | 10 |
| `LIGHT_LEFT` | Slight Left Turn | 11 |
| `QUITE_LEFT` | Normal Left Turn (90°) | 12 |
| `HEAVY_LEFT` | Sharp Left Turn | 13 |
| `ENTER_HIGHWAY_RIGHT_LANE`| Enter Highway (Right Lane) | 14 |
| `ENTER_HIGHWAY_LEFT_LANE` | Enter Highway (Left Lane) | 15 |
| `LEAVE_HIGHWAY_RIGHT_LANE`| Exit Highway (Right) | 16 |
| `LEAVE_HIGHWAY_LEFT_LANE` | Exit Highway (Left) | 17 |
| `HIGHWAY_KEEP_RIGHT` | Keep Right on Highway | 18 |
| `HIGHWAY_KEEP_LEFT` | Keep Left on Highway | 19 |
| `START` | Starting Point / Flag | 20 |
| `END` | Arrived at Destination (Checkered Flag) | 21 |
| `FERRY` | Ferry Transit | 22 |
| `PASS_STATION` | Pass Waypoint / Station | 23 |
| `HEAD_TO` | Head Towards Direction | 24 |
| `CHANGE_LINE` | Change Lane | 25 |
| `ROUNDABOUT_1` .. `_8` | Roundabout Exits 1 to 8 | 26..33 |
