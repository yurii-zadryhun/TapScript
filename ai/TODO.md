# Remaining work

Canonical current-state backlog. Keep this file short and actionable. Every completed development action must update this file; rewrite the current state instead of adding a chronological log.

**Current checkpoint:** `feature/loot-shadow-mode` adds keyed runtime advisory visuals plus a calibrated Dungeon Rush shadow-mode loot advisor on top of the CI-verified `0.2.0-alpha4` runtime branch. GitHub CI is green (`tests + lint + debug/release build`); physical-device visual alignment and real OCR behavior still need validation before merging back into `feature/runtime-experience`.

## P0 — validate loot shadow mode

- On the Galaxy S24 Ultra, verify `highlight()` alignment for both OCR regions and Sell/Equip targets while Dungeon Rush is foreground.
- Confirm the information card is readable without blocking the loot popup and that repeated OCR decisions update stable keys rather than stacking overlays.
- Confirm region outlines stay outside OCR rectangles and do not destabilize OCR/change detection through MediaProjection feedback.
- Confirm visuals disappear on manual pause, target-app loss, Stop, script error, and `× Close TapScript`.
- Validate real Dungeon Rush OCR against the shadow advisor: normal non-melee cards should parse 4–5 known percentage stats; incomplete/unknown OCR should show `CHECK OCR`; `[Melee]` candidates should recommend Sell without requiring 4–5 parsed stats.
- Collect real recommendation disagreements before enabling any automatic Sell/Equip tap.

## P0 — physical-device stability and correctness

- Re-test target-app workflow on the Galaxy S24 Ultra. The recorded crash was `WindowManager.BadTokenException` from a Compose `Dialog` opened inside `TYPE_APPLICATION_OVERLAY`; the overlay workspace no longer uses `AlertDialog`, `Dialog`, or `DropdownMenu`. Confirm this fixes the crash and that Accessibility no longer falls into `Not working` as a consequence of the process dying.
- Re-test MediaProjection start repeatedly. If a new capture crash appears, use the persisted crash record and exact stack trace; do not assume it has the same cause as the overlay-dialog crash.
- Verify Accessibility survives repeated profile start/stop/capture cycles. If Android kills/disconnects the service independently of an app crash, surface that state clearly and stop issuing commands.
- Verify Dungeon Rush launch through `PackageManager.getLaunchIntentForPackage()` and confirm foreground-package detection sees `com.lavalabs.dungeonrush` while it is active.
- Verify manual pause/resume from the floating rail and target-app auto-pause/auto-resume. Pause owners must remain independent.
- Verify script `log()` appears immediately in the live viewer, is checkpointed during the run, and remains in history after Stop or a recoverable crash.
- Verify Stop and `× Close TapScript` both terminate automation, capture, picker/workspace windows, floating controls, and runtime advisory visuals without leaving services/windows behind.
- Verify profile export -> uninstall/data wipe -> reinstall -> import restores edited profiles exactly.

## P1 — Dungeon Rush context-aware scoring

- Keep the calibrated stateless fallback: excellent 180, great 100, good 55, ok 12, bad 2; item level contributes at most 30 points and only when both levels parse.
- Add trustworthy equipment/loadout state before using build-context rules. Scoring must use the resulting build, not mutate context merely because the script recommended Equip.
- Model build-level Triple Hit cap near 100% separately from the per-item 30% roll cap.
- Model build-level Critical Damage Taken cap near 100% separately from the per-item 66% roll cap.
- Make Double Hit lose marginal value as resulting Triple Hit approaches 100%.
- Make cap handling replacement-aware: compare candidate/current item against the same remaining-gear baseline; never automatically remove unrelated slots unless a broader rebalance is explicitly represented.
- Add optional PvP/PvE/meta context only after coefficients are calibrated. Mega Crit remains especially strong in PvE; Ranged Defence is currently more broadly applicable than Melee Defence in PvP.
- Before unattended automation, add verified equipment-state reconciliation, post-equip confirmation, fail-closed OCR rules, safety margins, and whole-build monotonicity checks.

## P1 — capture model

- Support Android single-app MediaProjection as an optional capture scope. Current Android 14+ permission intent forces `createConfigForDefaultDisplay()`, which disables the system `Share one app` option. When changing it, handle app-capture resize/bounds and gesture/visual-coordinate mapping correctly.
- Keep whole-screen capture available. Never attempt to bypass the mandatory Android MediaProjection consent dialog.
- Improve capture/error diagnostics in the UI: distinguish permission cancelled, foreground-service failure, projection stopped, frame conversion failure, and timeout.

## P1 — overlay editor and visual authoring

- Device-verify edit-in-place for existing OCR regions/taps/swipes: dim target screen, show configured geometry, highlight selected item, redraw it, and preserve its id/configuration.
- Add selection handles and clearer geometry affordances; verify alignment across orientation/resolution changes.
- Polish the overlay workspace into the primary runtime UI: more compact icon-first controls, collapsible status/details, less text, and better hierarchy on a phone-sized screen.
- Render target app icons in the overlay app selector too; the Activity profile selector already has icons.

## P1 — logs and run history

- Add copy/share/export for live/history logs and clearer separation between runtime/script/action/error events. Search, level filters, and follow-tail already exist in the live viewer.
- Reconcile interrupted persisted runs on next launch so stale `RUNNING` records become an explicit interrupted outcome when there is no crash record.
- Add retention controls by count/disk size; never allow history JSON to grow without bounds.
- Let the overlay workspace open previous runs without leaving the target app.

## P1 — scripting/runtime quality

- Add concise in-app scripting reference/autocomplete or snippets for `tap`, `tapRandom`, `swipe`, `swipeRandom`, `waitMs`, `waitRandom`, `log`, `highlight`, `showInfo`, `clearVisual`, `clearVisuals`, and `vars`.
- Add script validation before Save/Run with line/column errors.
- Consider named reusable script helpers/modules without exposing Android, filesystem, network, or reflection APIs.
- Add execution tests for randomized gestures at screen edges and randomized swipe duration, in addition to command/randomizer unit tests.

## P2 — recognition and authoring capability

- Add recognizer types beyond OCR behind the existing ports: template/similarity matching, color/brightness checks, and optional TFLite model inference.
- Add live OCR preview while defining a region, including extracted variables and regex validation.
- Add explicit profile schema migration when the profile model first changes; current backup import validates schema/version and safely merges profiles by id.

## P2 — distribution and hardening

- Set up Google Play Console Internal testing for private installation/update delivery when desired; do not publish publicly yet.
- Review Accessibility/overlay declarations and Play policy requirements before any Play-distributed build.
- Add a reliable signed-release CI path using protected secrets only when the repository setup is ready. Never store the keystore or passwords in Git.
- Add performance/battery profiling for long capture sessions and memory/leak checks around repeated overlays, ImageReader resize, ML Kit OCR, and runtime visual overlays.
