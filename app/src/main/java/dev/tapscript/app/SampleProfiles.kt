package dev.tapscript.app

import dev.tapscript.engine.api.model.ActionKind
import dev.tapscript.engine.api.model.ActionTarget
import dev.tapscript.engine.api.model.AutomationLogic
import dev.tapscript.engine.api.model.AutomationProfile
import dev.tapscript.engine.api.model.LogicMode
import dev.tapscript.engine.api.model.MatchMode
import dev.tapscript.engine.api.model.NormalizedPoint
import dev.tapscript.engine.api.model.NormalizedRect
import dev.tapscript.engine.api.model.RecognitionRegion
import dev.tapscript.engine.api.model.RegexExtractorSpec
import dev.tapscript.engine.api.model.RegexFieldSpec
import dev.tapscript.engine.api.model.RuntimeSettings
import dev.tapscript.engine.api.model.TextRecognitionConfig
import dev.tapscript.engine.api.model.ValueType

object SampleProfiles {
    private const val LOOT_PROFILE_ID = "sample-loot-evaluator"

    fun lootEvaluator(): AutomationProfile = AutomationProfile(
        id = LOOT_PROFILE_ID,
        name = "Dungeon Rush loot evaluator",
        targetPackage = "com.lavalabs.dungeonrush",
        regions = listOf(
            statsRegion(
                id = "equipped",
                name = "Equipped item",
                bounds = NormalizedRect(0.32f, 0.54f, 0.92f, 0.68f),
            ),
            statsRegion(
                id = "candidate",
                name = "Candidate item",
                bounds = NormalizedRect(0.32f, 0.70f, 0.92f, 0.83f),
            ),
        ),
        actions = listOf(
            ActionTarget(
                id = "sell",
                name = "Sell",
                kind = ActionKind.TAP,
                start = NormalizedPoint(0.31f, 0.86f),
            ),
            ActionTarget(
                id = "equip",
                name = "Equip",
                kind = ActionKind.TAP,
                start = NormalizedPoint(0.68f, 0.86f),
            ),
        ),
        logic = AutomationLogic(
            mode = LogicMode.JAVASCRIPT,
            script = LootEvaluatorScript.source,
        ),
        settings = RuntimeSettings(
            minFrameIntervalMs = 120,
            changeThreshold = 0.015,
            postActionCooldownMs = 250,
        ),
    )

    /**
     * Updates only the known first-generation demo script. User-authored scripts are never overwritten.
     * Geometry, target app, names, actions, and runtime settings stay untouched.
     */
    fun upgradeLegacyLootEvaluator(profile: AutomationProfile): AutomationProfile? {
        if (profile.id != LOOT_PROFILE_ID) return null
        val script = profile.logic.script
        val looksLikeLegacyDemo =
            script.contains("Candidate wins:") &&
                script.contains("\"Triple Hit Chance\": 140") &&
                !script.contains("mega crit chance", ignoreCase = true)
        if (!looksLikeLegacyDemo) return null

        val newRegionConfig = lootEvaluator().regions.associateBy { it.id }
        return profile.copy(
            regions = profile.regions.map { region ->
                newRegionConfig[region.id]?.let { replacement ->
                    region.copy(textConfig = replacement.textConfig)
                } ?: region
            },
            logic = profile.logic.copy(
                mode = LogicMode.JAVASCRIPT,
                script = LootEvaluatorScript.source,
            ),
        )
    }

    private fun statsRegion(
        id: String,
        name: String,
        bounds: NormalizedRect,
    ) = RecognitionRegion(
        id = id,
        name = name,
        bounds = bounds,
        textConfig = TextRecognitionConfig(
            extractors = listOf(
                RegexExtractorSpec(
                    variable = "$id.stats",
                    pattern = "(?<amount>[+-]?\\d+(?:[.,]\\d+)?)%\\s+(?<name>[^\\n]+)",
                    matchMode = MatchMode.ALL,
                    fields = listOf(
                        RegexFieldSpec("value", "amount", ValueType.NUMBER),
                        RegexFieldSpec("name", "name", ValueType.TEXT),
                    ),
                ),
                RegexExtractorSpec(
                    variable = "$id.level",
                    pattern = "(?:Lv\\.?|Lvl\\.?|Level)\\s*[:.]?\\s*(?<value>\\d{1,3})",
                    matchMode = MatchMode.FIRST,
                    fields = listOf(
                        RegexFieldSpec("value", "value", ValueType.NUMBER),
                    ),
                    ignoreCase = true,
                ),
            ),
        ),
    )
}
