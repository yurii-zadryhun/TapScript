# Example: Dungeon Rush loot advisor

The seeded `Dungeon Rush loot evaluator` profile is based on the motivating use case: two item cards are visible, the candidate and currently equipped items have percentage stats, and TapScript should explain whether the candidate looks better.

The built-in profile currently runs in **shadow mode**. It never taps Sell or Equip. It highlights the recommended item/action and shows a score breakdown so the policy can be validated on real loot before destructive automation is enabled.

## Recognition

Two text regions are defined:

```text
equipped
candidate
```

Each region uses an `ALL` regex extractor for percentage stats:

```regex
(?<amount>[+-]?\d+(?:[.,]\d+)?)%\s+(?<name>[^\n]+)
```

The runtime turns OCR such as:

```text
+18.32% Attack Speed
+31.68% Double Hit Chance
+29.48% Triple Hit Chance
```

into data shaped like:

```javascript
vars.candidate.stats = [
  { value: 18.32, name: "Attack Speed" },
  { value: 31.68, name: "Double Hit Chance" },
  { value: 29.48, name: "Triple Hit Chance" }
];
```

Item level is extracted separately. Raw OCR text remains available as `vars.candidate.text` and `vars.equipped.text`.

## Current calibrated scorer

The stateless baseline was calibrated through blind item comparisons rather than chosen only by intuition:

```text
Excellent  180
Great      100
Good        55
OK          12
Bad          2
Level       30 max at level 130
```

Each known affix contributes:

```text
min(1, usefulRoll / perItemRollCap) × categoryWeight
```

`Critical Damage Taken` is shown as a negative percentage in the game, so its magnitude is treated as the beneficial roll.

Level is deliberately tie-breaker-scale. It is used only when both item levels parse.

## Shadow-mode UX

For a valid comparison the script emits only advisory visual/log commands:

- highlight the winning item region;
- highlight `equip` or `sell`;
- show candidate/equipped total, skill score, level score, delta, decision margin, and top contributions;
- log the same decision for later review;
- send **no gesture command**.

Stable visual keys update in place rather than stacking every time OCR runs.

The region outline is drawn outside the OCR rectangle so the overlay does not cover recognized text or create an OCR/change-detection feedback loop.

## Fail-closed checks

A normal non-melee item is expected to expose 4–5 percentage affixes (3 normal plus 1–2 exceptional). If either item parses outside that range, or a parsed stat name is unknown, the script shows `CHECK OCR` and makes no Equip/Sell recommendation.

Melee candidates are a special hard reject and are detected from the game title marker:

```text
[Melee]
```

The scorer also tolerates `[Meele]` so a common letter transposition/OCR error does not accidentally allow a melee candidate.

This check runs before the normal 4–5-stat validation because a melee card can be rejected safely from its title marker alone.

## Coordinates

The seeded normalized rectangles and Sell/Equip targets are approximate example values. Open the profile and tune them for the exact game layout/device. Because they are normalized, moving between display resolutions does not require rewriting pixel coordinates.

## Context-aware scoring

The current built-in advisor is intentionally stateless because the visible popup contains only the candidate and the currently equipped item for that slot.

A later loadout-aware scorer should use the resulting whole build, including:

- real combat cap near 100% total Triple Hit;
- real combat cap near 100% total Critical Damage Taken;
- Double Hit losing value as resulting Triple Hit approaches 100%;
- replacement-aware cap logic rather than blindly adding a candidate on top of the item it replaces;
- optional PvP/PvE context and meta applicability.

Until the app can observe or maintain trustworthy equipment state, the calibrated stateless policy is the safer fallback.
