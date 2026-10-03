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

## Current runtime-experience branch

- choose **Whole screen** or a launchable target application instead of typing a package name;
- application-scoped profiles pause when the target app leaves the foreground and resume when it returns;
- pause/resume transitions are written into automation logs;
- persistent run history with start time, duration, outcome, actions, errors, and log viewer;
- live recent logs remain visible while a profile is running;
- draggable floating bubble with compact status indicator and quick menu;
- floating-controls toggle and persisted bubble position.

## Next — overlay-first authoring

- full-screen transparent picker over the target app for tap points and swipe paths;
- region rectangle picker over the live target app;
- freeze-current-game-frame shortcut so OCR regions can be edited without importing a screenshot;
- compact profile editor rendered as an overlay sheet, so authoring does not require switching back to TapScript;
- configurable bubble side/size/opacity and quick-menu actions;
- pause/resume button in the floating menu without destroying the session;
- live variable inspector in the overlay;
- OCR preview with bounding boxes;
- “tap recognized text to create extractor” flow.

## Authoring and profile management

- duplicate profile action;
- profile import/export;
- inline validation for duplicate ids and unresolved action references;
- richer app picker with app icons, recent targets, and search ranking.

## Runtime observability

- log level and text filters;
- export/share one run or a selected log range;
- optional retention limits by age and disk size;
- per-region confidence and latency display;
- screenshots attached to explicitly requested debug events only;
- run summary with action counts and recognition error counts.

## Recognizers

- template matcher;
- color/brightness matcher;
- image hash / icon recognizer;
- optional custom TFLite recognizer.

## Automation language

- richer visual rule groups (AND/OR);
- formulas in the no-code builder;
- reusable profile functions;
- profile import/export with explicit schema migration.

## Performance follow-up

- bitmap pool / frame leases if profiler proves allocation pressure;
- per-region recognition scheduling;
- OCR dependency graph (only recognize variables the current logic needs);
- optional region-specific OCR cadence.
