package com.tbtktm.parser

import android.app.Notification
import android.os.Bundle
import android.util.Log
import com.tbtktm.model.NavSource
import com.tbtktm.model.NavigationData

object UniversalNavParser {

    private val tag = "UniversalNavParser"

    val GOOGLE_MAPS_PACKAGES = setOf(
        "com.google.android.apps.maps",
        "com.google.android.apps.mapsgo"
    )

    val YANDEX_PACKAGES = setOf(
        "ru.yandex.yandexnavi",
        "ru.yandex.yandexmaps",
        "ru.yandex.maps",
        "com.yandex.maps",
        "yandex.auto"
    )

    private val distancePattern = Regex("""(\d+(?:[.,]\d+)?\s*(?:km|mi|m|ft|yd|miles|feet|yard))""", RegexOption.IGNORE_CASE)
    private val etaPattern = Regex("""(\d{1,2}:\d{2})""")
    private val totalDistPattern = Regex("""(\d+(?:[.,]\d+)?\s*(?:km|mi|m|miles))""", RegexOption.IGNORE_CASE)

    fun isNavPackage(packageName: String): Boolean {
        return GOOGLE_MAPS_PACKAGES.contains(packageName) ||
                YANDEX_PACKAGES.contains(packageName) ||
                packageName.contains("maps", ignoreCase = true) ||
                packageName.contains("navi", ignoreCase = true)
    }

    fun parse(packageName: String, notification: Notification): NavigationData? {
        val extras = notification.extras ?: return null

        // Bildirimdeki tüm metin alanlarını topla
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val titleBig = extras.getCharSequence("android.title.big")?.toString() ?: ""
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""

        val allTextCombined = "$title $titleBig $text $bigText $subText $infoText".trim()

        Log.d(tag, "[$packageName] Gelen Ham Bildirim Metinleri -> Title: '$title' | Text: '$text' | SubText: '$subText' | BigText: '$bigText'")

        // Boş veya sadece uygulamanın adını içeren bildirimleri ele
        if (allTextCombined.isBlank()) return null

        val source = when {
            GOOGLE_MAPS_PACKAGES.contains(packageName) -> NavSource.GOOGLE_MAPS
            YANDEX_PACKAGES.contains(packageName) || packageName.contains("yandex", ignoreCase = true) -> NavSource.YANDEX_NAVI
            else -> NavSource.GOOGLE_MAPS
        }

        var distanceToTurn = ""
        var instruction = if (title.isNotBlank()) title else text
        var roadName = ""
        var eta = ""
        var distanceToDestination = ""

        // 1. Dönüş Mesafesi (Örn: "250 m", "1.2 km")
        val distMatches = distancePattern.findAll(allTextCombined).toList()
        if (distMatches.isNotEmpty()) {
            distanceToTurn = distMatches.first().value
        }

        // 2. Kalan Toplam Mesafe (Sonraki mesafe eşleşmesi)
        if (distMatches.size > 1) {
            distanceToDestination = distMatches.last().value
        } else {
            val totalMatch = totalDistPattern.find(text + " " + subText + " " + bigText)
            if (totalMatch != null) {
                distanceToDestination = totalMatch.value
            }
        }

        // 3. ETA (Varış Saati: "18:45")
        val etaMatch = etaPattern.find(allTextCombined)
        if (etaMatch != null) {
            eta = etaMatch.value
        }

        // 4. Cadde / Yol Adı ve Manevra Metni
        val cleanedInstruction = instruction.replace(distanceToTurn, "").trim(' ', '-', '•', ',')
        if (cleanedInstruction.contains("yönünde", ignoreCase = true)) {
            val parts = cleanedInstruction.split(Regex("yönünde", RegexOption.IGNORE_CASE))
            roadName = parts[0].trim()
        } else if (cleanedInstruction.contains("üzerinde", ignoreCase = true)) {
            val parts = cleanedInstruction.split(Regex("üzerinde", RegexOption.IGNORE_CASE))
            roadName = parts[0].trim()
        } else {
            roadName = if (subText.isNotBlank()) subText else cleanedInstruction
        }

        // 5. Manevra İkonu Tespiti
        val icon = IconMatcher.matchFromText(allTextCombined)

        return NavigationData(
            isActive = true,
            source = source,
            turnIcon = icon,
            distanceToTurn = distanceToTurn,
            turnInfo = if (cleanedInstruction.isNotBlank()) cleanedInstruction else icon.description,
            roadName = roadName,
            eta = eta,
            distanceToDestination = distanceToDestination,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
