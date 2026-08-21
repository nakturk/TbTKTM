package com.tbtktm.parser

import android.app.Notification
import android.os.Bundle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gelen WhatsApp, Telegram, SMS, Gmail, Yahoo Mail ve Outlook bildirimlerini
 * KTM TFT ekranına uygun formata dönüştüren ayrıştırıcı.
 */
data class AppNotificationData(
    val appName: String,
    val senderOrTitle: String,
    val fullMessageText: String,
    val timeFormatted: String,
    val badgeText: String = "MSG"
)

object AppNotificationParser {

    private val SUPPORTED_PACKAGES = setOf(
        // WhatsApp
        "com.whatsapp",
        "com.whatsapp.w4b",
        // Telegram
        "org.telegram.messenger",
        "org.telegram.plus",
        "org.thunderdog.challegram",
        // SMS
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        // E-Posta (Gmail, Yahoo, Outlook)
        "com.google.android.gm",
        "com.yahoo.mobile.client.android.mail",
        "com.microsoft.office.outlook"
    )

    fun isSupportedApp(packageName: String): Boolean {
        return SUPPORTED_PACKAGES.contains(packageName)
    }

    fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
            "org.telegram.messenger", "org.telegram.plus", "org.thunderdog.challegram" -> "Telegram"
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms" -> "SMS"
            "com.google.android.gm" -> "Gmail"
            "com.yahoo.mobile.client.android.mail" -> "Yahoo Mail"
            "com.microsoft.office.outlook" -> "Outlook"
            else -> "Bildirim"
        }
    }

    fun getBadgeText(packageName: String): String {
        return when (packageName) {
            "com.google.android.gm", "com.yahoo.mobile.client.android.mail", "com.microsoft.office.outlook" -> "MAIL"
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms" -> "SMS"
            else -> "MSG"
        }
    }

    fun parse(packageName: String, notification: Notification): AppNotificationData? {
        val extras: Bundle = notification.extras ?: return null

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val rawBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
        val rawSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        // Mesaj metnini en detaylı alandan seç
        val messageBody = when {
            rawBigText.isNotBlank() -> rawBigText
            rawText.isNotBlank() -> rawText
            else -> rawSubText
        }

        if (rawTitle.isBlank() && messageBody.isBlank()) return null

        // Başlık (Gönderen)
        val sender = if (rawTitle.isNotBlank()) rawTitle else getAppName(packageName)

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        return AppNotificationData(
            appName = getAppName(packageName),
            senderOrTitle = sender,
            fullMessageText = messageBody.replace("\n", " "),
            timeFormatted = timeStr,
            badgeText = getBadgeText(packageName)
        )
    }
}
