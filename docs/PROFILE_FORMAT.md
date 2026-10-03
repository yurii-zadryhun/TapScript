# Profile model

Profiles are serialized as readable JSON in the app-private files directory.

Important concepts:

- rectangles and gesture points are normalized to `0..1`;
- OCR is configured per region;
- extractors write named runtime variables;
- rules and scripts produce named commands;
- command targets are resolved to pixels only at execution time.

## Text region

Conceptual JSON:

```json
{
  "id": "candidate_stats",
  "name": "Candidate stats",
  "bounds": { "left": 0.34, "top": 0.70, "right": 0.88, "bottom": 0.83 },
  "recognizer": "TEXT",
  "textConfig": {
    "extractors": [
      {
        "variable": "candidate.attackSpeed",
        "pattern": "\\+(?<value>\\d+(?:\\.\\d+)?)% Attack Speed",
        "matchMode": "FIRST",
        "fields": [
          { "name": "value", "group": "value", "type": "NUMBER" }
        ]
      }
    ]
  }
}
```

## Action target

```json
{
  "id": "equip",
  "name": "Equip",
  "kind": "TAP",
  "start": { "x": 0.67, "y": 0.87 }
}
```

## Coordinate rules

Profile coordinates must be within `0..1` and are validated by the domain models. Pixel conversion is clamped to the current interaction bounds and uses the dimensions of the captured content, not hard-coded physical display dimensions. This keeps profiles portable across display resolutions and orientation changes.
