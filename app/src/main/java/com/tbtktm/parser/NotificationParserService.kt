package com.tbtktm.parser

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tbtktm.TbTApplication
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.model.NavigationData
import com.tbtktm.ticker.TftMarqueeTicker
import com.tbtktm.util.FileLogger

class NotificationParserService : NotificationListenerService() {

    private val tag = "NotificationParser"
    private lateinit var bleManager: KtmBleManager
    private lateinit var ticker: TftMarqueeTicker

    override fun onCreate() {
        super.onCreate()
        bleManager = KtmBleManager.getInstance(this)
        ticker = TftMarqueeTicker.getInstance(bleManager)
        Log.d(tag, "NotificationParserService onCreate")
        FileLogger.log(">> NotificationParserService OLUŞTURULDU (Navigasyon + Çoklu Bildirim Ticker Aktif)")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(tag, "NotificationParserService onListenerConnected")
        FileLogger.log(">> BİLDİRİM DİNLEME SERVİSİ AKTİF (Android Sistemine Bağlandı)")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(tag, "NotificationParserService onListenerDisconnected")
        FileLogger.log("!! BİLDİRİM DİNLEME SERVİSİ KOPTU (onListenerDisconnected)")
        try {
            requestRebind(ComponentName(this, NotificationParserService::class.java))
        } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val notification = sbn.notification ?: return

        // 1. Google Maps ve Harita Navigasyon Paketleri
        if (UniversalNavParser.isNavPackage(packageName)) {
            val navData = UniversalNavParser.parse(packageName, notification)

            if (navData != null) {
                FileLogger.log(">> 🧭 HARİTA BİLDİRİMİ: ${navData.turnIcon.description} | ${navData.distanceToTurn} | ${navData.roadName}")
                TbTApplication.updateNavigationData(navData)
                ticker.onNewNavigationData(navData)
            }
            return
        }

        // 2. Mesajlaşma ve E-Posta Paketleri (WhatsApp, Telegram, SMS, Gmail, Yahoo, Outlook)
        if (AppNotificationParser.isSupportedApp(packageName)) {
            val notifData = AppNotificationParser.parse(packageName, notification)
            if (notifData != null) {
                FileLogger.log(">> 💬 BİLDİRİM YAKALANDI [${notifData.appName}]: '${notifData.senderOrTitle}' - '${notifData.fullMessageText}'")
                ticker.displayNotification(notifData)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        if (UniversalNavParser.isNavPackage(packageName)) {
            val currentNav = TbTApplication.currentNavData.value
            if (currentNav.isActive) {
                FileLogger.log(">> Harita Bildirimi Kapatıldı: $packageName -> TFT Navigasyon Sıfırlanıyor")
                val emptyData = NavigationData(isActive = false)
                TbTApplication.updateNavigationData(emptyData)
                ticker.onNewNavigationData(emptyData)
            }
        }
    }
}
