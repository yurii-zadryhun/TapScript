# Example: loot comparison profile

The seeded `Loot evaluator demo` profile is based on the motivating use case: two item cards are visible, the candidate and currently equipped items have percentage stats, and the automation must choose **Sell** or **Equip**.

## Recognition

Two text regions are defined:

```text
equipped
candidate
```

Each region uses an `ALL` regex extractor:

```regex
\+(?<amount>\d+(?:[.,]\d+)?)%\s+(?<name>[^\n]+)
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

## Decision

The example JavaScript keeps caps and weights in plain objects and computes a normalized weighted score. The script refuses to tap anything until **both** item regions produced at least one parsed stat row.

This guard is intentional: OCR failure should default to “do nothing”, not to a destructive click.

## Coordinates

The seeded normalized rectangles and Sell/Equip targets are approximate example values. Open the profile and tune them for the exact game layout/device. Because they are normalized, moving from 1080p to 1440p does not require rewriting pixel coordinates.

## Recommended refinement

For a production personal profile, split recognition into smaller regions if the game UI is stable. For example, one ROI per stat line can reduce OCR ambiguity further and allows simple scalar variables such as `candidate.attackSpeed`.
