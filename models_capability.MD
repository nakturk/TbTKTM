# KTM & Husqvarna Motorcycle Hardware & Feature Capability Matrix (Models Capability)

This document is the **official model capability and protocol compatibility matrix** based on reverse-engineering analysis of **KTMconnect (v4.0.2)** source code, `bmdb-data` (Bike Model Database), `DASH_Meter_Info_1104.java` (`DASH_Variant` IDs), `CUKT_bCCU.json`, and `CUKTCUO_master.json` telemetry schemas.

---

## 1. General Model & Feature Compatibility Matrix

| Model Family | Display / Unit | TBT Plus Navigation (`0xCC4D`) | Marquee Ticker Notifications | Handlebar Controls (`RCM / SCA`) | 6-Axis IMU (Lean / Pitch Angle) | Hydraulic Brake Pressure (`ABS Bar`) | Tire Pressure (`TPMS mbar`) | CUO Engine Map & WP Telemetry |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **KTM 1290 Super Adventure S / R / T** *(2017-2024+)* | 7" TFT / BCCU | ✅ **Fully Supported** | ✅ **Fully Supported** | ✅ **8 Buttons (Full Switchgear + C1/C2)** | ✅ **Bosch 6D IMU (0x12F)** | ✅ **Front Brake (0x290)** | ✅ **Front / Rear (0x310)** | ❌ *(Street Model)* |
| **KTM 1290 Super Duke R / GT & 1390 Super Duke** | 5"-7" TFT / BCCU | ✅ **Fully Supported** | ✅ **Fully Supported** | ✅ **8 Buttons (Full Switchgear)** | ✅ **Bosch 6D IMU (0x12F)** | ✅ **Front Brake (0x290)** | ✅ **GT Std / R Opt.** | ❌ *(Street Model)* |
| **KTM 890 / 790 Adventure (R/Rally) & Norden 901** | 5" TFT / BCCU | ✅ **Fully Supported** | ✅ **Fully Supported** | 🟡 **6 Buttons (SET/BACK/D-Pad)** | ✅ **Bosch 6D IMU (0x12F)** | 🟡 **Select Versions** | 🟡 **Optional** | ❌ *(Street Model)* |
| **KTM 890 / 790 / 990 Duke** | 5" TFT / BCCU | ✅ **Fully Supported** | ✅ **Fully Supported** | 🟡 **6 Buttons (SET/BACK/D-Pad)** | ✅ **Bosch 6D IMU (0x12F)** | 🟡 **Select Versions** | ❌ | ❌ *(Street Model)* |
| **KTM 390 Adventure / 250 Adventure** *(2020-2024+)* | 5" TFT / bCCU | ✅ **Supported** | ✅ **Supported** | 🟡 **4 Buttons (D-Pad)** | ❌ *(No IMU)* | ❌ | ❌ | ❌ *(Street Model)* |
| **KTM 125 / 250 / 390 Duke & RC 125/200/390** | TFT / bCCU | ✅ **Supported** | ✅ **Supported** | 🟡 **4 Buttons (D-Pad)** | ❌ *(No IMU)* | ❌ | ❌ | ❌ *(Street Model)* |
| **Husqvarna Svartpilen / Vitpilen 125/250/401/701** | TFT / bCCU | ✅ **Supported** | ✅ **Supported** | 🟡 **4 Buttons (D-Pad)** | 🟡 *(701 Only)* | ❌ | ❌ | ❌ *(Street Model)* |
| **KTM 250/350/450 SX-F, XC-F & Husq. FC/FX** | CUO Unit (No TFT) | ❌ *(No TFT Display)* | ❌ | ❌ | ✅ *(10Hz GNSS + IMU)* | ❌ | ❌ | ✅ **Fully Supported (Wi-Fi/BT)** |

---

## 2. Detailed Breakdown by Model Family

### 🏍️ Group 1: Flagship Travel & Street (KTM 1290 / 1390 Series)
*Source Code Identifiers: `DASH_Variant` 604, 607, 609, 614, 615, 616, 617, 619*

* **Covered Models:**
  - KTM 1290 Super Adventure S / R / T (2017 – 2024+)
  - KTM 1290 Super Duke R / Super Duke GT (2017 – 2024+)
  - KTM 1390 Super Duke R / Evo (2024+)
* **Hardware Architecture:** 7-inch large full-color TFT HMI, full-spec BCCU Bluetooth unit, Bosch 6-Axis Inertial Measurement Unit (IMU), Cornering ABS (C-ABS), Motorcycle Traction Control (MTC), Tire Pressure Monitoring System (TPMS).
* **Supported Capabilities:**
  - **TBT Plus & Marquee Ticker:** Complete 57 maneuver icons, distance countdown, street names, ETA, and incoming WhatsApp/Telegram/SMS ticker notifications on the large TFT display.
  - **8-Button Handlebar Switchgear (RCM):** `SET`, `BACK`, `UP`, `DOWN`, `LEFT`, `RIGHT`, and customizable **`C1`**, **`C2`** shortcut switches.
  - **Deep CAN Telemetry:**
    - `0x12F`: Dynamic Lean Angle (`ABS_Lean_Angle` - Degrees), Pitch Angle (`ABS_Pitch_Angle` - Wheelie / Stoppie detection).
    - `0x290`: Front Brake Hydraulic Pressure (`ABS_Front_Wheel_Press` - Bar).
    - `0x120` & `0x129`: Engine RPM, Throttle Position (TPS %), Selected Gear (`0=N`, `1..6`), Quickshifter+ status, Cruise Control target speed.
    - `0x310`: Front and Rear Tire Pressure (`TPMS_Pressure_Front/Rear` - mbar) and internal tire temperature (°C).

---

### 🧭 Group 2: Midweight Adventure & Naked (KTM 790 / 890 / 990 & Husqvarna 901)
*Source Code Identifiers: `DASH_Variant` 635, 636, 637, 641, Husqvarna Norden 901*

* **Covered Models:**
  - KTM 790 Adventure / 790 Adventure R / Rally
  - KTM 890 Adventure / 890 Adventure R / Rally
  - KTM 790 Duke / 890 Duke / 890 Duke R
  - KTM 990 Duke (2024+)
  - Husqvarna Norden 901 / Norden 901 Expedition
* **Hardware Architecture:** 5-inch TFT display, standard BCCU unit, Bosch 6D IMU, and C-ABS.
* **Supported Capabilities:**
  - **TBT Plus & Marquee Ticker:** Full KMRC navigation and message streaming on the 5-inch screen.
  - **Handlebar Controls:** The switchgear does not feature `C1/C2`; the 4-way D-Pad (`UP`, `DOWN`, `LEFT`, `RIGHT`) along with `SET` and `BACK` (6 buttons in total) are fully active.
  - **Telemetry:** Lean angle, pitch angle, TPS, RPM, speed, gear position, and coolant temperature are fully streamed.

---

### 🛵 Group 3: Light & Entry Street (KTM 125 / 250 / 390 Duke, RC & 390 Adventure)
*Source Code Identifiers: `DASH_Variant` 930, 933, 935, 937, 940, 943, 945, 946, 947, 948, 957, 958, 960, 962, 963, 281, 284, 285, 286, 288, 289*

* **Covered Models:**
  - KTM 390 Adventure / 250 Adventure (MY19+)
  - KTM 125 Duke / 250 Duke / 390 Duke (MY17+)
  - KTM RC 125 / RC 200 / RC 250 / RC 390 (MY16, MY21+)
  - Husqvarna Svartpilen 125/250/401 & Vitpilen 125/250/401
* **Hardware Architecture:** bCCU (Lite BLE unit) and TFT / LCD display. These models do **not** include a 6-axis IMU or hydraulic brake pressure sensor.
* **Supported Capabilities:**
  - **Navigation:** Turn-by-Turn arrows, distances, and street names are successfully displayed on the dash.
  - **Handlebar Controls:** 4-way D-Pad buttons provide media control (Play/Pause, track skip, volume).
  - **Telemetry:** Engine RPM, speed, gear position (`ECU_Gear_Position`), engine coolant temperature, and fuel level are streamed; **Lean angle and brake hydraulic pressure are unavailable due to lack of sensor hardware**.

---

### 🏁 Group 4: Motocross & Cross-Country (KTM SX-F / XC-F & Husqvarna FC / FX Series)
*Source Code Schema: `CUKTCUO_masterv91.00.json` & `bmdb-data/bikemodels`*

* **Covered Models:**
  - KTM 250 SX-F / 350 SX-F / 450 SX-F (2020 – 2024+)
  - KTM 250 XC-F / 350 XC-F / 450 XC-F (2021 – 2024+)
  - Husqvarna FC 250 / FC 350 / FC 450 & FX 350 / FX 450 (2020 – 2024+)
  - GASGAS MC 250F / MC 450F (Factory Edition)
* **Hardware Architecture:** CUO (Connectivity Unit Offroad) Wi-Fi & Bluetooth module. No dashboard display is installed on the motorcycle.
* **Special Capabilities:**
  - **10Hz High-Precision GNSS:** Lap timing, track line analytics, acceleration, and braking zone diagnostics.
  - **WP Suspension Telemetry:** Front Fork Air Pressure (ID `45` - mbar) and Rear Swingarm Sag Voltage (ID `46` - mV).
  - **Wireless ECU Engine Mapping:** Smartphone tuning for ignition timing (advance), fuel injection trims, Launch Control, and Quickshifter kill times (`qsIGCut`).

---

## 3. Hardware Architecture: BCCU vs bCCU vs CUO Comparison

| Feature / Component | BCCU (Street / Adventure / Travel) | bCCU (Duke 390 / RC 390 / Adv 390) | CUO (Offroad / Motocross / Enduro) |
| :--- | :---: | :---: | :---: |
| **Connectivity Type** | Bluetooth Classic (RFCOMM) + BLE | Bluetooth Low Energy (BLE) | Wi-Fi 2.4GHz + Bluetooth |
| **Primary Navigation Channel** | `EXTENDED_TBT` (Port 52301 / `0xCC4D`) | TBT GATT Service (`0x0700`) | None (No TFT Display) |
| **Map Streaming Support** | `MAPNAV_STREAMING` (Port 52410) | None | None |
| **Handlebar Key Interception**| SCA / RCM GATT (`0x0100` / `0x0103`) | SCA GATT (`0x0100`) | None (No Switchgear) |
| **Telemetry Bus** | CAN-Bus ➔ RFCOMM Port 52070 (pRPC) | CAN-Bus ➔ BLE GATT | Wi-Fi TCP Port 52070 + 10Hz GNSS |
| **IMU / Lean Angle** | Yes (Bosch 6D Sensor) | None | Yes (Track Analysis Sensor) |
| **ECU Map Tuning** | None (Factory Locked) | None (Factory Locked) | Yes (User Tunable Fuel / Ignition) |

---

## 4. `DASH_Variant` Source Code Reference Table

The following factory variant codes are decoded directly from `com.ktm.mob.can.messages.generated.DASH_Meter_Info_1104`:

```
[ID 1]  603 R - 1190 Adventure R
[ID 2]  603 T - 1190 Adventure
[ID 3]  604   - 1290 Super Adventure (MY15-16)
[ID 5]  614   - 1290 Super Duke GT
[ID 16] 616   - 1290 SuperDuke R (MY17)
[ID 17] 641   - 790 Duke
[ID 18] 607   - 1290 Adventure (MY17)
[ID 19] 607 R - 1290 Adventure R (MY17)
[ID 20] 609   - 1290 Super Adventure (MY18)
[ID 24] 635   - 790 Adventure
[ID 25] 635 R - 790 Adventure R
[ID 26] 615   - 1290 Super Adventure
[ID 27] 958   - 390 Adventure (MY19)
[ID 29] 617   - 1290 Super Duke R (MY20)
[ID 41] 637   - 890 Adventure S
[ID 42] 619   - 1290 Super Adventure S (MY21+)
[ID 43] 619 R - 1290 Super Adventure R (MY21+)
[ID 45] 637 R - 890 Adventure R
[ID 50] 636   - 890 Duke (MY20+)
[ID 51] 963   - 390 Duke / 960 - 125 Duke (MY24)
```

---

## 5. Summary & Key Takeaways for TbTKTM

1. **Universal TFT Support:** **TbTKTM** supports Turn-by-Turn navigation and Marquee Ticker notifications on **every single KTM and Husqvarna motorcycle equipped with a color TFT display (from 125cc to 1390cc)**.
2. **Maximum Hardware Exploitation:** Dedicated `C1/C2` shortcut switches, 6-axis IMU lean angle streaming, and front brake hydraulic pressure telemetry are available in their richest form on **KTM 1290 Super Adventure / Super Duke** and **890 Adventure / Norden 901** models.
