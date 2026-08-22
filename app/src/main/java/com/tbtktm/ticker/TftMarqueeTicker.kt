package com.tbtktm.ticker

import com.tbtktm.TbTApplication
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.ble.KtmProtoUtils
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
import com.tbtktm.parser.AppNotificationData
import com.tbtktm.util.FileLogger
import com.tbtktm.util.toTftCleanText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * KTM TFT Ekranında Gelen Mesajları Yatay Kayan Yazı (Marquee Ticker) Olarak Gösteren Motor.
 * 800ms kaydırma adımları, dinamik süre hesabı (en az 10sn), Restore başlatma desteği
 * ve otomatik navigasyona dönüş sağlar.
 */
class TftMarqueeTicker private constructor(private val bleManager: KtmBleManager) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activeTickerJob: Job? = null

    // En son bilinen aktif Google Maps navigasyonu
    private var lastKnownNavData: NavigationData? = null
    var isNotificationActive: Boolean = false
        private set

    fun onNewNavigationData(navData: NavigationData) {
        if (navData.isActive) {
            lastKnownNavData = navData
        } else {
            lastKnownNavData = null
        }

        // Eğer kritik bir dönüş manevrası geldiyse ve şu an bildirim kayıyorsa bildirimi iptal edip haritayı öne al
        if (isNotificationActive && navData.isActive && navData.turnIcon != KtmTurnIcon.GO_STRAIGHT && navData.turnIcon != KtmTurnIcon.UNDEFINED) {
            FileLogger.log(">> ⚡ Kritik Navigasyon Manevrası Geldi: Bildirim İptal Edilip Harita Öne Alınıyor!")
            cancelActiveNotification()
            bleManager.sendNavigationUpdate(navData)
        } else if (!isNotificationActive) {
            bleManager.sendNavigationUpdate(navData)
        }
    }

    fun displayNotification(notif: AppNotificationData) {
        activeTickerJob?.cancel()
        isNotificationActive = true

        activeTickerJob = scope.launch {
            val message = notif.fullMessageText.ifBlank { "Yeni Bildirim" }.toTftCleanText()
            val senderName = notif.senderOrTitle.take(30).ifBlank { notif.appName }.toTftCleanText()
            FileLogger.log(">> 📜 TFT BİLDİRİM KAYAN YAZI BAŞLATILDI [${notif.appName}]: '$senderName' - '$message'")

            val windowSize = 32
            val stepSize = 6
            val stepDelayMs = 800L

            // 1. Kayan Yazı Karelerini Üret
            val frames = mutableListOf<String>()
            if (message.length <= windowSize) {
                frames.add(message)
            } else {
                var start = 0
                while (start < message.length) {
                    val end = (start + windowSize).coerceAtMost(message.length)
                    val frameText = message.substring(start, end)
                    frames.add(frameText)
                    if (end == message.length) break
                    start += stepSize
                }
            }

            // 2. Dinamik Süre Hesabı (En az 10 saniye)
            val slidingDurationMs = frames.size * stepDelayMs
            val holdDurationMs = max(3000L, 10000L - slidingDurationMs)

            val hasActiveRoute = lastKnownNavData?.isActive == true

            // 3. Kareleri Sırayla TFT'ye Bas
            // BÜYÜK ALAN (TurnRoad): Kayan Mesaj Metni (frameText)
            // KÜÇÜK ALAN (TurnInfo): Gönderen Kişi / Başlık (senderName)
            for ((index, frameText) in frames.withIndex()) {
                val isFirstFrame = (index == 0)
                // Eğer harita açık değilse, TFT ekranının TBT modunu açması için ilk karede mutlaka "Restore" gönderilir
                val msgId = if (isFirstFrame && !hasActiveRoute) "Restore" else "mup"

                val displayNav = NavigationData(
                    isActive = true,
                    turnIcon = KtmTurnIcon.START,
                    distanceToTurn = notif.badgeText.ifBlank { "MSG" },
                    turnInfo = senderName,
                    roadName = frameText,
                    eta = notif.timeFormatted,
                    distanceToDestination = notif.appName.take(12)
                )

                sendCustomTickerFrame(displayNav, msgId = msgId)
                TbTApplication.updateNavigationData(displayNav)

                delay(stepDelayMs)
            }

            // 4. Mesaj Sonunda Bekle
            delay(holdDurationMs)

            // 5. Bildirim Bitti: Otomatik Olarak Google Maps'e veya Boş Ekrana Dön
            isNotificationActive = false
            FileLogger.log(">> 🏁 TFT Bildirim Süresi Doldu, Navigasyon Durumuna Geri Dönülüyor...")

            val restoreNav = lastKnownNavData
            if (restoreNav != null && restoreNav.isActive) {
                FileLogger.log(">> 🧭 Google Maps Navigasyonu Geri Yükleniyor: ${restoreNav.roadName}")
                TbTApplication.updateNavigationData(restoreNav)
                bleManager.sendNavigationUpdate(restoreNav)
            } else {
                FileLogger.log(">> Navigasyon aktif değil, TFT ekranı kapatılıyor (GuidanceModeOff)")
                val emptyNav = NavigationData(isActive = false)
                TbTApplication.updateNavigationData(emptyNav)
                bleManager.sendNavigationUpdate(emptyNav)
            }
        }
    }

    private fun sendCustomTickerFrame(navData: NavigationData, msgId: String) {
        val kmrcJson = KtmProtoUtils.buildKmrcJsonNavigationMessage(navData, msgId = msgId)
        bleManager.sendRawKmrcJson(kmrcJson)
    }

    fun cancelActiveNotification() {
        activeTickerJob?.cancel()
        activeTickerJob = null
        isNotificationActive = false
    }

    companion object {
        @Volatile
        private var instance: TftMarqueeTicker? = null

        fun getInstance(bleManager: KtmBleManager): TftMarqueeTicker {
            return instance ?: synchronized(this) {
                instance ?: TftMarqueeTicker(bleManager).also { instance = it }
            }
        }
    }
}
