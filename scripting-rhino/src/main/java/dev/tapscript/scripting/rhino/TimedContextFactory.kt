package dev.tapscript.scripting.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EvaluatorException

internal class TimedContextFactory(
    private val timeoutMs: Long,
) : ContextFactory() {
    private val deadlineNanos = ThreadLocal<Long>()

    fun <T> withContext(block: (Context) -> T): T {
        deadlineNanos.set(System.nanoTime() + timeoutMs * 1_000_000)
        val context = enterContext()
        return try {
            block(context)
        } finally {
            Context.exit()
            deadlineNanos.remove()
        }
    }

    override fun makeContext(): Context = super.makeContext().apply {
        optimizationLevel = -1 // Required for reliable interpreted execution on Android.
        languageVersion = Context.VERSION_ES6
        instructionObserverThreshold = 10_000
    }

    override fun observeInstructionCount(context: Context, instructionCount: Int) {
        val deadline = deadlineNanos.get() ?: return
        if (System.nanoTime() > deadline) {
            throw EvaluatorException("Script exceeded ${timeoutMs}ms execution limit")
        }
    }
}
