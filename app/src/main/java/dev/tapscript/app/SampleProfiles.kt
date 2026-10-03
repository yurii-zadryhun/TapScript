package dev.tapscript.app

import dev.tapscript.engine.api.model.*

object SampleProfiles {
    fun lootEvaluator(): AutomationProfile = AutomationProfile(
        id = "sample-loot-evaluator",
        name = "Loot evaluator demo",
        targetPackage = "",
        regions = listOf(
            statsRegion(
                id = "equipped",
                name = "Equipped item stats",
                bounds = NormalizedRect(0.32f, 0.54f, 0.92f, 0.68f),
                variable = "equipped.stats",
            ),
            statsRegion(
                id = "candidate",
                name = "Candidate item stats",
                bounds = NormalizedRect(0.32f, 0.70f, 0.92f, 0.83f),
                variable = "candidate.stats",
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
            script = SAMPLE_SCRIPT.trimIndent(),
        ),
        settings = RuntimeSettings(
            minFrameIntervalMs = 120,
            changeThreshold = 0.015,
            postActionCooldownMs = 250,
        ),
    )

    private fun statsRegion(
        id: String,
        name: String,
        bounds: NormalizedRect,
        variable: String,
    ) = RecognitionRegion(
        id = id,
        name = name,
        bounds = bounds,
        textConfig = TextRecognitionConfig(
            extractors = listOf(
                RegexExtractorSpec(
                    variable = variable,
                    pattern = "\\+(?<amount>\\d+(?:[.,]\\d+)?)%\\s+(?<name>[^\\n]+)",
                    matchMode = MatchMode.ALL,
                    fields = listOf(
                        RegexFieldSpec("value", "amount", ValueType.NUMBER),
                        RegexFieldSpec("name", "name", ValueType.TEXT),
                    ),
                ),
            ),
        ),
    )

    private const val SAMPLE_SCRIPT = """
const caps = {
  "Attack Speed": 40,
  "Double Hit Chance": 40,
  "Critical Damage": 100,
  "Damage": 15,
  "Lifesteal": 20,
  "Triple Hit Chance": 30
};

const weights = {
  "Attack Speed": 100,
  "Double Hit Chance": 100,
  "Critical Damage": 80,
  "Damage": 50,
  "Lifesteal": 15,
  "Triple Hit Chance": 140
};

function score(rows) {
  if (!rows) return 0;
  let total = 0;
  rows.forEach(function (row) {
    const cap = caps[row.name];
    const weight = weights[row.name];
    if (cap && weight) total += (Number(row.value) / cap) * weight;
  });
  return total;
}

const candidateRows = vars.candidate && vars.candidate.stats;
const equippedRows = vars.equipped && vars.equipped.stats;

if (!candidateRows || !candidateRows.length || !equippedRows || !equippedRows.length) {
  log("Waiting for both item stat regions to parse");
} else {
  const candidateScore = score(candidateRows);
  const equippedScore = score(equippedRows);

  if (candidateScore > equippedScore) {
    log("Candidate wins: " + candidateScore.toFixed(1) + " > " + equippedScore.toFixed(1));
    tap("equip");
  } else {
    log("Keep equipped: " + equippedScore.toFixed(1) + " >= " + candidateScore.toFixed(1));
    tap("sell");
  }
  waitMs(120);
}
"""
}
