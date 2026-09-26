package com.aistudio.ecotobacco.kfzqw.data.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    enum class LogType { DEBUG, INFO, WARN, ERROR }
    data class LogEntry(val timestamp: Long, val tag: String, val message: String, val type: LogType)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(tag: String, message: String, type: LogType = LogType.DEBUG) {
        val entry = LogEntry(System.currentTimeMillis(), tag, message, type)
        _logs.value += entry
        if (_logs.value.size > 200) _logs.value = _logs.value.takeLast(200)

        // Also output to system logcat
        when (type) {
            LogType.DEBUG -> Log.d(tag, message)
            LogType.INFO -> Log.i(tag, message)
            LogType.WARN -> Log.w(tag, message)
            LogType.ERROR -> Log.e(tag, message)
        }
    }

    fun d(tag: String, message: String) = log(tag, message, LogType.DEBUG)
    fun i(tag: String, message: String) = log(tag, message, LogType.INFO)
    fun w(tag: String, message: String) = log(tag, message, LogType.WARN)
    fun e(tag: String, message: String, throwable: Throwable? = null) = 
        log(tag, "$message ${throwable?.message ?: ""}", LogType.ERROR)

    fun clear() { _logs.value = emptyList() }

    fun getFormattedLogs(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return _logs.value.joinToString("\n") { 
            "${sdf.format(Date(it.timestamp))} [${it.type}] ${it.tag}: ${it.message}" 
        }
    }
}
