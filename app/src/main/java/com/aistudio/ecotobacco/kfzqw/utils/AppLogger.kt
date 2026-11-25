package com.aistudio.ecotobacco.kfzqw.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    enum class LogType {
        DEBUG, INFO, WARNING, WARN, ERROR
    }

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val tag: String = DEFAULT_TAG,
        val message: String,
        val type: LogType = LogType.DEBUG
    )

    private const val DEFAULT_TAG = "EcoTobacco"
    private const val MAX_LOGS = 200
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun d(message: String) = log(DEFAULT_TAG, message, LogType.DEBUG)
    fun d(tag: String, message: String) = log(tag, message, LogType.DEBUG)

    fun i(message: String) = log(DEFAULT_TAG, message, LogType.INFO)
    fun i(tag: String, message: String) = log(tag, message, LogType.INFO)

    fun w(message: String) = log(DEFAULT_TAG, message, LogType.WARN)
    fun w(tag: String, message: String) = log(tag, message, LogType.WARN)

    fun e(message: String, throwable: Throwable? = null) {
        val msg = if (throwable?.message != null) "$message: ${throwable.message}" else message
        Log.e(DEFAULT_TAG, msg, throwable)
        appendEntry(DEFAULT_TAG, msg, LogType.ERROR)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable?.message != null) "$message: ${throwable.message}" else message
        Log.e(tag, msg, throwable)
        appendEntry(tag, msg, LogType.ERROR)
    }

    fun log(tag: String, message: String, type: LogType = LogType.INFO) {
        when (type) {
            LogType.DEBUG -> Log.d(tag, message)
            LogType.INFO -> Log.i(tag, message)
            LogType.WARNING, LogType.WARN -> Log.w(tag, message)
            LogType.ERROR -> Log.e(tag, message)
        }
        appendEntry(tag, message, type)
    }

    private fun appendEntry(tag: String, message: String, type: LogType) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            type = type
        )
        val updated = (_logs.value + entry).let { list ->
            if (list.size > MAX_LOGS) list.takeLast(MAX_LOGS) else list
        }
        _logs.value = updated
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getFormattedLogs(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return _logs.value.joinToString("\n") {
            "${sdf.format(Date(it.timestamp))} [${it.type}] ${it.tag}: ${it.message}"
        }
    }
}
