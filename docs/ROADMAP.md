# Roadmap

## Implemented in the current MVP

- frozen screenshot / gallery image authoring reference;
- drag-to-select normalized OCR regions;
- visual tap target picker;
- visual swipe target picker;
- direct normalized-coordinate editing;
- regex extractor editor;
- no-code scalar rules;
- JavaScript command scripting;
- ROI-aware change detection and conflated frame processing.

## v0.2 — authoring refinement

- OCR preview with bounding boxes;
- “tap recognized text to create extractor” flow;
- package picker for installed launchable apps;
- duplicate/profile import/export actions;
- inline validation for duplicate ids and unresolved action references.

## v0.3 — runtime UX

- tiny floating control overlay;
- freeze-current-game-frame shortcut;
- pause/resume without destroying the configured session;
- live variable inspector;
- per-region confidence and latency display;
- session history kept only in memory unless exported.

## v0.4 — recognizers

- template matcher;
- color/brightness matcher;
- image hash / icon recognizer;
- optional custom TFLite recognizer.

## v0.5 — automation language

- richer visual rule groups (AND/OR);
- formulas in the no-code builder;
- reusable profile functions;
- profile import/export with explicit schema migration.

## Performance follow-up

- bitmap pool / frame leases if profiler proves allocation pressure;
- per-region recognition scheduling;
- OCR dependency graph (only recognize variables the current logic needs);
- optional region-specific OCR cadence.
