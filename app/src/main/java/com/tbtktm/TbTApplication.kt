package com.tbtktm

import android.app.Application
import com.tbtktm.model.NavigationData
import com.tbtktm.rcm.HandlebarKeyManager
import com.tbtktm.util.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TbTApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    lateinit var handlebarKeyManager: HandlebarKeyManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        FileLogger.init(this)
        com.tbtktm.i18n.AppLanguageManager.init(this)
        handlebarKeyManager = HandlebarKeyManager(this, applicationScope)
        handlebarKeyManager.startListening()
    }

    companion object {
        lateinit var instance: TbTApplication
            private set

        private val _currentNavData = MutableStateFlow(NavigationData())
        val currentNavData: StateFlow<NavigationData> = _currentNavData.asStateFlow()

        private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
        val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

        private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun updateNavigationData(navData: NavigationData) {
            _currentNavData.value = navData
        }

        fun appendLog(message: String) {
            val timestamp = timeFormat.format(Date())
            val entry = "[$timestamp] $message"
            val currentList = _diagnosticLogs.value.toMutableList()
            if (currentList.size > 50) {
                currentList.removeAt(0)
            }
            currentList.add(entry)
            _diagnosticLogs.value = currentList
        }
    }
}
