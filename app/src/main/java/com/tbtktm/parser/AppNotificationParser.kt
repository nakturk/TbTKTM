package com.tbtktm.parser

import android.app.Notification
import android.os.Bundle
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gelen WhatsApp, Telegram, SMS, Gmail, Yahoo Mail ve Outlook bildirimlerini
 * KTM TFT ekranına uygun formata dönüştüren ayrıştırıcı.
 * 
 * Birden fazla okunmamış mesaj olduğunda "n messages" / "n yeni mesaj" gibi özet sayaçları yerine
 * her zaman en yeni gelen mesajın içeriğini ve gönderenini çıkarır.
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
        "com.microsoft.office.outlook",
        // Diğer Mesajlaşma Uygulamaları
        "org.thoughtcrime.securesms", // Signal
        "com.viber.voip",             // Viber
        "com.discord",                // Discord
        "com.turkcell.bip"            // BiP
    )

    // "n new messages", "2 yeni mesaj", "3 messages from 2 chats" gibi özet sayaçlarını yakalayan regex
    private val GENERIC_SUMMARY_REGEX = Regex(
        """^(?:\d+\s+(?:yeni|new|okunmamış|unread|neue|nouveaux|nuevos|nuovi)?\s*(?:mesaj(?:lar)?|messages?|nachrichten?|mensajes?|messaggi|bildirim|notification|sohbet|chat)[\w\s\(\)]*|\d+\s+(?:sohbetten|chats?|conversations?|kişi|gruptan)\s+\d+.*)$""",
        RegexOption.IGNORE_CASE
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
            "org.thoughtcrime.securesms" -> "Signal"
            "com.viber.voip" -> "Viber"
            "com.discord" -> "Discord"
            "com.turkcell.bip" -> "BiP"
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

    private fun isGenericSummary(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        return GENERIC_SUMMARY_REGEX.matches(trimmed)
    }

    fun parse(packageName: String, notification: Notification): AppNotificationData? {
        val extras: Bundle = notification.extras ?: return null
        val defaultAppName = getAppName(packageName)
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""

        var resolvedSender: String? = null
        var resolvedMessage: String? = null

        // 1. ÖNCELİK: Android MessagingStyle (WhatsApp, Telegram, Signal, SMS)
        // Birden fazla okunmamış mesaj olduğunda tüm mesajlar buradadır; son mesaj en yenisidir.
        try {
            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
                // Zaman damgasına göre en yeni mesajı veya son mesajı bul
                val validMessages = messagingStyle.messages.filter { !it.text.isNullOrBlank() }
                val latestMessage = validMessages.maxByOrNull { it.timestamp } ?: validMessages.lastOrNull()

                if (latestMessage != null) {
                    val msgText = latestMessage.text?.toString()?.trim()
                    if (!msgText.isNullOrBlank() && !isGenericSummary(msgText)) {
                        resolvedMessage = msgText

                        val personName = latestMessage.person?.name?.toString()?.trim()
                            ?: latestMessage.sender?.toString()?.trim()
                        val convTitle = messagingStyle.conversationTitle?.toString()?.trim()
                        val isGroup = messagingStyle.isGroupConversation

                        resolvedSender = when {
                            !personName.isNullOrBlank() && !convTitle.isNullOrBlank() && (isGroup || convTitle != personName) -> {
                                "$personName ($convTitle)"
                            }
                            !personName.isNullOrBlank() -> personName
                            !convTitle.isNullOrBlank() -> convTitle
                            rawTitle.isNotBlank() && !isGenericSummary(rawTitle) -> rawTitle
                            else -> defaultAppName
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. ÖNCELİK: InboxStyle Satırları (EXTRA_TEXT_LINES - E-Posta özetleri veya çoklu WhatsApp satırları)
        if (resolvedMessage.isNullOrBlank()) {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (lines != null && lines.isNotEmpty()) {
                val lastValidLine = lines.reversed().firstOrNull { !it.isNullOrBlank() }?.toString()?.trim()
                if (lastValidLine != null && !isGenericSummary(lastValidLine)) {
                    if (lastValidLine.contains(": ")) {
                        val prefix = lastValidLine.substringBefore(": ").trim()
                        val suffix = lastValidLine.substringAfter(": ").trim()
                        if (suffix.isNotBlank()) {
                            resolvedMessage = suffix
                            resolvedSender = if (rawTitle.isNotBlank() && !isGenericSummary(rawTitle) && rawTitle != defaultAppName) {
                                "$prefix ($rawTitle)"
                            } else {
                                prefix
                            }
                        } else {
                            resolvedMessage = lastValidLine
                        }
                    } else {
                        resolvedMessage = lastValidLine
                    }
                }
            }
        }

        // 3. ÖNCELİK: EXTRA_BIG_TEXT, EXTRA_TEXT, EXTRA_SUB_TEXT
        if (resolvedMessage.isNullOrBlank()) {
            val rawBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
            val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
            val rawSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

            val candidate = when {
                rawBigText.isNotBlank() && !isGenericSummary(rawBigText) -> rawBigText
                rawText.isNotBlank() && !isGenericSummary(rawText) -> rawText
                rawSubText.isNotBlank() && !isGenericSummary(rawSubText) -> rawSubText
                else -> ""
            }

            if (candidate.isNotBlank()) {
                resolvedMessage = candidate
            }
        }

        // Eğer mesaj hala bulunamadıysa (örneğin sadece "2 new messages" olan bir grup özeti bildirimi)
        // TFT ekranına anlamsız "n messages" basmamak için bildirimi geç
        if (resolvedMessage.isNullOrBlank()) {
            return null
        }

        // Gönderen belirlenmediyse rawTitle veya AppName kullan
        val sender = when {
            !resolvedSender.isNullOrBlank() -> resolvedSender
            rawTitle.isNotBlank() && !isGenericSummary(rawTitle) -> rawTitle
            else -> defaultAppName
        }

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        return AppNotificationData(
            appName = defaultAppName,
            senderOrTitle = sender,
            fullMessageText = resolvedMessage.replace("\n", " ").trim(),
            timeFormatted = timeStr,
            badgeText = getBadgeText(packageName)
        )
    }
}
