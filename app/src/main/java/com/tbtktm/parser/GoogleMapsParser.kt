package com.tbtktm.parser

import android.app.Notification
import android.os.Bundle
import com.tbtktm.model.NavSource
import com.tbtktm.model.NavigationData

object GoogleMapsParser {
    const val PACKAGE_NAME = "com.google.android.apps.maps"

    // Örnek bildirim metinleri:
    // Title: "250 m - Bağdat Cd. yönünde sağa dönün"
    // Text: "18:45 • 12 km • 18 dk"
    // SubText: "Navigasyon devam ediyor"

    private val distancePattern = Regex("""^(\d+(?:[.,]\d+)?\s*(?:m|km|ft|mi))""", RegexOption.IGNORE_CASE)
    private val etaPattern = Regex("""(\d{1,2}:\d{2})""")
    private val totalDistPattern = Regex("""(\d+(?:[.,]\d+)?\s*(?:km|mi))""", RegexOption.IGNORE_CASE)

    fun parse(notification: Notification): NavigationData? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return null

        var distanceToTurn = ""
        var instruction = title
        var roadName = ""

        // 1. Sonraki dönüş mesafesini ayıkla
        val distMatch = distancePattern.find(title)
        if (distMatch != null) {
            distanceToTurn = distMatch.value
            instruction = title.substring(distMatch.range.last + 1).trim(' ', '-', '•', ',')
        }

        // 2. Yol / Cadde adını ayıkla (varsa "yönünde", "üzerinde" öncesi)
        if (instruction.contains("yönünde")) {
            val parts = instruction.split("yönünde")
            roadName = parts[0].trim()
        } else if (instruction.contains("üzerinde")) {
            val parts = instruction.split("üzerinde")
            roadName = parts[0].trim()
        } else {
            roadName = instruction
        }

        // 3. ETA ve Kalan toplam mesafeyi ayıkla
        val etaMatch = etaPattern.find(text)
        val eta = etaMatch?.value ?: ""

        val totalDistMatch = totalDistPattern.find(text)
        val distanceToDestination = totalDistMatch?.value ?: ""

        // 4. İkon tespiti
        val icon = IconMatcher.matchFromText(title + " " + instruction)

        return NavigationData(
            isActive = true,
            source = NavSource.GOOGLE_MAPS,
            turnIcon = icon,
            distanceToTurn = distanceToTurn,
            turnInfo = instruction,
            roadName = roadName,
            eta = eta,
            distanceToDestination = distanceToDestination,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
