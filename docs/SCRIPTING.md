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

## Commands

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

## Sandbox philosophy

The script is not given a `Context`, filesystem, network client, AccessibilityService, reflection bridge, or MediaProjection object. It only receives plain serialized data and command functions.

This is not a hardened hostile-code sandbox. Profiles are assumed to be authored by the device owner. The boundary keeps profile logic decoupled from Android implementation details and makes behavior testable.

## Example

```javascript
const weights = {
  attackSpeed: 100,
  doubleHit: 100,
  critDamage: 70
};

const caps = {
  attackSpeed: 40,
  doubleHit: 40,
  critDamage: 100
};

function score(item) {
  if (!item) return 0;
  let total = 0;
  Object.keys(weights).forEach(function (key) {
    total += (Number(item[key] || 0) / caps[key]) * weights[key];
  });
  return total;
}

if (score(vars.candidate) > score(vars.equipped)) {
  tapRandom("equip", 10);
} else {
  tap("sell");
}
waitRandom(100, 160);
```

## Recommended script style

Keep scripts pure where possible: compute from `vars`, then emit commands. Put domain mechanics into small functions. Prefer deterministic commands unless variability is part of the workflow. Avoid loops whose termination depends on wall-clock time.
