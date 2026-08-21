package com.tbtktm.parser

import android.app.Notification
import com.tbtktm.model.NavSource
import com.tbtktm.model.NavigationData

object YandexNaviParser {
    val PACKAGE_NAMES = listOf("ru.yandex.yandexnavi", "ru.yandex.yandexmaps")

    private val distancePattern = Regex("""(\d+(?:[.,]\d+)?\s*(?:m|km))""", RegexOption.IGNORE_CASE)
    private val etaPattern = Regex("""(\d{1,2}:\d{2})""")
    private val totalDistPattern = Regex("""(\d+(?:[.,]\d+)?\s*km)""", RegexOption.IGNORE_CASE)

    fun parse(notification: Notification): NavigationData? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return null

        var distanceToTurn = ""
        var instruction = text
        var roadName = title

        // Yandex Navigasyon genelde Başlıkta cadde adı veya mesafeyi verir
        val distMatch = distancePattern.find(title + " " + text)
        if (distMatch != null) {
            distanceToTurn = distMatch.value
        }

        val etaMatch = etaPattern.find(text + " " + subText)
        val eta = etaMatch?.value ?: ""

        val totalDistMatch = totalDistPattern.find(text + " " + subText)
        val distanceToDestination = totalDistMatch?.value ?: ""

        val icon = IconMatcher.matchFromText(title + " " + text)

        return NavigationData(
            isActive = true,
            source = NavSource.YANDEX_NAVI,
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
