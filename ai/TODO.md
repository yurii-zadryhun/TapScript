# Remaining work

Canonical current-state backlog. Keep this file short and actionable. Every completed development action must update this file; rewrite the current state instead of adding a chronological log.

**Current checkpoint:** `0.2.0-alpha3` adds explicit randomized script waits/positions with tests and keeps deterministic commands intact. Runtime-experience branch CI must be green before this checkpoint is considered build-verified.

## P0 — physical-device stability and correctness

- Re-test MediaProjection start repeatedly on the Galaxy S24 Ultra. A previous build sometimes crashed immediately after capture consent and Accessibility later showed `Not working`. Use the persisted crash diagnostics to identify the real exception; do not guess at the root cause.
- Verify Accessibility survives repeated profile start/stop/capture cycles. If Android kills/disconnects the service, surface that state clearly and stop issuing commands.
- Verify Dungeon Rush launch through `PackageManager.getLaunchIntentForPackage()` and confirm foreground-package detection sees `com.lavalabs.dungeonrush` while it is active.
- Verify manual pause/resume from the floating rail and target-app auto-pause/auto-resume. Pause owners must remain independent.
- Verify script `log()` appears immediately in the live viewer, is checkpointed during the run, and remains in history after Stop or a recoverable crash.
- Verify Stop and `× Close TapScript` both terminate automation, capture, picker/workspace windows, and floating controls without leaving services behind.

## P1 — capture model

- Support Android single-app MediaProjection as an optional capture scope. Current Android 14+ permission intent forces `createConfigForDefaultDisplay()`, which disables the system `Share one app` option. When changing it, handle app-capture resize/bounds and coordinate mapping correctly.
- Keep whole-screen capture available. Never attempt to bypass the mandatory Android MediaProjection consent dialog.
- Improve capture/error diagnostics in the UI: distinguish permission cancelled, foreground-service failure, projection stopped, frame conversion failure, and timeout.

## P1 — overlay editor and visual authoring

- Complete edit-in-place for existing OCR regions/taps/swipes: `Edit -> dim target screen -> show all configured geometry -> drag/redraw selected item -> return with same id`.
- Add selection handles/labels and clearer colors for regions vs taps vs swipes; ensure geometry remains correct across orientation/resolution changes.
- Finish replacing overlay-hostile Compose `AlertDialog`/`DropdownMenu` flows with inline overlay-safe editors/sheets where needed.
- Polish the overlay workspace into the primary runtime UI: compact icon-first controls, collapsible status/details, less text, better hierarchy on a phone-sized screen.
- Verify target app icons render in every app selector and use a graceful fallback when a drawable cannot be loaded.

## P1 — logs and run history

- Make live logs comfortable for long runs: autoscroll toggle, pause-follow, level filter, search, copy/share/export, and clear separation between runtime/script/action/error events.
- Reconcile interrupted persisted runs on next launch so stale `RUNNING` records become an explicit interrupted/crashed outcome when appropriate.
- Add retention controls by count/disk size; never allow history JSON to grow without bounds.
- Let the overlay workspace open previous runs without leaving the target app.

## P1 — scripting/runtime quality

- Add concise in-app scripting reference/autocomplete or snippets for `tap`, `tapRandom`, `swipe`, `swipeRandom`, `waitMs`, `waitRandom`, `log`, and `vars`.
- Add script validation before Save/Run with line/column errors.
- Consider named reusable script helpers/modules without exposing Android, filesystem, network, or reflection APIs.
- Add execution tests for randomized gestures at screen edges and randomized swipe duration, in addition to command/randomizer unit tests.

## P2 — recognition and authoring capability

- Add recognizer types beyond OCR behind the existing ports: template/similarity matching, color/brightness checks, and optional TFLite model inference.
- Add live OCR preview while defining a region, including extracted variables and regex validation.
- Add profile import/export with schema migration and validation.

## P2 — distribution and hardening

- Set up Google Play Console Internal testing for private installation/update delivery when desired; do not publish publicly yet.
- Review Accessibility/overlay declarations and Play policy requirements before any Play-distributed build.
- Add a reliable signed-release CI path using protected secrets only when the repository setup is ready. Never store the keystore or passwords in Git.
- Add performance/battery profiling for long capture sessions and memory/leak checks around repeated overlays, ImageReader resize, and ML Kit OCR.
