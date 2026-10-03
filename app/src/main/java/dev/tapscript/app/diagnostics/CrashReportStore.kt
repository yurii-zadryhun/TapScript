package dev.tapscript.app.diagnostics

import android.content.Context
import java.io.File

class CrashReportStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        if (previousHandler != null) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler = previous
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                file.writeText(
                    buildString {
                        appendLine(System.currentTimeMillis())
                        appendLine(thread.name)
                        append(throwable.stackTraceToString())
                    },
                    Charsets.UTF_8,
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(): CrashReport? = runCatching {
        if (!file.exists()) return@runCatching null
        val lines = file.readLines(Charsets.UTF_8)
        val timestamp = lines.firstOrNull()?.toLongOrNull() ?: return@runCatching null
        CrashReport(
            timestampEpochMs = timestamp,
            threadName = lines.getOrNull(1).orEmpty(),
            stackTrace = lines.drop(2).joinToString("\n").take(MAX_STACK_CHARS),
        )
    }.getOrNull()

    fun clear() {
        runCatching { if (file.exists()) file.delete() }
    }

    companion object {
        private const val FILE_NAME = "last-crash.txt"
        private const val MAX_STACK_CHARS = 40_000
    }
}

data class CrashReport(
    val timestampEpochMs: Long,
    val threadName: String,
    val stackTrace: String,
)
