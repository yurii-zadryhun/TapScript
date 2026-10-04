package dev.tapscript.app

/**
 * Dungeon Rush loot advisor.
 *
 * This profile intentionally runs in shadow mode: it scores the visible candidate and equipped
 * item, highlights the recommendation, and shows an explainable breakdown without tapping Sell or
 * Equip. The user can validate recommendations against real loot before enabling destructive
 * automation later.
 */
internal object LootEvaluatorScript {
    val source: String = """
const categoryWeights = {
  excellent: 180,
  great: 100,
  good: 55,
  ok: 12,
  bad: 2
};

const maxItemLevel = 130;
const levelWeight = 30;
const minimumStatsPerNonMeleeItem = 4;
const maximumStatsPerItem = 5;

const skills = {
  "mega crit chance":       { cap: 10,  category: "excellent" },
  "triple hit chance":      { cap: 30,  category: "excellent" },
  "ranged defense":         { cap: 100, category: "excellent" },
  "critical damage taken":  { cap: 66,  category: "excellent" },

  "melee defense":          { cap: 100, category: "great" },
  "attack speed":           { cap: 40,  category: "great" },
  "critical chance":        { cap: 12,  category: "great" },
  "critical damage":        { cap: 100, category: "great" },

  "damage":                 { cap: 15,  category: "good" },
  "ranged damage":          { cap: 15,  category: "good" },
  "lifesteal":              { cap: 20,  category: "good" },

  "block chance":           { cap: 5,   category: "ok" },
  "double hit chance":      { cap: 40,  category: "ok" },
  "health":                 { cap: 15,  category: "ok" },

  "thorns":                 { cap: 15,  category: "bad" },
  "movement speed":         { cap: 20,  category: "bad" },
  "knockback":              { cap: 15,  category: "bad" },
  "health regen":           { cap: 6,   category: "bad" },
  "melee damage":           { cap: 50,  category: "bad" },
  "companion damage":       { cap: 30,  category: "bad" },
  "companion cooldown":     { cap: 7,   category: "bad" }
};

const aliases = {
  "mega crit": "mega crit chance",
  "triple hit": "triple hit chance",
  "double hit": "double hit chance",
  "crit chance": "critical chance",
  "crit damage": "critical damage",
  "crit damage taken": "critical damage taken",
  "melee defence": "melee defense",
  "ranged defence": "ranged defense",
  "regen": "health regen"
};

function normalizeName(name) {
  const normalized = String(name || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
  return aliases[normalized] || normalized;
}

function displayName(name) {
  return String(name || "").replace(/\b\w/g, function (letter) { return letter.toUpperCase(); });
}

function isMeleeWeapon(text) {
  // The game marks melee weapons in the item title, e.g. "Divine Mace [Melee]".
  // Accept the common transposition "[Meele]" too so a small OCR error cannot make us recommend it.
  return /\[\s*(?:melee|meele)\s*\]/i.test(String(text || ""));
}

function finiteNumber(value) {
  const number = Number(value);
  return isFinite(number) ? number : null;
}

function beneficialMagnitude(name, rawValue) {
  const value = finiteNumber(rawValue);
  if (value === null) return null;
  if (name === "critical damage taken") return Math.abs(value);
  return Math.max(0, value);
}

function scoreSkills(rows) {
  let total = 0;
  const details = [];
  const unknown = [];

  (rows || []).forEach(function (row) {
    const name = normalizeName(row.name);
    const config = skills[name];
    const value = beneficialMagnitude(name, row.value);
    if (!config || value === null) {
      unknown.push(String(row && row.name || "unknown"));
      return;
    }

    const normalized = Math.min(1, value / config.cap);
    const contribution = normalized * categoryWeights[config.category];
    total += contribution;
    details.push({
      name: name,
      value: value,
      contribution: contribution
    });
  });

  details.sort(function (left, right) { return right.contribution - left.contribution; });
  return { total: total, details: details, unknown: unknown };
}

function itemScore(rows, level, useLevels) {
  const scoredSkills = scoreSkills(rows);
  let levelScore = 0;

  if (useLevels) {
    const numericLevel = finiteNumber(level);
    if (numericLevel !== null) {
      levelScore = Math.max(0, Math.min(maxItemLevel, numericLevel)) / maxItemLevel * levelWeight;
    }
  }

  return {
    total: scoredSkills.total + levelScore,
    skills: scoredSkills.total,
    level: levelScore,
    details: scoredSkills.details,
    unknown: scoredSkills.unknown
  };
}

function validStatCount(rows) {
  const count = rows ? rows.length : 0;
  return count >= minimumStatsPerNonMeleeItem && count <= maximumStatsPerItem;
}

function scoreSummary(label, score) {
  return label + " " + score.total.toFixed(1) +
    " (skills " + score.skills.toFixed(1) + ", level " + score.level.toFixed(1) + ")";
}

function topContributions(score, limit) {
  return score.details.slice(0, limit).map(function (detail) {
    return displayName(detail.name) + " +" + detail.contribution.toFixed(1);
  }).join(" · ");
}

function signed(value) {
  return (value >= 0 ? "+" : "") + value.toFixed(1);
}

function marginLabel(delta) {
  const magnitude = Math.abs(delta);
  if (magnitude < 5) return "VERY CLOSE";
  if (magnitude < 15) return "CLOSE";
  if (magnitude < 30) return "CLEAR";
  return "STRONG";
}

function clearDecisionGeometry() {
  clearVisual("loot-item");
  clearVisual("loot-action");
}

function showWaiting(message) {
  clearDecisionGeometry();
  showInfo("loot-info", "CHECK OCR", message, "warning");
  log("Loot advisor waiting: " + message);
}

const candidate = vars.candidate || {};
const equipped = vars.equipped || {};
const candidateRows = candidate.stats;
const equippedRows = equipped.stats;

if (isMeleeWeapon(candidate.text)) {
  highlight("loot-item", "candidate", "MELEE — REJECT", "danger");
  highlight("loot-action", "sell", "SELL", "danger");
  showInfo(
    "loot-info",
    "SELL · melee weapon",
    "Candidate title contains [Melee]. Melee weapon candidates are always rejected by this profile.\nShadow mode: no tap was sent.",
    "danger"
  );
  log("Recommendation SELL: candidate is tagged [Melee]");
} else if (!candidateRows || !equippedRows) {
  showWaiting("Both candidate and equipped stat regions must parse before a recommendation is shown.");
} else if (!validStatCount(candidateRows) || !validStatCount(equippedRows)) {
  showWaiting(
    "Expected 4–5 parsed stats on each non-melee item, got candidate=" + candidateRows.length +
      " and equipped=" + equippedRows.length + ". No recommendation shown."
  );
} else {
  const candidateLevel = finiteNumber(candidate.level);
  const equippedLevel = finiteNumber(equipped.level);
  const useLevels = candidateLevel !== null && equippedLevel !== null;
  const candidateScore = itemScore(candidateRows, candidateLevel, useLevels);
  const equippedScore = itemScore(equippedRows, equippedLevel, useLevels);
  const unknown = candidateScore.unknown.concat(equippedScore.unknown);

  if (unknown.length) {
    showWaiting("Unknown or invalid stat text: " + unknown.join(", ") + ". Check OCR before deciding.");
  } else {
    const delta = candidateScore.total - equippedScore.total;
    const candidateWins = delta > 0.01;
    const actionId = candidateWins ? "equip" : "sell";
    const itemId = candidateWins ? "candidate" : "equipped";
    const tone = candidateWins ? "success" : "warning";
    const decision = candidateWins ? "EQUIP" : "KEEP CURRENT";
    const actionLabel = candidateWins ? "EQUIP" : "SELL CANDIDATE";

    highlight("loot-item", itemId, decision + " " + signed(delta), tone);
    highlight("loot-action", actionId, actionLabel, tone);

    const body =
      scoreSummary("Candidate", candidateScore) + "\n" +
      scoreSummary("Equipped", equippedScore) + "\n" +
      "Delta " + signed(delta) + " · margin " + marginLabel(delta) + "\n" +
      "Candidate: " + topContributions(candidateScore, 3) + "\n" +
      "Equipped: " + topContributions(equippedScore, 3) + "\n" +
      (useLevels ? "" : "Level ignored because both levels did not parse.\n") +
      "Context: calibrated stateless scoring · shadow mode (no tap).";

    showInfo(
      "loot-info",
      decision + " · " + signed(delta) + " · " + marginLabel(delta),
      body,
      tone
    );

    log(
      "Loot recommendation " + decision + " | " +
      scoreSummary("candidate", candidateScore) + " | " +
      scoreSummary("equipped", equippedScore) + " | delta " + signed(delta) +
      " | shadow mode"
    );
  }
}
""".trimIndent()
}
