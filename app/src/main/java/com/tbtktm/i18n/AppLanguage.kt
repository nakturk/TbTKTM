package com.tbtktm.i18n

enum class AppLanguage(val code: String, val displayName: String, val flagEmoji: String) {
    TURKISH("tr", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "🇬🇧"),
    ITALIAN("it", "Italiano", "🇮🇹"),
    SPANISH("es", "Español", "🇪🇸"),
    GREEK("el", "Ελληνικά", "🇬🇷");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
