package com.tbtktm.rcm

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.tbtktm.model.ButtonAction

class ActionDispatcher(private val context: Context) {

    private val tag = "ActionDispatcher"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun execute(action: ButtonAction) {
        Log.d(tag, "Executing Button Action: ${action.label}")

        when (action) {
            ButtonAction.NONE -> {}

            ButtonAction.MEDIA_PLAY_PAUSE -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }

            ButtonAction.MEDIA_NEXT -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            }

            ButtonAction.MEDIA_PREVIOUS -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }

            ButtonAction.VOLUME_UP -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            ButtonAction.VOLUME_DOWN -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            ButtonAction.VOICE_ASSISTANT -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_VOICE_ASSIST)
            }

            ButtonAction.RECENTER_MAP -> {
                // Haritayı öne getirmek veya harita intent'i göndermek için
                Log.d(tag, "Recenter Map triggered")
            }
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val eventDown = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN,
            keyCode,
            0
        )
        val eventUp = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            keyCode,
            0
        )

        audioManager?.dispatchMediaKeyEvent(eventDown)
        audioManager?.dispatchMediaKeyEvent(eventUp)
    }
}
