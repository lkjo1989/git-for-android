package com.gitforandroid.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple file-based logger for debugging and crash analysis.
 * Writes timestamped messages to a log file in app-private external storage.
 * Controlled by the "log_enabled" setting (default: enabled).
 */
object FileLogger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "gitforandroid.log"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024L // 5 MB

    @Volatile
    var enabled: Boolean = true

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun getLogFile(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), LOG_DIR)
        dir.mkdirs()
        return File(dir, LOG_FILE)
    }

    /**
     * Returns the absolute path to the log file for display in settings / sharing.
     */
    fun getLogPath(context: Context): String = getLogFile(context).absolutePath

    suspend fun info(context: Context, tag: String, message: String) {
        log(context, "INFO", tag, message, null)
    }

    suspend fun warn(context: Context, tag: String, message: String) {
        log(context, "WARN", tag, message, null)
    }

    suspend fun error(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        log(context, "ERROR", tag, message, throwable)
    }

    private suspend fun log(
        context: Context,
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?
    ) = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext
        try {
            val logFile = getLogFile(context)

            // Rotate if too large
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                val backup = File(logFile.parentFile, "gitforandroid.old.log")
                backup.delete()
                logFile.renameTo(backup)
            }

            val timestamp = dateFormat.format(Date())
            val sb = StringBuilder()
            sb.appendLine("$timestamp $level/$tag: $message")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                sb.appendLine(sw.toString())
            }

            FileWriter(logFile, true).use { it.write(sb.toString()) }
        } catch (_: Exception) {
            // Silently ignore logging failures — don't crash the app
        }
    }
}
