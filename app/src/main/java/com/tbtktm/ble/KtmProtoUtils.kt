package com.tbtktm.ble

import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
import com.tbtktm.util.toTftCleanText
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * KTM 1290 Super Adventure Birebir Orijinal KMRC JSON Formatı ve Paket Çerçeveleyicisi
 * (KTMconnect Kaynak Kodları ve Canlı Trace ile %100 Doğrulanmıştır)
 */
object KtmProtoUtils {

    /**
     * KMRC Resmi Taşıma Çerçevesi (KMRC Framing)
     * Header: [4 Bayt Big-Endian Uzunluk (Payload + 1)] + [1 Bayt Mesaj Tipi (0x01)] + [JSON Baytları]
     */
    fun frameKmrcMessage(jsonString: String): ByteArray {
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(4 + 1 + jsonBytes.size)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(jsonBytes.size + 1) // 4-byte Int: data size + 1 (type byte)
        buffer.put(0x01.toByte())          // 1-byte: KMRC Msg Type = 1
        buffer.put(jsonBytes)
        return buffer.array()
    }

    fun buildKmrcJsonNavigationMessage(navData: NavigationData, msgId: String = "mup"): String {
        val root = JSONObject()

        if (navData.isActive) {
            // 1. Standart RoutePlans (TFT Menüsünün Açılması İçin Şart)
            val routePlans = JSONArray()
            val fav0 = JSONObject().apply {
                put("Class", "Favorite")
                put("Description", "Ev".toTftCleanText())
                put("IsRoutable", true)
                put("ID", "Fav#0")
                put("SubTitle", "")
                put("Title", "Home".toTftCleanText())
            }
            val fav1 = JSONObject().apply {
                put("Class", "Favorite")
                put("Description", "KTM Motohall".toTftCleanText())
                put("IsRoutable", true)
                put("ID", "Fav#1")
                put("SubTitle", "")
                put("Title", "KTM Motohall".toTftCleanText())
            }
            routePlans.put(fav0)
            routePlans.put(fav1)
            root.put("RoutePlans", routePlans)

            // 2. Ses Seviyesi
            val volObj = JSONObject().apply {
                put("Level", 80)
            }
            root.put("GuidanceVolumeAdjusted", volObj)

            // 3. Navigasyon Modu Açık
            root.put("TbtGuidanceModeOn", JSONObject())

            // 4. Bildirim & GPS İkonları
            val notifText = JSONObject().apply { put("Visibility", "off") }
            val notifIcon = JSONObject().apply { put("Visibility", "off") }
            val gpsIcon = JSONObject().apply { put("Visibility", "off") }
            root.put("NotificationText", notifText)
            root.put("NotificationIcon", notifIcon)
            root.put("GpsIcon", gpsIcon)

            // 5. GuidanceUpdate (TFT'deki Dönüş Oku ve Bilgiler)
            val guidanceUpdate = JSONObject()

            // Turn Icon
            val turnIconObj = JSONObject().apply {
                put("Image", navData.turnIcon.name)
                put("Visibility", "full")
            }
            guidanceUpdate.put("TurnIcon", turnIconObj)

            // Turn Distance & Unit
            if (navData.distanceToTurn.isNotBlank()) {
                val cleanDist = navData.distanceToTurn.toTftCleanText()
                val distParts = cleanDist.trim().split(" ")
                val distNum = distParts.getOrNull(0) ?: cleanDist
                val distUnit = if (distParts.size > 1) distParts[1] else "m"

                val distObj = JSONObject().apply {
                    put("Text", distNum)
                    put("Visibility", "full")
                }
                guidanceUpdate.put("TurnDist", distObj)

                val unitObj = JSONObject().apply {
                    put("Text", distUnit)
                    put("Visibility", "full")
                }
                guidanceUpdate.put("TurnDistUnit", unitObj)
            }

            // Turn Info (Özel metin varsa onu, yoksa seçili dilde yerelleştirilmiş açıklama)
            val currentLang = com.tbtktm.i18n.AppLanguageManager.currentLanguage.value
            val rawDesc = if (navData.turnInfo.isNotBlank()) navData.turnInfo else navData.turnIcon.getLocalizedDescription(currentLang)
            val localizedDesc = rawDesc.toTftCleanText()
            val infoObj = JSONObject().apply {
                put("Text", if (localizedDesc.isNotBlank()) localizedDesc else " ")
                put("Visibility", "full")
            }
            guidanceUpdate.put("TurnInfo", infoObj)

            // Road Name (Sokak / Cadde)
            if (navData.roadName.isNotBlank()) {
                val roadObj = JSONObject().apply {
                    put("Text", navData.roadName.toTftCleanText())
                    put("Visibility", "full")
                }
                guidanceUpdate.put("TurnRoad", roadObj)
            }

            // ETA
            if (navData.eta.isNotBlank()) {
                val etaObj = JSONObject().apply {
                    put("Text", navData.eta.toTftCleanText())
                    put("Visibility", "full")
                }
                guidanceUpdate.put("ETA", etaObj)
            }

            // Distance to Destination
            if (navData.distanceToDestination.isNotBlank()) {
                val destObj = JSONObject().apply {
                    put("Text", navData.distanceToDestination.toTftCleanText())
                    put("Visibility", "full")
                }
                guidanceUpdate.put("Dist2Target", destObj)
            }

            root.put("GuidanceUpdate", guidanceUpdate)
            root.put("ActiveWayPoint", JSONObject())
            root.put("MsgId", msgId)

        } else {
            root.put("TbtGuidanceModeOff", JSONObject())
            root.put("MsgId", "GuidanceModeOff")
        }

        return root.toString()
    }
}
