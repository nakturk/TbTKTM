package com.tbtktm.util

import android.content.Context
import android.util.Log
import com.tbtktm.TbTApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private val tag = "FileLogger"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var logFile: File? = null

    fun init(context: Context) {
        try {
            val logDir = context.getExternalFilesDir(null) ?: context.filesDir
            logFile = File(logDir, "tbtktm_ble_debug.log")
            if (!logFile!!.exists()) {
                logFile!!.createNewFile()
            }
            log("=== TbTKTM BLE Logger Başlatıldı [${dateFormat.format(Date())}] ===")
        } catch (e: Exception) {
            Log.e(tag, "Log dosyası oluşturulamadı", e)
        }
    }

    fun getLogFilePath(): String {
        return logFile?.absolutePath ?: "Bilinmiyor"
    }

    fun getLogFile(): File? = logFile

    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] $message"

        // 1. Logcat
        Log.d("TbTKTM_DEBUG", message)

        // 2. UI StateFlow Logları
        TbTApplication.appendLog(message)

        // 3. Kalıcı Diske Yazma
        scope.launch {
            try {
                logFile?.let { file ->
                    FileWriter(file, true).use { writer ->
                        writer.appendLine(entry)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Diske log yazma hatası", e)
            }
        }
    }

    fun logBytes(prefix: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        val ascii = data.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
        log("$prefix (len=${data.size}): HEX=[$hex] ASCII=[$ascii]")
    }

    fun clearLogs() {
        scope.launch {
            try {
                logFile?.writeText("")
                log("=== Loglar Sıfırlandı ===")
            } catch (_: Exception) {}
        }
    }
}
