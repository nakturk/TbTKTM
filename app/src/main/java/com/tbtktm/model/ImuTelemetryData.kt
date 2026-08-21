package com.tbtktm.model

/**
 * Real-time 6-Axis IMU and CAN-Bus Telemetry Data for KTM 1290 Super Adventure
 */
data class ImuTelemetryData(
    val isConnected: Boolean = false,
    val currentLeanAngle: Float = 0.0f,         // Degrees (-: Left, +: Right)
    val maxLeftLeanAngle: Float = 0.0f,         // Peak Left Lean Angle (Degrees)
    val maxRightLeanAngle: Float = 0.0f,        // Peak Right Lean Angle (Degrees)
    val pitchAngle: Float = 0.0f,               // Degrees (+: Wheelie / Incline, -: Stoppie / Decline)
    val trajectoryAccelG: Float = 0.0f,         // G-Force (m/s² converted or raw)
    val frontBrakePressureBar: Float = 0.0f,    // Hydraulic Pressure (0.0 - 25.0 Bar)
    val throttlePositionPercent: Float = 0.0f,  // TPS (0.0% - 100.0%)
    val engineRpm: Int = 0,                     // Engine RPM
    val gear: Int = 0,                          // 0 = Neutral, 1..6 = Gears
    val speedKmh: Float = 0.0f,                 // Vehicle Speed (km/h)
    val coolantTempC: Float = 0.0f,             // Coolant Temperature (°C)
    val oilTempC: Int = 0,                      // Oil Temperature (°C)
    val frontTirePressureBar: Float = 0.0f,     // Front TPMS (Bar converted from mbar)
    val rearTirePressureBar: Float = 0.0f,      // Rear TPMS (Bar converted from mbar)
    val lastTimestamp: Long = 0L
)
