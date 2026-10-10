package com.achyutpal.scrolljourney.data.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Saves the details of an app crash to a small file on the phone (the last [MAX_FILES] are kept),
 * so a tester can send them with "Share diagnostics". The app has no internet: nothing is uploaded.
 *
 * Installed once in Application.onCreate as the default *uncaught exception handler*: it writes the
 * report, then hands the crash to the previous handler so Android still shows its normal crash.
 */
class CrashLog(context: Context) {

    private val dir = File(context.filesDir, "crashes")

    fun install(appVersion: String) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(thread.name, error, appVersion) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The saved reports, newest first. */
    fun reports(): List<String> =
        dir.listFiles { f -> f.name.endsWith(".txt") }.orEmpty()
            .sortedByDescending { it.name }
            .map { it.readText() }

    /** Saves one crash report (internal so the on-device test can call it without crashing). */
    internal fun write(threadName: String, error: Throwable, appVersion: String) {
        dir.mkdirs()
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val text = buildString {
            appendLine("Crash at ${Instant.now()} on thread \"$threadName\"")
            appendLine("App $appVersion · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}")
            append(trace.take(MAX_TRACE_CHARS))
        }
        File(dir, "crash-${System.currentTimeMillis()}.txt").writeText(text)
        dir.listFiles().orEmpty().sortedByDescending { it.name }.drop(MAX_FILES).forEach { it.delete() }
    }

    private companion object {
        const val MAX_FILES = 5
        const val MAX_TRACE_CHARS = 12_000
    }
}
