package dev.tapscript.engine.core.decision

import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.AutomationSnapshot
import dev.tapscript.engine.api.model.LogicMode
import dev.tapscript.engine.api.ports.AutomationLogger
import dev.tapscript.engine.api.ports.DecisionEngine
import dev.tapscript.engine.api.ports.DecisionResult
import dev.tapscript.engine.api.ports.ScriptEngine
import dev.tapscript.engine.api.ports.ScriptRequest

class DefaultDecisionEngine(
    private val ruleEvaluator: RuleEvaluator,
    private val scriptEngine: ScriptEngine,
    private val logger: AutomationLogger,
) : DecisionEngine {
    override fun decide(
        profile: AutomationProfile,
        snapshot: AutomationSnapshot,
    ): DecisionResult = when (profile.logic.mode) {
        LogicMode.RULES -> DecisionResult(
            commands = ruleEvaluator.evaluate(profile.logic.rules, snapshot),
        )
        LogicMode.JAVASCRIPT -> {
            val result = scriptEngine.evaluate(
                ScriptRequest(
                    source = profile.logic.script,
                    variables = snapshot.values,
                ),
            )
            result.error?.let { error ->
                logger.error("Script error: $error")
                return DecisionResult(error = error)
            }
            DecisionResult(commands = result.commands)
        }
    }
}
