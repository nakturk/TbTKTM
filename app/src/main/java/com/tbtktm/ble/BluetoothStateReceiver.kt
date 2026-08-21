package com.tbtktm.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothStateReceiver : BroadcastReceiver() {

    private val tag = "BluetoothStateReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val bleManager = KtmBleManager.getInstance(context)
        val savedAddress = bleManager.getLastConnectedAddress()

        if (savedAddress.isNullOrBlank()) return

        when (action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(tag, "Bluetooth Açıldı -> Bağlantı deneniyor: $savedAddress")
                    bleManager.connect(savedAddress)
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device?.address?.equals(savedAddress, ignoreCase = true) == true) {
                    Log.d(tag, "Motosiklet ACL Bağlantısı Algılandı -> Bağlanıyor")
                    bleManager.connect(savedAddress)
                }
            }
        }
    }
}
