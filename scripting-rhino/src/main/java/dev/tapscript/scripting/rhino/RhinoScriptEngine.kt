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
        putFunction(scope, "swipe") { args ->
            addCommand(commands, AutomationCommand.Swipe(requiredString(args, 0, "swipe")))
        }
        putFunction(scope, "waitMs") { args ->
            val duration = Context.toNumber(args.getOrNull(0)).toLong().coerceIn(0, 60_000)
            addCommand(commands, AutomationCommand.Wait(duration))
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
}
