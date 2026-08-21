package com.tbtktm.rcm

import android.content.Context
import android.util.Log
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.model.ButtonAction
import com.tbtktm.model.HandlebarButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HandlebarKeyManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val tag = "HandlebarKeyManager"
    private val actionDispatcher = ActionDispatcher(context)
    private val bleManager = KtmBleManager.getInstance(context)

    // Kullanıcının atadığı tuş haritası (Varsayılan değerlerle)
    private val _keyMappings = MutableStateFlow<Map<HandlebarButton, ButtonAction>>(
        mapOf(
            HandlebarButton.SET to ButtonAction.MEDIA_PLAY_PAUSE,
            HandlebarButton.UP to ButtonAction.VOLUME_UP,
            HandlebarButton.DOWN to ButtonAction.VOLUME_DOWN,
            HandlebarButton.RIGHT to ButtonAction.MEDIA_NEXT,
            HandlebarButton.LEFT to ButtonAction.MEDIA_PREVIOUS,
            HandlebarButton.BACK to ButtonAction.NONE,
            HandlebarButton.C1 to ButtonAction.VOICE_ASSISTANT,
            HandlebarButton.C2 to ButtonAction.RECENTER_MAP
        )
    )
    val keyMappings: StateFlow<Map<HandlebarButton, ButtonAction>> = _keyMappings.asStateFlow()

    private var previousState: Map<HandlebarButton, Boolean> = emptyMap()
    private var listenJob: Job? = null

    fun startListening() {
        listenJob?.cancel()
        listenJob = scope.launch(Dispatchers.IO) {
            bleManager.handlebarButtonEvents.collect { newState ->
                processButtonTransitions(previousState, newState)
                previousState = newState
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
    }

    fun updateMapping(button: HandlebarButton, action: ButtonAction) {
        val current = _keyMappings.value.toMutableMap()
        current[button] = action
        _keyMappings.value = current
    }

    private fun processButtonTransitions(
        prev: Map<HandlebarButton, Boolean>,
        curr: Map<HandlebarButton, Boolean>
    ) {
        for ((button, isPressed) in curr) {
            val wasPressed = prev[button] ?: false
            
            // Butona basıldığı an (Rising edge / Click)
            if (isPressed && !wasPressed) {
                val assignedAction = _keyMappings.value[button] ?: ButtonAction.NONE
                Log.d(tag, "Handlebar Button Clicked: ${button.displayName} -> ${assignedAction.label}")
                actionDispatcher.execute(assignedAction)
            }
        }
    }
}
