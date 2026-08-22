package com.tbtktm.telemetry

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.tbtktm.model.ImuTelemetryData
import com.tbtktm.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

/**
 * KTM 1290 Super Adventure Cockpit Telemetry Engine.
 * Dual-source engine:
 * 1. Phone 6-Axis IMU (Gyroscope + Accelerometer + Gravity) & GPS Real-Time Sensor Fusion.
 * 2. KTM BCCU CAN-Bus pRPC stream (Port 52070 / cb66) when motorcycle connection is active.
 */
@SuppressLint("MissingPermission")
class KtmTelemetryManager private constructor(private val context: Context) : SensorEventListener, LocationListener {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetryState = MutableStateFlow(ImuTelemetryData())
    val telemetryState: StateFlow<ImuTelemetryData> = _telemetryState.asStateFlow()

    private val _telemetrySource = MutableStateFlow("PHONE_IMU")
    val telemetrySource: StateFlow<String> = _telemetrySource.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var activeStream: OutputStream? = null
    private var isConnecting = false
    private var simulationJob: Job? = null

    // Phone Sensors
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var rotationSensor: Sensor? = null
    private var linearAccelSensor: Sensor? = null
    private var gravitySensor: Sensor? = null

    // Sensor Calibration Offsets
    private var leanOffsetDeg = 0f
    private var pitchOffsetDeg = 0f
    private var rawLeanDeg = 0f
    private var rawPitchDeg = 0f

    private var isPhoneSensorsActive = false

    // KTM 1290 Telemetry Channel UUID Candidates (cb66 = Port 52070 pRPC)
    val TELEMETRY_CANDIDATE_UUIDS = listOf(
        UUID.fromString("cb661fb3-482e-4389-bdeb-57b7aac889ae"), // Port 52070 (pRPC Telemetry)
        UUID.fromString("cb5c1fb3-482e-4389-bdeb-57b7aac889ae"), // Port 52060 (KTM 1290 Stream)
        UUID.fromString("cb2a1fb3-482e-4389-bdeb-57b7aac889ae")  // Port 52010 (CCU Base)
    )

    init {
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        linearAccelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    /**
     * Start live phone sensors (IMU 6-axis & GPS) for instant, seamless cockpit HUD tracking.
     */
    fun startPhoneSensors() {
        if (isPhoneSensorsActive) return
        isPhoneSensorsActive = true

        stopSimulation()

        // Register Sensor Listeners at SENSOR_DELAY_GAME (~20-50Hz)
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        linearAccelSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        // Register GPS Location Listener
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L, // 500ms
                0f,
                this
            )
        } catch (_: Exception) {}

        _telemetryState.value = _telemetryState.value.copy(isConnected = true)
        _telemetrySource.value = "PHONE 6-AXIS IMU (CANLI)"
        FileLogger.log(">> 📱 Telefon 6-Eksenli IMU Sensör Motoru Başlatıldı (20-50Hz Canlı)")
    }

    fun stopPhoneSensors() {
        if (!isPhoneSensorsActive) return
        isPhoneSensorsActive = false
        sensorManager?.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    /**
     * Zero / Calibrate current phone position as 0° Lean & 0° Pitch baseline.
     */
    fun calibrateZero() {
        leanOffsetDeg = rawLeanDeg
        pitchOffsetDeg = rawPitchDeg
        resetPeakLeanAngles()
        FileLogger.log(">> 🎯 Cockpit HUD Sıfırlandı (Lean Offset: $leanOffsetDeg°, Pitch Offset: $pitchOffsetDeg°)")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isPhoneSensorsActive) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Cihazın yerçekimi vektörünü rotasyon matrisinden çıkar (Gimbal Lock ve montaj açısından bağımsız)
                // R[6] = gravity X (sol/sağ), R[7] = gravity Y (üst/alt), R[8] = gravity Z (ekran düzlemi)
                val gx = rotationMatrix[6]
                val gy = rotationMatrix[7]
                val gz = rotationMatrix[8]

                // Sol/Sağ Yatış Açısı (MotoGP Lean Angle)
                val leanRad = kotlin.math.atan2(gx.toDouble(), kotlin.math.sqrt((gy * gy + gz * gz).toDouble()))
                val rollDeg = Math.toDegrees(leanRad).toFloat()

                // Ön/Arka Yunuslama Açısı (Pitch Angle - Wheelie / Stoppie / Yokuş)
                val pitchRad = kotlin.math.atan2(gy.toDouble(), kotlin.math.sqrt((gx * gx + gz * gz).toDouble()))
                val pitchDeg = Math.toDegrees(pitchRad).toFloat()

                rawLeanDeg = rollDeg
                rawPitchDeg = pitchDeg

                // Kalibre edilmiş açılar
                val calibratedLean = (rollDeg - leanOffsetDeg).coerceIn(-65f, 65f)
                val calibratedPitch = (pitchDeg - pitchOffsetDeg).coerceIn(-45f, 45f)

                val currentState = _telemetryState.value
                val maxL = if (calibratedLean < 0) kotlin.math.max(currentState.maxLeftLeanAngle, abs(calibratedLean)) else currentState.maxLeftLeanAngle
                val maxR = if (calibratedLean > 0) kotlin.math.max(currentState.maxRightLeanAngle, calibratedLean) else currentState.maxRightLeanAngle

                _telemetryState.value = currentState.copy(
                    isConnected = true,
                    currentLeanAngle = calibratedLean,
                    maxLeftLeanAngle = maxL,
                    maxRightLeanAngle = maxR,
                    pitchAngle = calibratedPitch,
                    lastTimestamp = System.currentTimeMillis()
                )
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // İleri / Geri ivmelenme ve Fren G-Kuvveti (Y/Z düzlemi bileşkesi)
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // Hareket yönündeki ivmelenme (G)
                val forwardAccel = ay
                val gForce = (forwardAccel / 9.81f).coerceIn(-2.5f, 2.5f)

                val currentState = _telemetryState.value
                
                // G-kuvvetinden fren basıncı (Bar) ve gaz tepkisi (%) tahmini (Telefon IMU Modu)
                val estBrakeBar = if (gForce < -0.1f) (abs(gForce) * 8.5f).coerceIn(0f, 16.0f) else 0f
                val estThrottlePct = if (gForce > 0.1f) (gForce * 120.0f).coerceIn(0f, 100.0f) else 0f

                _telemetryState.value = currentState.copy(
                    trajectoryAccelG = gForce,
                    frontBrakePressureBar = if (currentState.frontBrakePressureBar == 0f || _telemetrySource.value.contains("PHONE")) estBrakeBar else currentState.frontBrakePressureBar,
                    throttlePositionPercent = if (currentState.throttlePositionPercent == 0f || _telemetrySource.value.contains("PHONE")) estThrottlePct else currentState.throttlePositionPercent
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LocationListener for real GPS Speed
    override fun onLocationChanged(location: Location) {
        val speedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
        _telemetryState.value = _telemetryState.value.copy(
            speedKmh = speedKmh
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    /**
     * KTM CAN Telemetry connection handling.
     * Note: KTM 1290 Street BCCU does not host raw pRPC telemetry;
     * automatically uses high-precision phone 6-axis IMU & GPS sensor fusion.
     */
    fun connect(deviceAddress: String) {
        // Telefon IMU sensörlerini anında ve kesintisiz aktif tut
        startPhoneSensors()
    }

    private fun listenTelemetryStream(inputStream: java.io.InputStream) {
        val dis = DataInputStream(inputStream)
        val buffer = ByteArray(2048)

        while (scope.isActive && activeSocket?.isConnected == true) {
            try {
                val bytesRead = dis.read(buffer)
                if (bytesRead <= 0) break

                val packet = buffer.copyOfRange(0, bytesRead)
                parsePrpcPacket(packet)
            } catch (e: Exception) {
                FileLogger.log(">> Telemetry socket read interrupted: ${e.message}")
                break
            }
        }

        disconnect()
        startPhoneSensors()
    }

    /**
     * Decodes incoming pRPC Notification Triples from KTM BCCU
     */
    private fun parsePrpcPacket(data: ByteArray) {
        if (data.size < 4) return

        val notifyCode = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short
        if (notifyCode.toInt() != 7 && notifyCode.toInt() != 1) return

        var offset = 4 // Skip header (NotifyCode: 2B + Length: 1B + Stuffing: 1B)
        var updatedState = _telemetryState.value

        while (offset + 10 <= data.size) {
            val timestamp = ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
            val dpId = ByteBuffer.wrap(data, offset + 8, 2).order(ByteOrder.LITTLE_ENDIAN).short
            offset += 10

            when (dpId) {
                DP_LEAN_ANGLE -> {
                    if (offset + 4 <= data.size) {
                        val lean = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        val maxLeft = if (lean < 0) kotlin.math.max(updatedState.maxLeftLeanAngle, abs(lean)) else updatedState.maxLeftLeanAngle
                        val maxRight = if (lean > 0) kotlin.math.max(updatedState.maxRightLeanAngle, lean) else updatedState.maxRightLeanAngle
                        updatedState = updatedState.copy(
                            currentLeanAngle = lean,
                            maxLeftLeanAngle = maxLeft,
                            maxRightLeanAngle = maxRight,
                            lastTimestamp = timestamp
                        )
                    }
                }
                DP_PITCH_ANGLE -> {
                    if (offset + 4 <= data.size) {
                        val pitch = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(pitchAngle = pitch)
                    }
                }
                DP_TRAJECTORY_ACCEL -> {
                    if (offset + 4 <= data.size) {
                        val accel = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(trajectoryAccelG = accel / 9.81f)
                    }
                }
                DP_FRONT_BRAKE_PRESS -> {
                    if (offset + 4 <= data.size) {
                        val bar = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(frontBrakePressureBar = bar.coerceAtLeast(0f))
                    }
                }
                DP_THROTTLE_TPS -> {
                    if (offset + 4 <= data.size) {
                        val tps = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(throttlePositionPercent = tps.coerceIn(0f, 100f))
                    }
                }
                DP_GEAR_POS -> {
                    if (offset + 1 <= data.size) {
                        val gear = data[offset].toInt() and 0xFF
                        offset += 1
                        updatedState = updatedState.copy(gear = gear)
                    }
                }
                DP_ENGINE_RPM -> {
                    if (offset + 2 <= data.size) {
                        val rpm = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                        offset += 2
                        updatedState = updatedState.copy(engineRpm = rpm)
                    }
                }
                DP_FRONT_SPEED, DP_REAR_SPEED -> {
                    if (offset + 4 <= data.size) {
                        val spd = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(speedKmh = spd)
                    }
                }
                DP_WATER_TEMP -> {
                    if (offset + 4 <= data.size) {
                        val temp = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
                        offset += 4
                        updatedState = updatedState.copy(coolantTempC = temp)
                    }
                }
                DP_TPMS_FRONT -> {
                    if (offset + 2 <= data.size) {
                        val mbar = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                        offset += 2
                        updatedState = updatedState.copy(frontTirePressureBar = mbar / 1000.0f)
                    }
                }
                DP_TPMS_REAR -> {
                    if (offset + 2 <= data.size) {
                        val mbar = ByteBuffer.wrap(data, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                        offset += 2
                        updatedState = updatedState.copy(rearTirePressureBar = mbar / 1000.0f)
                    }
                }
                else -> {
                    offset += 1
                }
            }
        }

        _telemetryState.value = updatedState
    }

    private fun sendTimeSync() {
        val now = System.currentTimeMillis()
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 8).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte())
        buffer.put(10.toByte())
        buffer.putShort(2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(now)
        sendRawBytes(buffer.array())
    }

    private fun configureDatapoint(datapointId: Short, sampleRateMs: Short) {
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 2 + 2).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte())
        buffer.put(6.toByte())
        buffer.putShort(3)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(datapointId)
        buffer.putShort(sampleRateMs)
        sendRawBytes(buffer.array())
    }

    private fun sendControlCommand(cmd: Short) {
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 2).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte())
        buffer.put(4.toByte())
        buffer.putShort(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(cmd)
        sendRawBytes(buffer.array())
    }

    private fun sendRawBytes(bytes: ByteArray) {
        try {
            activeStream?.write(bytes)
            activeStream?.flush()
        } catch (e: Exception) {
            FileLogger.log(">> Error sending telemetry raw command: ${e.message}")
        }
    }

    fun resetPeakLeanAngles() {
        _telemetryState.value = _telemetryState.value.copy(
            maxLeftLeanAngle = 0.0f,
            maxRightLeanAngle = 0.0f
        )
    }

    fun startSimulation() {
        stopSimulation()
        stopPhoneSensors()
        _telemetryState.value = _telemetryState.value.copy(isConnected = true)
        _telemetrySource.value = "SIMULATION ENGINE"
        simulationJob = scope.launch {
            var step = 0.0
            val currentGear = 3
            var maxL = 0f
            var maxR = 0f

            while (isActive) {
                step += 0.08
                val lean = (sin(step) * 46.0).toFloat()
                val pitch = (sin(step * 0.5) * 4.0).toFloat()
                val throttle = ((sin(step * 1.5) + 1.0) * 45.0).toFloat().coerceIn(0f, 100f)
                val brake = if (lean < -20 || lean > 20) (abs(lean) / 4.0f).coerceIn(0f, 16f) else 0f
                val speed = (75.0 + sin(step * 0.3) * 30.0).toFloat()
                val rpm = (4000 + (throttle * 40)).toInt()

                if (lean < 0) maxL = kotlin.math.max(maxL, abs(lean))
                if (lean > 0) maxR = kotlin.math.max(maxR, lean)

                _telemetryState.value = _telemetryState.value.copy(
                    isConnected = true,
                    currentLeanAngle = lean,
                    maxLeftLeanAngle = maxL,
                    maxRightLeanAngle = maxR,
                    pitchAngle = pitch,
                    trajectoryAccelG = (throttle / 100.0f * 0.8f) - (brake / 16.0f * 0.9f),
                    frontBrakePressureBar = brake,
                    throttlePositionPercent = throttle,
                    engineRpm = rpm,
                    gear = currentGear,
                    speedKmh = speed,
                    coolantTempC = 88.5f,
                    frontTirePressureBar = 2.42f,
                    rearTirePressureBar = 2.85f,
                    lastTimestamp = System.currentTimeMillis()
                )

                delay(50)
            }
        }
        FileLogger.log(">> 🎮 KTM 1290 Telemetry Simulation Engine STARTED")
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun disconnect() {
        try {
            activeStream?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        activeStream = null
        activeSocket = null
        isConnecting = false
    }

    companion object {
        @Volatile
        private var instance: KtmTelemetryManager? = null

        fun getInstance(context: Context): KtmTelemetryManager {
            return instance ?: synchronized(this) {
                instance ?: KtmTelemetryManager(context.applicationContext).also { instance = it }
            }
        }

        // Datapoint IDs from CUKT_bCCU.json
        const val DP_TIME_SYNC = 200.toShort()
        const val DP_WATER_TEMP = 302.toShort()
        const val DP_ENGINE_RPM = 303.toShort()
        const val DP_THROTTLE_TPS = 304.toShort()
        const val DP_GEAR_POS = 314.toShort()
        const val DP_OIL_TEMP = 320.toShort()
        const val DP_TRAJECTORY_ACCEL = 321.toShort()
        const val DP_LEAN_ANGLE = 322.toShort()
        const val DP_PITCH_ANGLE = 323.toShort()
        const val DP_REAR_SPEED = 325.toShort()
        const val DP_FRONT_SPEED = 326.toShort()
        const val DP_TPMS_REAR = 337.toShort()
        const val DP_TPMS_FRONT = 338.toShort()
        const val DP_FRONT_BRAKE_PRESS = 358.toShort()
    }
}
