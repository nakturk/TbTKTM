package com.tbtktm.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tbtktm.MainActivity
import com.tbtktm.R

class KtmBleService : Service() {

    private val channelId = "ktm_ble_service_channel"
    private val notificationId = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, createNotification("KTM BLE Bağlantı Servisi Aktif"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val deviceAddress = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)

        val bleManager = KtmBleManager.getInstance(this)

        if (action == ACTION_CONNECT && deviceAddress != null) {
            bleManager.connect(deviceAddress)
        } else if (action == ACTION_DISCONNECT) {
            bleManager.disconnect()
            stopSelf()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TbTKTM TFT Bağlantısı")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "TbTKTM BLE Servisi",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_CONNECT = "com.tbtktm.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.tbtktm.ACTION_DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"

        fun startService(context: Context, deviceAddress: String) {
            val intent = Intent(context, KtmBleService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KtmBleService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
