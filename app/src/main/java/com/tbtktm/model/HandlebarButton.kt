package com.tbtktm.model

import com.tbtktm.i18n.AppStrings

/**
 * KTM 1290 Super Adventure ve diğer KTM modellerinin sol kütüğündeki butonlar.
 */
enum class HandlebarButton(val bitMask: Int, val displayName: String) {
    SET(1 shl 0, "SET (Center)"),
    BACK(1 shl 1, "BACK"),
    DOWN(1 shl 2, "DOWN"),
    UP(1 shl 3, "UP"),
    LEFT(1 shl 4, "LEFT"),
    RIGHT(1 shl 5, "RIGHT"),
    C1(1 shl 6, "Custom 1 (C1)"),
    C2(1 shl 7, "Custom 2 (C2)");

    companion object {
        fun parseFromByte(byteVal: Int): Map<HandlebarButton, Boolean> {
            return entries.associateWith { (byteVal and it.bitMask) != 0 }
        }
    }
}

/**
 * Gidon tuşlarına atanabilecek eylemler.
 */
enum class ButtonAction(val label: String) {
    NONE("İşlevsiz"),
    MEDIA_PLAY_PAUSE("Müzik Oynat / Duraklat"),
    MEDIA_NEXT("Sonraki Parça"),
    MEDIA_PREVIOUS("Önceki Parça"),
    VOLUME_UP("Ses Artır"),
    VOLUME_DOWN("Ses Azalt"),
    VOICE_ASSISTANT("Sesli Asistan"),
    RECENTER_MAP("Haritayı Ortala");

    fun getLocalizedLabel(strings: AppStrings): String {
        return when (this) {
            NONE -> strings.actionNone
            MEDIA_PLAY_PAUSE -> strings.actionPlayPause
            MEDIA_NEXT -> strings.actionNextTrack
            MEDIA_PREVIOUS -> strings.actionPrevTrack
            VOLUME_UP -> strings.actionVolumeUp
            VOLUME_DOWN -> strings.actionVolumeDown
            VOICE_ASSISTANT -> strings.actionVoiceAssistant
            RECENTER_MAP -> strings.actionRecenterMap
        }
    }
}
