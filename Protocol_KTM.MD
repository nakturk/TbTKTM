# KTM BLE & Bluetooth Classic Communication Protocol & TFT Dashboard Control Specification

This document provides the **authoritative, 100% reverse-engineered and hardware-verified technical specification** of the **KTMconnect** communication protocol for the **KTM 1290 Super Adventure** (Euro 5 BCCU connectivity unit and compatible KTM / Husqvarna / GASGAS motorcycles).

With this specification, any developer can build custom mobile applications (Android / iOS) or embedded devices (ESP32, Raspberry Pi) to **control Turn-by-Turn (TBT) navigation on the KTM TFT dashboard, stream live motorcycle telemetry (lean angle, throttle, gear, brake pressure), and intercept handlebar switch controls**.

---

## 1. General Architecture & Transport Model

The KTM Dashboard (TFT) and Bluetooth Connectivity Control Unit (BCCU) communicate via **Bluetooth Classic RFCOMM (SPP)** as the primary high-speed data bus for Turn-by-Turn navigation, telemetry, and diagnostics:

* **RFCOMM Service Base UUID:** `XXXX1fb3-482e-4389-bdeb-57b7aac889ae`
* **Primary Navigation Channel:** `EXTENDED_TBT` (`cc4d1fb3-482e-4389-bdeb-57b7aac889ae` / Port 52301)
* **Primary Telemetry Channel:** `TELEMETRY` (`cb661fb3-482e-4389-bdeb-57b7aac889ae` / Port 52070)
* **Underlying Protocol:** **KMRC Framed UTF-8 JSON Tree**

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

## 3. Bluetooth Classic RFCOMM Channel Map (KMRC TransportMap)

| Channel Name | Port (Dec) | Port (Hex) | RFCOMM Service UUID | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`EXTENDED_TBT`** | 52301 | `0xCC4D` | `cc4d1fb3-482e-4389-bdeb-57b7aac889ae` | **Primary Turn-by-Turn Navigation Channel** |
| **`CCUBASE`** | 52010 | `0xCB2A` | `cb2a1fb3-482e-4389-bdeb-57b7aac889ae` | Base HMI, Device State & Login Handshake |
| **`NAVIGATION`** | 52300 | `0xCC4C` | `cc4c1fb3-482e-4389-bdeb-57b7aac889ae` | Standard Navigation Channel |
| **`CCULOGGING`** | 52030 | `0xCB3E` | `cb3e1fb3-482e-4389-bdeb-57b7aac889ae` | BCCU Diagnostic & System Logging |
| **`CCUWIFICONFIG`**| 52040 | `0xCB48` | `cb481fb3-482e-4389-bdeb-57b7aac889ae` | Wi-Fi / Hotspot Provisioning |
| **`TELEMETRY`** | 52070 | `0xCB66` | `cb661fb3-482e-4389-bdeb-57b7aac889ae` | Real-time Sensor & Telemetry Data |
| **`KTELNET`** | 52000 | `0xCB20` | `cb201fb3-482e-4389-bdeb-57b7aac889ae` | Factory Diagnostic Shell (kshell/Telnet) |
| **`KSTREAM`** | 52001 | `0xCB21` | `cb211fb3-482e-4389-bdeb-57b7aac889ae` | High-speed Data Stream |

---

## 4. Hardware-Verified KMRC JSON Specification

### 4.1. Step 1: Initial Handshake / Dashboard Wakeup (`MsgId: "Restore"`)

Immediately upon connecting to `EXTENDED_TBT (cc4d)`, send the initial state frame. The presence of `"RoutePlans"` and `"TbtGuidanceModeOn"` prompts the TFT dashboard to switch to navigation mode:

```json
{
  "RoutePlans": [
    {
      "Class": "Favorite",
      "Description": "Home Address",
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
      "Text": "Turn Right",
      "Visibility": "full"
    },
    "TurnRoad": {
      "Text": "Main Street",
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

### 4.2. Step 2: Dynamic Maneuver Updates (`MsgId: "mup"`)

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
      "Text": "Turn Left onto Broadway",
      "Visibility": "full"
    },
    "TurnRoad": {
      "Text": "Broadway Ave",
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

### 4.3. Step 3: Location / Distance Decrement Updates (`MsgId: "lup"`)

Sent every 1–2 seconds while riding to refresh remaining distance and ETA:

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

### 4.4. Step 4: Stopping Navigation (`MsgId: "GuidanceModeOff"`)

```json
{
  "TbtGuidanceModeOff": {},
  "MsgId": "GuidanceModeOff"
}
```

---

## 5. Critical Formatting Rules

1. **`Visibility` Values:** Must always be lowercase strings: `"full"`, `"half"`, or `"off"`. Uppercase (`"FULL"`) is rejected by the cluster.
2. **`TurnIcon.Image` Enum Names:** Must be exact uppercase string identifiers (e.g. `"QUITE_RIGHT"`, `"GO_STRAIGHT"`).
3. **`MsgId` Values:**
   * `"Restore"`: Handshake & initial state sync.
   * `"mup"`: Maneuver update (new turn direction/arrow).
   * `"lup"`: Location update (distance countdown / ETA).
   * `"GuidanceModeOff"`: Exit navigation mode.

---

## 6. Turn Maneuver & Icon Enum Mapping

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
| `ROUNDABOUT_1` .. `_8` | Roundabout Exits 1 to 8 | 26..33 |

---

## 7. Real-time Telemetry Datapoint Catalog (`CUKT_bCCU.json`)

KTM BCCU streams binary sensor telemetry on channel `TELEMETRY` (`cb661fb3-482e-4389-bdeb-57b7aac889ae` / Port 52070):

| Datapoint ID | Signal Name | Unit | Data Type | Description |
| :---: | :--- | :---: | :---: | :--- |
| **`322`** | `ABS_Lean_Angle` | Degree (°) | `Float32` | **Dynamic Roll/Lean Angle (-: Left, +: Right)** |
| **`323`** | `ABS_Pitch_Angle` | Degree (°) | `Float32` | **Dynamic Pitch Angle (Wheelie / Stoppie)** |
| **`304`** | `ECU_Accel_Position` | % | `Float32` | **Throttle Position (TPS %0.0 - %100.0)** |
| **`303`** | `ECU_Engine_Rpm` | RPM | `UInt16` | **Engine Speed (RPM)** |
| **`314`** | `ECU_Gear_Position` | - | `UInt8` | **Selected Gear (`0=N`, `1..6`)** |
| **`326`** | `ABS_Display_Front_Wheel_Speed` | km/h | `Float32` | **Front Wheel Speed** |
| **`325`** | `ABS_Display_Rear_Wheel_Speed` | km/h | `Float32` | **Rear Wheel Speed** |
| **`358`** | `ABS_Front_Wheel_Press` | Bar | `Float32` | **Front Brake Hydraulic Pressure** |
| **`302`** | `ECU_Water_Temperature` | °C | `Float32` | **Engine Coolant Temperature** |
| **`320`** | `ECU_Oil_Temp_Data` | °C | `Int16` | **Engine Oil Temperature** |
| **`338`** | `TPMS_Pressure_Front` | mbar | `UInt16` | **Front Tire Pressure (mbar)** |
| **`337`** | `TPMS_Pressure_Rear` | mbar | `UInt16` | **Rear Tire Pressure (mbar)** |
| **`356`** | `DASH_Fuel_Range` | km | `UInt16` | **Remaining Fuel Range** |
| **`349`** | `DASH_Odo_Meter_Extended` | km | `Float32` | **Odometer Total Mileage** |

---

## 8. Handlebar Switch Controls (`RCM_REMOTE_CONTROL`)

Handlebar button events are received via characteristic `71ced1ac-0103-44f5-9454-806ff70b3e02`:

* **Byte 16:** `0xFF` indicates RCM active.
* **Byte 17:** Button bitmask:
  * `0x01`: `SET / CENTER` (Confirm)
  * `0x02`: `BACK`
  * `0x04`: `DOWN`
  * `0x08`: `UP`
  * `0x10`: `LEFT`
  * `0x20`: `RIGHT`
  * `0x40`: `C1` (Custom Switch 1)
  * `0x80`: `C2` (Custom Switch 2)
