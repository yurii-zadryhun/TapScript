package dev.tapscript.engine.api.ports

import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSnapshot
import dev.tapscript.engine.api.model.PixelPoint

interface GestureDispatcher {
    suspend fun tap(point: PixelPoint): Result<Unit>
    suspend fun swipe(start: PixelPoint, end: PixelPoint, durationMs: Long): Result<Unit>
}

interface ScriptEngine {
    fun evaluate(request: ScriptRequest): ScriptResult
}

data class ScriptRequest(
    val source: String,
    val variables: Map<String, Any?>,
)

data class ScriptResult(
    val commands: List<AutomationCommand>,
    val error: String? = null,
)

interface ProfileRepository {
    suspend fun list(): List<AutomationProfile>
    suspend fun get(id: String): AutomationProfile?
    suspend fun save(profile: AutomationProfile)
    suspend fun delete(id: String)
}

interface AutomationLogger {
    fun debug(message: String)
    fun info(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

data class DecisionResult(
    val commands: List<AutomationCommand> = emptyList(),
    val error: String? = null,
)

interface DecisionEngine {
    fun decide(profile: AutomationProfile, snapshot: AutomationSnapshot): DecisionResult
}
