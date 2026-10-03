package dev.tapscript.app

/**
 * Default Dungeon Rush loot comparison policy.
 *
 * Skill contribution = roll / cap * category weight. Item level is only a
 * tie-breaker-scale contribution and is used only when both item levels were
 * recognized. A candidate explicitly identified as a melee weapon is rejected.
 */
internal object LootEvaluatorScript {
    val source: String = """
const categoryWeights = {
  excellent: 140,
  great: 100,
  good: 50,
  ok: 12,
  bad: 2
};

const maxItemLevel = 130;
const levelWeight = 30;

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

function isMeleeWeapon(text) {
  return /\bmelee\s+weapon\b/i.test(String(text || ""));
}

function finiteNumber(value) {
  const number = Number(value);
  return isFinite(number) ? number : null;
}

function skillScore(rows) {
  let total = 0;
  const details = [];
  (rows || []).forEach(function (row) {
    const name = normalizeName(row.name);
    const config = skills[name];
    const value = finiteNumber(row.value);
    if (!config || value === null) return;

    // Crit Damage Taken is normally a negative roll; magnitude represents benefit.
    const normalized = Math.min(1, Math.abs(value) / config.cap);
    const contribution = normalized * categoryWeights[config.category];
    total += contribution;
    details.push(name + "=" + contribution.toFixed(1));
  });
  return { total: total, details: details };
}

function itemScore(rows, level, rawText, useLevels) {
  if (isMeleeWeapon(rawText)) {
    return { total: -1000000, skills: 0, level: 0, details: ["melee weapon"] };
  }

  const scoredSkills = skillScore(rows);
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
    details: scoredSkills.details
  };
}

function summary(label, score) {
  return label + "=" + score.total.toFixed(1) +
    " (skills " + score.skills.toFixed(1) + ", level " + score.level.toFixed(1) + ")";
}

const candidate = vars.candidate || {};
const equipped = vars.equipped || {};
const candidateRows = candidate.stats;
const equippedRows = equipped.stats;

if (!candidateRows || !candidateRows.length || !equippedRows || !equippedRows.length) {
  log("Waiting for both item stat regions to parse");
} else if (isMeleeWeapon(candidate.text)) {
  log("Reject candidate: melee weapon");
  tapRandom("sell", 8);
  waitRandom(100, 170);
} else {
  const candidateLevel = finiteNumber(candidate.level);
  const equippedLevel = finiteNumber(equipped.level);
  const useLevels = candidateLevel !== null && equippedLevel !== null;

  const candidateScore = itemScore(candidateRows, candidateLevel, candidate.text, useLevels);
  const equippedScore = itemScore(equippedRows, equippedLevel, equipped.text, useLevels);

  log(summary("candidate", candidateScore) + " | " + summary("equipped", equippedScore) +
      (useLevels ? "" : " | level ignored until both levels parse"));

  if (candidateScore.total > equippedScore.total + 0.01) {
    tapRandom("equip", 8);
  } else {
    tapRandom("sell", 8);
  }
  waitRandom(100, 170);
}
""".trimIndent()
}
