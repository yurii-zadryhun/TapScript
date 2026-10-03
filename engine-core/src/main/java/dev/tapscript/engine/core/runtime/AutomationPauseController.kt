package dev.tapscript.engine.core.runtime

/**
 * Holds independent pause requests from UI/runtime concerns.
 * A caller only resumes the pause token it owns, so opening an overlay cannot
 * accidentally resume a session that the user paused manually.
 */
class AutomationPauseController {
    private val lock = Any()
    private val reasons = linkedMapOf<String, String>()

    fun pause(token: String, message: String) {
        require(token.isNotBlank()) { "Pause token cannot be blank" }
        synchronized(lock) {
            reasons[token] = message.ifBlank { "Paused" }
        }
    }

    fun resume(token: String) {
        synchronized(lock) {
            reasons.remove(token)
        }
    }

    fun clear() {
        synchronized(lock) {
            reasons.clear()
        }
    }

    fun currentReason(): String? = synchronized(lock) {
        reasons.values.firstOrNull()
    }

    fun isPaused(): Boolean = synchronized(lock) { reasons.isNotEmpty() }

    fun isPaused(token: String): Boolean = synchronized(lock) { reasons.containsKey(token) }

    companion object {
        const val MANUAL_TOKEN = "manual"
        const val OVERLAY_TOKEN = "overlay-workspace"
    }
}
