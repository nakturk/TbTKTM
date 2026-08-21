# 🏍️ TbTKTM - KTM Turn-by-Turn Navigation & TFT Dashboard Controller

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%20%2F%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Protocol-KTM%20KMRC%20%2F%20RFCOMM%20%2F%20BLE-FF6600?style=for-the-badge" alt="KTM Protocol" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License" />
</p>

**TbTKTM** is an open-source Android application designed to intercept real-time turn-by-turn routing data from third-party navigation apps (**Google Maps**, **Yandex Navigation**, etc.) and project live maneuver arrows, distance countdowns, street names, and ETA directly onto the color TFT dashboard of **KTM 1290 Super Adventure** (and all Euro 5 BCCU-equipped KTM / Husqvarna / GASGAS motorcycles). It also streams incoming notifications as a horizontal **Marquee Ticker** and maps the motorcycle's **8-way left handlebar switchgear** (SET, BACK, UP, DOWN, LEFT, RIGHT, C1, C2) to smartphone controls (Music, Volume, Voice Assistant).

---

## ✨ Key Features

### 🧭 1. Advanced Navigation Integration (TBT Plus)
- **Google Maps & Yandex Navigation Support:** The background `UniversalNavParser` dynamically parses active navigation notifications to extract maneuver icons, distance to next turn, street/road names, Estimated Time of Arrival (ETA), and total remaining distance.
- **57 Official KTM Maneuver Icons:** Full support for KTM's built-in icon set (`KtmTurnIcon`), including roundabout exits (1-8), highway on/off-ramps, sharp/slight turns, and U-turns.
- **Smart Text Cleaning:** Cleans up directional suffixes and prepositions to ensure maximum legibility on the TFT cluster.

### 📜 2. TFT Horizontal Marquee Notification System
- **Supported Apps:** WhatsApp, Telegram, SMS, Gmail, Yahoo Mail, and Outlook.
- **Dynamic Marquee Ticker:** Incoming messages scroll across the TFT display within a 32-character sliding window at 800ms intervals.
- **Safety Prioritization:** If a critical turn maneuver occurs while a message is scrolling, the notification is immediately paused/dismissed to prioritize navigation instructions; the display automatically returns to the map/idle state once finished.

### 🌍 3. 5-Language Internationalization (i18n)
- **Supported Languages:** Turkish (🇹🇷), English (🇬🇧), Italian (🇮🇹), Spanish (🇪🇸), Greek (🇬🇷).
- Maneuver descriptions and HMI strings are dynamically translated and transmitted to the cluster in the selected language.

### 🕹️ 4. 8-Button Handlebar Switchgear Management (RCM)
- Intercepts presses from **`SET`**, **`BACK`**, **`UP`**, **`DOWN`**, **`LEFT`**, **`RIGHT`**, **`C1`**, and **`C2`** over Bluetooth.
- **Customizable Actions:**
  - ⏯️ Music Play / Pause
  - ⏭️ / ⏮️ Next / Previous Track
  - 🔊 / 🔉 Volume Up / Down
  - 🎙️ Google Voice Assistant
  - 🗺️ Recenter Navigation Map

### ⚡ 5. Native KMRC & RFCOMM Protocol Support
- **Hardware-Verified Architecture:** Full duplex communication via the official `EXTENDED_TBT` RFCOMM channel (`0xCC4D` / `cc4d1fb3-482e-4389-bdeb-57b7aac889ae`).
- **5-Byte KMRC Transport Framing:** Robust socket streaming using `[4-Byte Big-Endian Length] + [1-Byte MsgType (0x01)] + [JSON Payload]`.
- **Auto-Reconnect Loop:** `BluetoothStateReceiver` automatically recovers connection and re-establishes the handshake when the motorcycle ignition is turned on or enters range.

### 🖥️ 6. Live TFT Dashboard Simulator
- Built-in graphical KTM TFT HMI simulator and virtual motorcycle emulator (`KtmBccuSimulator`) for testing UI rendering and protocol packet flow without needing the physical motorcycle nearby.

---

## 🏗️ Project Architecture

```text
com.tbtktm/
├── ble/                                # Bluetooth & Hardware Communication Layer
│   ├── KtmGattAttributes.kt            # BLE & RFCOMM Channel UUIDs (0x0700, 0x0100, CC4D, etc.)
│   ├── KtmProtoUtils.kt                # KMRC JSON Builder & 5-byte Transport Frame Encoder
│   ├── KtmFramingUtils.kt              # Salt + Padding BLE Framing Algorithm
│   ├── KtmRfcommManager.kt             # Bluetooth Classic RFCOMM (EXTENDED_TBT) Socket Manager
│   ├── KtmBleManager.kt                # BLE GATT Connection, Notification Subscriptions & Queue
│   ├── KtmBleService.kt                # Persistent Foreground Service
│   ├── KtmBccuSimulator.kt             # Virtual Motorcycle Simulator for Testing
│   ├── KtmBccuAuthManager.kt           # BCCU Handshake & Authentication Manager
│   └── BluetoothStateReceiver.kt       # System Bluetooth State Listener & Auto-Reconnector
├── parser/                             # Notification Listener & Parsing Engine
│   ├── NotificationParserService.kt    # Android NotificationListenerService Base
│   ├── UniversalNavParser.kt           # Google Maps & Yandex Navigation Regex Parser
│   ├── AppNotificationParser.kt        # WhatsApp, Telegram, Mail & SMS Message Parser
│   ├── GoogleMapsParser.kt             # Google Maps Parser
│   ├── YandexNaviParser.kt             # Yandex Navigation Parser
│   └── IconMatcher.kt                  # Text-to-KTM 57 Icon ID Matching Engine
├── rcm/                                # Handlebar Remote Control Module
│   ├── HandlebarKeyManager.kt          # 8-Way Button Event Dispatcher
│   └── ActionDispatcher.kt             # Media, Volume & Assistant Intent Trigger
├── ticker/                             # Notification Ticker Engine
│   └── TftMarqueeTicker.kt             # 32-Character Sliding Window & Priority Manager
├── i18n/                               # Localization Layer
│   ├── AppLanguage.kt                  # Supported Languages Enum (TR, EN, IT, ES, EL)
│   └── AppLanguageManager.kt           # Global Language State Manager
├── model/                              # Data Models
│   ├── KtmTurnIcon.kt                  # 57 Maneuver Icons with 5-Language Descriptions
│   ├── HandlebarButton.kt              # Handlebar Buttons & Assignable Action Models
│   └── NavigationData.kt               # Live Navigation & Telemetry State Model
├── util/                               # Utilities
│   └── FileLogger.kt                   # Live Diagnostics & File Logger
└── ui/                                 # Jetpack Compose UI
    ├── screens/
    │   ├── DashboardScreen.kt          # Live Cluster Preview, Simulator & Language Picker
    │   ├── DeviceScanScreen.kt         # BLE / BT Classic Device Discovery & Pairing
    │   └── KeyMappingScreen.kt         # Handlebar Button Customization Menu
    ├── components/
    │   └── TftDashboardCard.kt         # KTM TFT HMI Dashboard Component
    └── theme/                          # KTM Ready-to-Race Theme & Color Palette
```

---

## 📡 Protocol & Technical Documentation

For in-depth reverse-engineered protocol documentation and model capabilities:
- [Protocol_KTM.MD](file:///home/nakturk/Projects/KTMconnect_4.0.2.2026061701-release_APKPure/TbTKTM/Protocol_KTM.MD) — Full KMRC Transport framing, binary pRPC telemetry specifications, and CAN-bus arbitration catalogs.
- [models_capability.MD](file:///home/nakturk/Projects/KTMconnect_4.0.2.2026061701-release_APKPure/TbTKTM/models_capability.MD) — Motorcycle model compatibility and hardware matrix across KTM and Husqvarna lineups.

### KMRC Packet Structure:
```
+--------------------------+--------------------+---------------------------------------+
|  Length (4 Bytes Int32)  | MsgType (1 Byte)   |          JSON Payload (UTF-8)         |
|       Big-Endian         |       0x01         |     {"GuidanceUpdate": {...}, ...}    |
+--------------------------+--------------------+---------------------------------------+
|<----------------- 5 Bytes Header ------------>|<------------- N Bytes --------------->|
```

### Primary RFCOMM Channels:
| Channel | Port (Hex) | RFCOMM Service UUID | Description |
| :--- | :--- | :--- | :--- |
| **`EXTENDED_TBT`** | `0xCC4D` | `cc4d1fb3-482e-4389-bdeb-57b7aac889ae` | Turn-by-Turn Navigation & Marquee Notifications |
| **`CCUBASE`** | `0xCB2A` | `cb2a1fb3-482e-4389-bdeb-57b7aac889ae` | Base HMI, Device State & Login Handshake |
| **`TELEMETRY`** | `0xCB66` | `cb661fb3-482e-4389-bdeb-57b7aac889ae` | Real-time Sensor & CAN-Bus Telemetry Stream |

---

## 📲 Requirements & Installation

### Requirements
- **Android Version:** Android 8.0 (API 26) or higher (Fully compatible with Android 12, 13, and 14).
- **IDE:** Android Studio (Hedgehog / Iguana / Jellyfish or newer).
- **Build Stack:** Kotlin 1.9+, Jetpack Compose (BOM 2024+), Gradle 8+.

### Required Android Permissions
1. **Bluetooth & Location:** Required for scanning, connecting, and discovering peripheral motorcycle devices (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`).
2. **Notification Access:**
   - Go to *Settings ➔ Apps ➔ Special App Access ➔ Notification Access* and enable **TbTKTM** (required to read navigation and messaging notifications).
3. **Disable Battery Optimization:** Set battery usage to "Unrestricted" to maintain a seamless background connection during rides.

---

## 🚀 Quick Start

1. Clone the repository and build it on your Android device:
   ```bash
   git clone https://github.com/nakturk/TbTKTM.git
   ```
2. Open **TbTKTM**, navigate to the **Motorcycle** tab, and pair with your bike.
3. Switch to the **Handlebar** tab to customize actions for your switchgear buttons.
4. Start navigation in **Google Maps** or **Yandex Navigation**.
5. Enjoy real-time turn arrows, street names, distance countdowns, and marquee notifications on your KTM TFT cluster! 🏍️💨

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
KTM® is a registered trademark of KTM Sportmotorcycle GmbH. This project is not affiliated with or endorsed by KTM AG.
