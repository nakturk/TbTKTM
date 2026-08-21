package com.tbtktm.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
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
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class KtmRfcommManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var activeSocket: BluetoothSocket? = null
    private var activeStream: OutputStream? = null
    private var lastConnectedAddress: String? = null
    private var isConnecting = false

    private val _connectedChannelsCount = MutableStateFlow(0)
    val connectedChannelsCount: StateFlow<Int> = _connectedChannelsCount.asStateFlow()

    // En son gönderilecek güncel navigasyon verisi
    private var pendingNavData: NavigationData? = null

    // KTM 1290 Extended TBT Kanalı (KTMconnect ile Birebir Aynı)
    val EXTENDED_TBT_UUID: UUID = UUID.fromString("cc4d1fb3-482e-4389-bdeb-57b7aac889ae")

    fun connect(deviceAddress: String, customPasswordOrVin: String? = null) {
        if (isConnecting) return
        lastConnectedAddress = deviceAddress

        if (activeStream != null && activeSocket?.isConnected == true) {
            FileLogger.log(">> RFCOMM zaten bağlı, yeniden bağlanmaya gerek yok.")
            return
        }

        disconnect()
        isConnecting = true

        scope.launch {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                isConnecting = false
                return@launch
            }

            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                FileLogger.log(">> KTM 1290 RFCOMM Bağlantısı Başlatılıyor (${device.name} - $deviceAddress)...")
                FileLogger.log(">> [EXTENDED_TBT (cc4d)] Kanalına bağlanılıyor...")

                val socket = device.createRfcommSocketToServiceRecord(EXTENDED_TBT_UUID)
                socket.connect()

                activeSocket = socket
                activeStream = socket.outputStream
                _connectedChannelsCount.value = 1
                isConnecting = false

                FileLogger.log(">> 🎉 [EXTENDED_TBT (cc4d)] BAŞARIYLA BAĞLANDI!")

                // 1. Dinleme Döngüsünü Başlat
                listenSocket(socket.inputStream)

                // 2. KTMconnect Birebir Başlangıç Paketi (Restore + KMRC Framing)
                val initialNav = pendingNavData ?: NavigationData(
                    turnIcon = KtmTurnIcon.QUITE_RIGHT,
                    distanceToTurn = "350 m",
                    roadName = "TbTKTM Ready to Race",
                    eta = "12:00",
                    distanceToDestination = "5.0 km",
                    isActive = true
                )
                
                val restoreJson = KtmProtoUtils.buildKmrcJsonNavigationMessage(initialNav, msgId = "Restore")
                val framedRestore = KtmProtoUtils.frameKmrcMessage(restoreJson)
                sendFramedBytes(framedRestore, "Restore KMRC Framed")

                // 3. 200ms sonra canlı manevra paketini bas
                delay(200)
                val mupJson = KtmProtoUtils.buildKmrcJsonNavigationMessage(initialNav, msgId = "mup")
                val framedMup = KtmProtoUtils.frameKmrcMessage(mupJson)
                sendFramedBytes(framedMup, "Live Maneuver Framed (mup)")

            } catch (e: Exception) {
                FileLogger.log("❌ RFCOMM Bağlantı Hatası: ${e.message}")
                _connectedChannelsCount.value = 0
                isConnecting = false
            }
        }
    }

    private fun listenSocket(inputStream: InputStream) {
        scope.launch {
            val dis = DataInputStream(inputStream)
            try {
                while (isActive) {
                    val len = dis.readInt()
                    val type = dis.readByte()
                    val data = ByteArray(len - 1)
                    dis.readFully(data)

                    if (type == 0x01.toByte()) {
                        val text = String(data, Charsets.UTF_8)
                        FileLogger.log("<< [KMRC JSON Gelen] $text")
                    }
                }
            } catch (e: Exception) {
                FileLogger.log("RFCOMM Dinleme Kapandı: ${e.message}")
                _connectedChannelsCount.value = 0
                activeStream = null
                activeSocket = null
            }
        }
    }

    fun sendNavigationUpdate(navData: NavigationData) {
        pendingNavData = navData

        scope.launch {
            if (activeStream == null || activeSocket?.isConnected != true) {
                FileLogger.log(">> RFCOMM bağlı değil, otomatik bağlanılıyor...")
                val target = lastConnectedAddress ?: KtmBleManager.getInstance(context).getLastConnectedAddress()
                if (target != null) {
                    connect(target)
                } else {
                    FileLogger.log("RFCOMM Gönderimi Atlandı: Kayıtlı motosiklet adresi yok")
                }
                return@launch
            }

            val kmrcJson = KtmProtoUtils.buildKmrcJsonNavigationMessage(navData, msgId = "mup")
            val framedMsg = KtmProtoUtils.frameKmrcMessage(kmrcJson)
            sendFramedBytes(framedMsg, "Live Maneuver Update (mup)")
        }
    }

    fun sendRawKmrcJson(jsonString: String, label: String = "Custom JSON") {
        scope.launch {
            if (activeStream == null || activeSocket?.isConnected != true) {
                val target = lastConnectedAddress ?: KtmBleManager.getInstance(context).getLastConnectedAddress()
                if (target != null) connect(target)
                return@launch
            }
            val framed = KtmProtoUtils.frameKmrcMessage(jsonString)
            sendFramedBytes(framed, label)
        }
    }

    private fun sendFramedBytes(framedBytes: ByteArray, label: String) {
        try {
            activeStream?.let { stream ->
                stream.write(framedBytes)
                stream.flush()
                FileLogger.log(">> [RFCOMM - $label] (len=${framedBytes.size}) Gönderildi!")
            }
        } catch (e: Exception) {
            FileLogger.log("❌ [$label] Gönderme Hatası: ${e.message}")
            _connectedChannelsCount.value = 0
            activeStream = null
            activeSocket = null
        }
    }

    fun disconnect() {
        try {
            activeStream?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        activeStream = null
        activeSocket = null
        _connectedChannelsCount.value = 0
        isConnecting = false
    }

    companion object {
        @Volatile
        private var instance: KtmRfcommManager? = null

        fun getInstance(context: Context): KtmRfcommManager {
            return instance ?: synchronized(this) {
                instance ?: KtmRfcommManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
