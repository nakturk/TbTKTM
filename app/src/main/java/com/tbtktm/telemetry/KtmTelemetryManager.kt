package com.tbtktm.telemetry

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
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
 * KTM 1290 Super Adventure 6-Axis IMU & Telemetry Manager (pRPC over RFCOMM Port 52070).
 * Handles real-time Lean Angle, Pitch Angle, G-Force, Brake Hydraulic Pressure, and TPS streaming.
 */
@SuppressLint("MissingPermission")
class KtmTelemetryManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _telemetryState = MutableStateFlow(ImuTelemetryData())
    val telemetryState: StateFlow<ImuTelemetryData> = _telemetryState.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var activeStream: OutputStream? = null
    private var isConnecting = false
    private var simulationJob: Job? = null

    // KTM 1290 Telemetry Channel UUID Candidates (cb5c, cb66, cb2a, cb34)
    val TELEMETRY_CANDIDATE_UUIDS = listOf(
        UUID.fromString("cb5c1fb3-482e-4389-bdeb-57b7aac889ae"), // Port 52060 (KTM 1290 Stream)
        UUID.fromString("cb661fb3-482e-4389-bdeb-57b7aac889ae"), // Port 52070 (pRPC Telemetry)
        UUID.fromString("cb2a1fb3-482e-4389-bdeb-57b7aac889ae"), // Port 52010 (CCU Base)
        UUID.fromString("cb341fb3-482e-4389-bdeb-57b7aac889ae")  // Port 52020 (CCU Data)
    )

    fun connect(deviceAddress: String) {
        if (isConnecting) return
        if (activeSocket?.isConnected == true) {
            FileLogger.log(">> Telemetry kanalı zaten bağlı.")
            return
        }
        stopSimulation()
        disconnect()

        isConnecting = true
        scope.launch {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
                isConnecting = false
                return@launch
            }

            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            FileLogger.log(">> 📡 KTM 1290 Telemetry Bağlantısı Başlatılıyor (${device.name} - $deviceAddress)...")

            var connectedSocket: BluetoothSocket? = null
            for (uuid in TELEMETRY_CANDIDATE_UUIDS) {
                try {
                    FileLogger.log(">> 📡 Telemetry Kanalı Deneniyor: $uuid ...")
                    val socket = device.createRfcommSocketToServiceRecord(uuid)
                    socket.connect()
                    connectedSocket = socket
                    FileLogger.log(">> 🎉 Telemetry Kanalı BAĞLANDI: $uuid")
                    break
                } catch (e: Exception) {
                    FileLogger.log(">> ⚠️ Telemetry UUID ($uuid) bağlanamadı: ${e.message}")
                }
            }

            if (connectedSocket == null) {
                isConnecting = false
                _telemetryState.value = _telemetryState.value.copy(isConnected = false)
                FileLogger.log(">> ❌ Telemetry kanallarına bağlanılamadı!")
                return@launch
            }

            activeSocket = connectedSocket
            activeStream = connectedSocket.outputStream
            isConnecting = false
            _telemetryState.value = _telemetryState.value.copy(isConnected = true)

            // 1. Send Time Sync
            sendTimeSync()
            delay(100)

            // 2. Configure IMU & CAN subscriptions (20Hz = 50ms)
            configureDatapoint(DP_LEAN_ANGLE, 50)
            configureDatapoint(DP_PITCH_ANGLE, 50)
            configureDatapoint(DP_TRAJECTORY_ACCEL, 50)
            configureDatapoint(DP_FRONT_BRAKE_PRESS, 50)
            configureDatapoint(DP_THROTTLE_TPS, 50)
            configureDatapoint(DP_GEAR_POS, 100)
            configureDatapoint(DP_ENGINE_RPM, 100)
            configureDatapoint(DP_FRONT_SPEED, 100)
            configureDatapoint(DP_WATER_TEMP, 1000)
            configureDatapoint(DP_TPMS_FRONT, 2000)
            configureDatapoint(DP_TPMS_REAR, 2000)
            delay(100)

            // 3. Start Telemetry Streaming
            sendControlCommand(0) // Start
            FileLogger.log(">> 🚀 Telemetry Stream Başlatıldı (IMU 20Hz Aktif)")

            // 4. Ingest incoming pRPC stream
            listenTelemetryStream(connectedSocket.inputStream)
        }
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
                    // Unknown or skipped datapoint
                    offset += 1
                }
            }
        }

        _telemetryState.value = updatedState
    }

    private fun sendTimeSync() {
        val now = System.currentTimeMillis()
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 8).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte()) // SessionID
        buffer.put(10.toByte())   // Length
        buffer.putShort(2)        // Command 2: SetTime
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(now)
        sendRawBytes(buffer.array())
    }

    private fun configureDatapoint(datapointId: Short, sampleRateMs: Short) {
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 2 + 2).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte()) // SessionID
        buffer.put(6.toByte())    // Length
        buffer.putShort(3)        // Command 3: Configure
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(datapointId)
        buffer.putShort(sampleRateMs)
        sendRawBytes(buffer.array())
    }

    private fun sendControlCommand(cmd: Short) {
        val buffer = ByteBuffer.allocate(1 + 1 + 2 + 2).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte()) // SessionID
        buffer.put(4.toByte())    // Length
        buffer.putShort(4)        // Command 4: Control
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(cmd)      // 0 = Start, 1 = Pause, 2 = Stop
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
        _telemetryState.value = _telemetryState.value.copy(isConnected = true)
        simulationJob = scope.launch {
            var step = 0.0
            var currentGear = 3
            var maxL = 0f
            var maxR = 0f

            while (isActive) {
                step += 0.08
                // Sine wave lean angle between -48° and +45°
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

                delay(50) // 20Hz UI update
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
        _telemetryState.value = _telemetryState.value.copy(isConnected = false)
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
