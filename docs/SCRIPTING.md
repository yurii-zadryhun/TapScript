# Scripting API

TapScript embeds Rhino JavaScript behind a small command API.

## Variables

Recognition variables are exposed as the global `vars` object. Dot-separated extractor names are expanded into nested objects.

If extractors write:

```text
candidate.attackSpeed = 18.32
candidate.doubleHit   = 31.68
equipped.attackSpeed  = 24.10
```

JavaScript sees:

```javascript
vars.candidate.attackSpeed
vars.candidate.doubleHit
vars.equipped.attackSpeed
```

Every text region also exposes raw OCR text under:

```javascript
vars.<regionId>.text
```

## Commands

### `tap(actionId)`

Queues a tap on a named action target.

```javascript
tap("equip");
```

### `swipe(actionId)`

Queues a swipe using a named swipe target.

### `waitMs(milliseconds)`

Queues a bounded delay. This is not a busy wait and does not block the UI thread.

### `log(message)`

Writes a line through TapScript’s application logger (Logcat in the current MVP).

## Sandbox philosophy

The script is not given a `Context`, filesystem, network client, AccessibilityService, reflection bridge, or MediaProjection object. It only receives plain serialized data and command functions.

This is not a hardened hostile-code sandbox. Profiles are assumed to be authored by the device owner. The boundary exists to keep game-specific scripts decoupled from Android implementation details and to make behavior testable.

## Example: weighted item score

```javascript
const weights = {
  attackSpeed: 100,
  doubleHit: 100,
  critDamage: 70,
  lifesteal: 15
};

const caps = {
  attackSpeed: 40,
  doubleHit: 40,
  critDamage: 100,
  lifesteal: 20
};

function score(item) {
  if (!item) return 0;
  let total = 0;
  Object.keys(weights).forEach(function (key) {
    const value = Number(item[key] || 0);
    total += (value / caps[key]) * weights[key];
  });
  return total;
}

if (score(vars.candidate) > score(vars.equipped)) {
  tap("equip");
} else {
  tap("sell");
}
waitMs(120);
```

## Recommended script style

Keep scripts pure where possible: compute from `vars`, then emit commands. Put game mechanics into small functions. Avoid loops whose termination depends on wall-clock time.
