package com.tbtktm.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.tbtktm.TbTApplication
import com.tbtktm.model.HandlebarButton
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
import com.tbtktm.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

@SuppressLint("MissingPermission")
class KtmBleManager private constructor(private val context: Context) {

    private val tag = "KtmBleManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefs: SharedPreferences = context.getSharedPreferences("tbtktm_ble_prefs", Context.MODE_PRIVATE)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    // Bluetooth Classic RFCOMM / SPP Soketi
    private var rfcommSocket: BluetoothSocket? = null
    private var rfcommOutputStream: OutputStream? = null
    private var rfcommInputStream: InputStream? = null

    // Bağlantı Durumu
    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    // Gidon Tuş Olayları
    private val _handlebarButtonEvents = MutableSharedFlow<Map<HandlebarButton, Boolean>>(extraBufferCapacity = 10)
    val handlebarButtonEvents: SharedFlow<Map<HandlebarButton, Boolean>> = _handlebarButtonEvents.asSharedFlow()

    // GATT Yazma Kuyruğu
    private val writeQueue: Queue<Pair<BluetoothGattCharacteristic, ByteArray>> = LinkedList()
    private var isWriting = false

    init {
        val savedAddress = getLastConnectedAddress()
        if (!savedAddress.isNullOrBlank()) {
            _connectedDeviceName.value = getLastConnectedName() ?: "KTM Motosiklet"
            FileLogger.log("Kayıtlı motosiklet: $savedAddress")
        }

        // Bluetooth SDP, ACL ve Eşleşme Alıcısı
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val target = getLastConnectedAddress()

                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val devAddress = dev?.address
                        val devName = dev?.name ?: ""
                        val isKtm = devName.contains("KTM", ignoreCase = true) || devName.contains("SPORTMOTORCYCLE", ignoreCase = true)
                        if (devAddress?.equals(target, ignoreCase = true) == true || (isKtm && !devAddress.isNullOrBlank())) {
                            FileLogger.log(">> 🏍️ Motosiklet ACL Bağlantısı Algılandı ($devName - $devAddress) -> Otomatik Bağlanıyor...")
                            connect(devAddress)
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_ON) {
                            autoConnect()
                        }
                    }
                    BluetoothDevice.ACTION_UUID -> {
                        if (dev != null && target != null && dev.address.equals(target, ignoreCase = true)) {
                            val uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                            FileLogger.log(">> SDP UUID SONUÇLARI GELDİ (${dev.name} - ${dev.address}):")
                            if (uuids != null) {
                                for (u in uuids) {
                                    val parcelUuid = u as? ParcelUuid
                                    FileLogger.log("    [SDP UUID] ${parcelUuid?.uuid}")
                                }
                            } else {
                                FileLogger.log("    (SDP UUID boş)")
                            }
                        }
                    }
                }
            }
        }, filter)

        // Uygulama başlatıldığında kayıtlı motosiklet varsa otomatik bağlanmayı dene
        if (!savedAddress.isNullOrBlank()) {
            mainHandler.postDelayed({
                autoConnect()
            }, 1000)
        }
    }

    /**
     * Kayıtlı motosiklet varsa ve şu anda bağlı değilse otomatik olarak bağlantıyı başlatır.
     */
    fun autoConnect() {
        val savedAddress = getLastConnectedAddress()
        if (savedAddress.isNullOrBlank()) return
        if (_connectionState.value == BluetoothProfile.STATE_CONNECTED) return
        if (KtmRfcommManager.getInstance(context).connectedChannelsCount.value > 0) return

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) return

        FileLogger.log(">> 🔄 Kayıtlı Motosiklete Otomatik Bağlanılıyor: $savedAddress")
        connect(savedAddress)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            _connectionState.value = newState
            val statusMsg = if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS (0)" else "Status: $status"
            FileLogger.log("--> onConnectionStateChange: newState=${getStateName(newState)}, $statusMsg")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val name = gatt.device.name ?: getLastConnectedName() ?: gatt.device.address
                _connectedDeviceName.value = name
                saveLastConnectedDevice(gatt.device.address, name)

                FileLogger.log("Motosiklete Bağlandı: $name (${gatt.device.address})")

                // Bluetooth Classic RFCOMM Soket bağlantısını başlat!
                KtmRfcommManager.getInstance(context).connect(gatt.device.address)

                // SDP ile Classic servis UUID'lerini iste
                FileLogger.log(">> Motosikletin Bluetooth Classic / SDP UUID'leri isteniyor (fetchUuidsWithSdp)...")
                gatt.device.fetchUuidsWithSdp()

                mainHandler.postDelayed({
                    gatt.discoverServices()
                }, 400)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                FileLogger.log("Bağlantı Kesildi")
                KtmRfcommManager.getInstance(context).disconnect()
                com.tbtktm.telemetry.KtmTelemetryManager.getInstance(context).disconnect()
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                FileLogger.log("--> onServicesDiscovered: Bulunan Servis Sayısı: ${gatt.services.size}")
                for (s in gatt.services) {
                    FileLogger.log("  [GATT SERVIS] ${s.uuid}")
                    for (ch in s.characteristics) {
                        FileLogger.log("    [CHAR] ${ch.uuid}")
                    }
                }

                // 1. TBT Yetkilendirme İsteği Dinleyicisini Aç (0x0701)
                val tbtService = gatt.getService(UUID.fromString("71ced1ac-0700-44f5-9454-806ff70b3e02"))
                if (tbtService != null) {
                    val authReqChar = tbtService.getCharacteristic(UUID.fromString("71ced1ac-0701-44f5-9454-806ff70b3e02"))
                    if (authReqChar != null) {
                        gatt.setCharacteristicNotification(authReqChar, true)
                        val cccd = authReqChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (cccd != null) {
                            cccd.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                            gatt.writeDescriptor(cccd)
                            FileLogger.log(">> [BCCU AUTH] 0x0701 TBT_AUTH_REQUESTS Indication Başarıyla Açıldı!")
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val uuid = characteristic.uuid.toString()
            val value = characteristic.value ?: return

            if (uuid.equals("71ced1ac-0701-44f5-9454-806ff70b3e02", ignoreCase = true)) {
                FileLogger.log(">> [GATT 0x0701 ALINDI] Motosiklet Auth Talebi Geldi!")
                val response = KtmBccuAuthManager.handleIncomingAuthMessage(value)
                if (response != null) {
                    val tbtService = gatt.getService(UUID.fromString("71ced1ac-0700-44f5-9454-806ff70b3e02"))
                    val replyChar = tbtService?.getCharacteristic(UUID.fromString("71ced1ac-0702-44f5-9454-806ff70b3e02"))
                    if (replyChar != null) {
                        replyChar.value = response
                        gatt.writeCharacteristic(replyChar)
                        FileLogger.log(">> [GATT 0x0702 YAZILDI] BCCU Auth Yanıtı Motosiklete İletildi!")
                    }
                }
            }
        }
    }

    fun connect(deviceAddress: String) {
        closeGatt()
        val adapter = bluetoothAdapter ?: return
        val device = adapter.getRemoteDevice(deviceAddress)
        val devName = device.name ?: deviceAddress
        _connectedDeviceName.value = devName
        saveLastConnectedDevice(device.address, devName)

        _connectionState.value = BluetoothProfile.STATE_CONNECTING
        FileLogger.log("Bağlantı Başlatılıyor: $devName ($deviceAddress)")

        // 1. Bluetooth Classic SDP sorgusu yap
        FileLogger.log("fetchUuidsWithSdp çağrılıyor...")
        device.fetchUuidsWithSdp()

        // 2. GATT bağlantısı başlat
        mainHandler.post {
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO)
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        closeGatt()
        _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
    }

    private fun closeGatt() {
        try {
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
    }

    private fun getStateName(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED (2)"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING (1)"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED (0)"
            else -> "STATE_$state"
        }
    }

    private fun saveLastConnectedDevice(address: String, name: String) {
        prefs.edit()
            .putString("last_device_address", address)
            .putString("last_device_name", name)
            .apply()
    }

    fun getLastConnectedAddress(): String? = prefs.getString("last_device_address", null)
    fun getLastConnectedName(): String? = prefs.getString("last_device_name", null)

    fun sendNavigationUpdate(navData: NavigationData) {
        FileLogger.log(">> Navigasyon Güncellemesi: ${navData.turnIcon.description} | ${navData.distanceToTurn} | ${navData.roadName}")
        
        // Bluetooth Classic RFCOMM kanalına gönder
        KtmRfcommManager.getInstance(context).sendNavigationUpdate(navData)
    }

    fun sendRawKmrcJson(jsonString: String) {
        KtmRfcommManager.getInstance(context).sendRawKmrcJson(jsonString, "Ticker Frame")
    }

    companion object {
        @Volatile
        private var instance: KtmBleManager? = null

        fun getInstance(context: Context): KtmBleManager {
            return instance ?: synchronized(this) {
                instance ?: KtmBleManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
