package com.tbtktm.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.IntentCompat
import com.tbtktm.util.FileLogger

class BluetoothStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val bleManager = KtmBleManager.getInstance(context)
        val savedAddress = bleManager.getLastConnectedAddress()

        if (savedAddress.isNullOrBlank()) return

        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    FileLogger.log(">> 📶 Bluetooth Açıldı -> Kayıtlı Motosiklete Otomatik Bağlanıyor: $savedAddress")
                    bleManager.autoConnect()
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val devAddress = device?.address
                val devName = device?.name ?: ""
                val isKtm = devName.contains("KTM", ignoreCase = true) || devName.contains("SPORTMOTORCYCLE", ignoreCase = true)

                if (devAddress?.equals(savedAddress, ignoreCase = true) == true || (isKtm && !devAddress.isNullOrBlank())) {
                    FileLogger.log(">> 🏍️ Motosiklet Bluetooth Bağlantısı Algılandı ($devName - $devAddress) -> Otomatik TbT Bağlanıyor...")
                    bleManager.connect(devAddress)
                }
            }
        }
    }
}
