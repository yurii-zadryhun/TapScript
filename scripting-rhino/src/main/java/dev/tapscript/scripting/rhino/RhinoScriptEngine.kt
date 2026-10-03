package dev.tapscript.scripting.rhino

import com.google.gson.Gson
import dev.tapscript.engine.api.command.AutomationCommand
import dev.tapscript.engine.api.ports.ScriptEngine
import dev.tapscript.engine.api.ports.ScriptRequest
import dev.tapscript.engine.api.ports.ScriptResult
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

class RhinoScriptEngine(
    private val gson: Gson = Gson(),
    timeoutMs: Long = 100,
    private val commandLimit: Int = 256,
) : ScriptEngine {
    private val contextFactory = TimedContextFactory(timeoutMs)

    override fun evaluate(request: ScriptRequest): ScriptResult {
        val commands = mutableListOf<AutomationCommand>()
        return runCatching {
            contextFactory.withContext { context ->
                val scope = context.initSafeStandardObjects()
                installVariables(context, scope, request.variables)
                installCommands(scope, commands)
                context.evaluateString(scope, request.source, "profile.js", 1, null)
            }
            ScriptResult(commands = commands.toList())
        }.getOrElse { throwable ->
            ScriptResult(
                commands = emptyList(),
                error = throwable.message ?: throwable::class.java.simpleName,
            )
        }
    }

    private fun installVariables(context: Context, scope: Scriptable, values: Map<String, Any?>) {
        val nested = NestedVariableMap.fromFlat(values)
        val json = gson.toJson(nested)
        context.evaluateString(scope, "var vars = $json;", "variables.js", 1, null)
    }

    private fun installCommands(scope: Scriptable, commands: MutableList<AutomationCommand>) {
        putFunction(scope, "tap") { args ->
            addCommand(commands, AutomationCommand.Tap(requiredString(args, 0, "tap")))
        }
        putFunction(scope, "tapRandom") { args ->
            addCommand(
                commands,
                AutomationCommand.RandomTap(
                    targetId = requiredString(args, 0, "tapRandom"),
                    radiusPx = requiredLong(args, 1, "tapRandom").coerceIn(0, MAX_POSITION_JITTER_PX.toLong()).toInt(),
                ),
            )
        }
        putFunction(scope, "swipe") { args ->
            addCommand(commands, AutomationCommand.Swipe(requiredString(args, 0, "swipe")))
        }
        putFunction(scope, "swipeRandom") { args ->
            addCommand(
                commands,
                AutomationCommand.RandomSwipe(
                    targetId = requiredString(args, 0, "swipeRandom"),
                    radiusPx = requiredLong(args, 1, "swipeRandom").coerceIn(0, MAX_POSITION_JITTER_PX.toLong()).toInt(),
                    durationJitterMs = optionalLong(args, 2, 0).coerceIn(0, MAX_WAIT_MS),
                ),
            )
        }
        putFunction(scope, "waitMs") { args ->
            val duration = requiredLong(args, 0, "waitMs").coerceIn(0, MAX_WAIT_MS)
            addCommand(commands, AutomationCommand.Wait(duration))
        }
        putFunction(scope, "waitRandom") { args ->
            val min = requiredLong(args, 0, "waitRandom").coerceIn(0, MAX_WAIT_MS)
            val max = requiredLong(args, 1, "waitRandom").coerceIn(0, MAX_WAIT_MS)
            require(min <= max) { "waitRandom minimum must not exceed maximum" }
            addCommand(commands, AutomationCommand.RandomWait(min, max))
        }
        putFunction(scope, "log") { args ->
            addCommand(commands, AutomationCommand.Log(Context.toString(args.getOrNull(0))))
        }
    }

    private fun putFunction(scope: Scriptable, name: String, callback: (Array<out Any?>) -> Unit) {
        val function = object : BaseFunction() {
            override fun call(
                context: Context,
                functionScope: Scriptable,
                thisObject: Scriptable,
                args: Array<out Any?>,
            ): Any {
                callback(args)
                return Undefined.instance
            }
        }
        ScriptableObject.putProperty(scope, name, function)
    }

    private fun addCommand(commands: MutableList<AutomationCommand>, command: AutomationCommand) {
        require(commands.size < commandLimit) { "Script emitted more than $commandLimit commands" }
        commands += command
    }

    private fun requiredString(args: Array<out Any?>, index: Int, function: String): String {
        val value = args.getOrNull(index)
        require(value != null && value != Undefined.instance) { "$function requires an action id" }
        return Context.toString(value)
    }

    private fun requiredLong(args: Array<out Any?>, index: Int, function: String): Long {
        val value = args.getOrNull(index)
        require(value != null && value != Undefined.instance) { "$function requires argument ${index + 1}" }
        val number = Context.toNumber(value)
        require(number.isFinite()) { "$function argument ${index + 1} must be a finite number" }
        return number.toLong()
    }

    private fun optionalLong(args: Array<out Any?>, index: Int, default: Long): Long {
        val value = args.getOrNull(index)
        if (value == null || value == Undefined.instance) return default
        val number = Context.toNumber(value)
        require(number.isFinite()) { "Optional numeric argument ${index + 1} must be finite" }
        return number.toLong()
    }

    private companion object {
        const val MAX_POSITION_JITTER_PX = 1_000
        const val MAX_WAIT_MS = 60_000L
    }
}
