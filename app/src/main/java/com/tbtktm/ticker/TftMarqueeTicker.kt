package com.tbtktm.ticker

import com.tbtktm.TbTApplication
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
import com.tbtktm.parser.AppNotificationData
import com.tbtktm.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * KTM TFT Ekranında Gelen Mesajları Yatay Kayan Yazı (Marquee Ticker) Olarak Gösteren Motor.
 * 800ms kaydırma adımları, dinamik süre hesabı (en az 10sn) ve otomatik navigasyona dönüş sağlar.
 */
class TftMarqueeTicker(private val bleManager: KtmBleManager) {

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
            val message = notif.fullMessageText.ifBlank { "Yeni Bildirim" }
            FileLogger.log(">> 📜 TFT BİLDİRİM KAYAN YAZI BAŞLATILDI [${notif.appName}]: '${notif.senderOrTitle}' - '$message'")

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

            // 3. Kareleri Sırayla TFT'ye Bas
            for ((index, frameText) in frames.withIndex()) {
                val displayNav = NavigationData(
                    isActive = true,
                    turnIcon = KtmTurnIcon.START,
                    distanceToTurn = notif.badgeText,
                    roadName = notif.senderOrTitle.take(30),
                    eta = notif.timeFormatted,
                    distanceToDestination = notif.appName.take(12)
                )

                // TurnInfo alanına kayan pencere metnini bas
                val customDescriptionNav = displayNav.copy(
                    turnIcon = KtmTurnIcon.START
                )
                // KtmProtoUtils için geçici özel açıklama
                sendCustomTickerFrame(displayNav, frameText)

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

    private fun sendCustomTickerFrame(baseNav: NavigationData, tickerText: String) {
        val navWithTicker = baseNav.copy(
            roadName = baseNav.roadName,
            distanceToTurn = baseNav.distanceToTurn
        )
        // Custom KMRC JSON ile TurnInfo metnini bas
        val kmrcJson = buildTickerKmrcJson(navWithTicker, tickerText)
        bleManager.sendRawKmrcJson(kmrcJson)
    }

    private fun buildTickerKmrcJson(navData: NavigationData, tickerText: String): String {
        val root = org.json.JSONObject()
        root.put("TbtGuidanceModeOn", org.json.JSONObject())

        val guidanceUpdate = org.json.JSONObject()

        val turnIconObj = org.json.JSONObject().apply {
            put("Image", "START")
            put("Visibility", "full")
        }
        guidanceUpdate.put("TurnIcon", turnIconObj)

        val distObj = org.json.JSONObject().apply {
            put("Text", "1")
            put("Visibility", "full")
        }
        guidanceUpdate.put("TurnDist", distObj)

        val unitObj = org.json.JSONObject().apply {
            put("Text", navData.distanceToTurn)
            put("Visibility", "full")
        }
        guidanceUpdate.put("TurnDistUnit", unitObj)

        val infoObj = org.json.JSONObject().apply {
            put("Text", tickerText)
            put("Visibility", "full")
        }
        guidanceUpdate.put("TurnInfo", infoObj)

        val roadObj = org.json.JSONObject().apply {
            put("Text", navData.roadName)
            put("Visibility", "full")
        }
        guidanceUpdate.put("TurnRoad", roadObj)

        val etaObj = org.json.JSONObject().apply {
            put("Text", navData.eta)
            put("Visibility", "full")
        }
        guidanceUpdate.put("ETA", etaObj)

        val destObj = org.json.JSONObject().apply {
            put("Text", navData.distanceToDestination)
            put("Visibility", "full")
        }
        guidanceUpdate.put("Dist2Target", destObj)

        root.put("GuidanceUpdate", guidanceUpdate)
        root.put("ActiveWayPoint", org.json.JSONObject())
        root.put("MsgId", "mup")

        return root.toString()
    }

    fun cancelActiveNotification() {
        activeTickerJob?.cancel()
        activeTickerJob = null
        isNotificationActive = false
    }
}
