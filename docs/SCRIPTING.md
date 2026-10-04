# Scripting API

TapScript embeds Rhino JavaScript behind a small command API. Scripts receive recognized variables and emit commands; Android objects are not exposed to JavaScript.

## Variables

Recognition variables are exposed as the global `vars` object. Dot-separated extractor names are expanded into nested objects.

```text
candidate.attackSpeed = 18.32
candidate.doubleHit   = 31.68
equipped.attackSpeed  = 24.10
```

becomes:

```javascript
vars.candidate.attackSpeed
vars.candidate.doubleHit
vars.equipped.attackSpeed
```

Every text region also exposes raw OCR text as `vars.<regionId>.text`.

## Gesture and timing commands

Deterministic commands remain the default:

```javascript
tap("equip");
swipe("scroll");
waitMs(150);
log("candidate accepted");
```

For workflows that intentionally need timing or coordinate variability, use the explicit randomized variants:

```javascript
tapRandom("equip", 12);          // point sampled inside a 12 px radius
swipeRandom("scroll", 8, 40);    // start/end ±8 px, duration ±40 ms
waitRandom(120, 180);             // inclusive random duration in milliseconds
```

Randomized coordinates are sampled at execution time, clamped to the physical screen, and never change the stored action target. `tapRandom` and `swipeRandom` accept up to 1000 px of position jitter. Waits and swipe-duration jitter are bounded to 60 seconds.

`log(message)` writes through the same application logger used by the runtime, so script messages are visible in live logs and persistent run history.

## Runtime visual commands

Scripts can explain a decision without touching the target app:

```javascript
highlight("loot-action", "equip", "EQUIP +34.7", "success");
showInfo(
  "loot-details",
  "EQUIP · +34.7 · STRONG",
  "Candidate 351.4\nEquipped 316.7\nDelta +34.7",
  "success"
);
```

`highlight(key, targetId, label?, tone?)` highlights either a named action target or a named recognition region. The same id must not exist as both a region and an action.

`showInfo(key, title, body?, tone?)` shows a compact non-interactive information card over the target app.

Visuals use **stable keys**. Emitting the same key again updates the existing visual instead of stacking another copy. This is important because a profile script may execute many times during one run.

```javascript
clearVisual("loot-action");
clearVisuals();
```

Supported tones are:

```text
neutral
info
success  (alias: positive)
warning  (alias: warn)
danger   (aliases: error, negative)
```

Runtime visuals are intentionally non-touchable. They disappear when the profile stops, errors out, is paused, or leaves its target app. Visual-only commands do not trigger the post-action gesture cooldown.

Region highlights are rendered outside the configured OCR rectangle. MediaProjection may capture application overlays, so drawing over recognized text could otherwise contaminate OCR or trigger a frame-change feedback loop.

## Sandbox philosophy

The script is not given a `Context`, filesystem, network client, AccessibilityService, reflection bridge, or MediaProjection object. It only receives plain serialized data and command functions.

This is not a hardened hostile-code sandbox. Profiles are assumed to be authored by the device owner. The boundary keeps profile logic decoupled from Android implementation details and makes behavior testable.

## Example: advisory decision

```javascript
function score(item) {
  if (!item) return 0;
  return Number(item.attackSpeed || 0) * 2 + Number(item.damage || 0);
}

const candidateScore = score(vars.candidate);
const equippedScore = score(vars.equipped);
const delta = candidateScore - equippedScore;

if (delta > 0) {
  highlight("decision", "equip", "EQUIP +" + delta.toFixed(1), "success");
} else {
  highlight("decision", "sell", "KEEP CURRENT", "warning");
}

showInfo(
  "decision-info",
  delta > 0 ? "EQUIP" : "KEEP CURRENT",
  "Candidate " + candidateScore.toFixed(1) + "\nEquipped " + equippedScore.toFixed(1),
  delta > 0 ? "success" : "warning"
);
```

## Recommended script style

Keep scripts pure where possible: compute from `vars`, then emit commands. Put domain mechanics into small functions. Prefer deterministic commands unless variability is part of the workflow. Use runtime visuals for shadow/advisory modes before enabling destructive actions. Avoid loops whose termination depends on wall-clock time.
