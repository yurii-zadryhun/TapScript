# TapScript project state

## Product

TapScript is a personal Android visual automation tool. A profile defines what to observe on screen, how to extract values, what logic to run, and which named gestures to execute. The intended authoring flow is visual rather than coordinate-first.

Runtime pipeline:

`MediaProjection frame -> ROI change detection -> ROI OCR -> regex extractors -> variables/snapshot -> rules or Rhino JS -> semantic commands -> Accessibility gestures`

## Modules

- `app`: Compose UI, dependency graph, profile/session orchestration, overlay workspace, floating controls, history UI, crash diagnostics.
- `engine-api`: stable models, ports, commands.
- `engine-core`: capture-frame flow, recognition orchestration, decision/rule logic, pause ownership, command execution.
- `platform-android`: MediaProjection capture, Accessibility gestures/foreground-app events, package launching/app discovery.
- `recognition-mlkit`: on-device ML Kit text recognition.
- `scripting-rhino`: constrained JavaScript command API.
- `storage-json`: profile and run-history persistence.

## Current UX/runtime contracts

- Screen capture is requested when a profile starts, not as a permanent setup prerequisite.
- Stopping a profile stops capture.
- A profile can target the whole screen or a specific package. App-scoped profiles pause when the target app is not foreground and resume when it returns.
- Floating controls are part of the TapScript runtime when overlay permission exists. They are not a separately hideable feature. Closing TapScript should close workspace/pickers, stop automation/capture, and remove overlays.
- The floating control is a narrow draggable rail with compact status. Its expanded menu opens the overlay workspace, pause/resume, stop, and close.
- The overlay workspace is the long-term primary runtime UI: inspect variables/OCR/logs and edit profiles without leaving the target app.
- OCR regions, tap points, and swipe paths should be visible and editable on a dimmed overlay over the real target app. Numeric coordinate editing is an advanced fallback.
- Run history is persistent JSON. Active runs checkpoint logs to disk so a crash does not erase the whole session.
- Script `log()` uses the application logger and belongs in live logs + persistent run history.
- Deterministic and randomized scripting commands coexist. Randomized commands are explicit (`tapRandom`, `swipeRandom`, `waitRandom`); deterministic commands remain unchanged.

## Android constraints

- Accessibility is required for gesture dispatch and foreground-package observation.
- MediaProjection consent is a system security dialog and cannot legitimately be removed by the app.
- Current capture permission code on Android 14+ explicitly requests `MediaProjectionConfig.createConfigForDefaultDisplay()`. This is why the system chooser shows single-app sharing as unsupported. Supporting single-app projection requires deliberately changing this flow and validating capture bounds/coordinate mapping.
- Sideloaded Accessibility apps can hit Android Restricted Settings and Play Protect. Do not weaken target SDK or remove required capabilities as a workaround.
- Overlay windows use `TYPE_APPLICATION_OVERLAY`; Compose dialogs/dropdowns that create child Android windows can be unsafe there. Prefer inline overlay UI or explicitly overlay-aware window handling.

## Build/release

- Java 17, compile SDK 36, target SDK 36, min SDK 26.
- CI workflow: `.github/workflows/build.yml`.
- Required green gate: unit tests + Android lint + debug APK + release APK.
- Release signing material is intentionally outside Git. Never regenerate or replace the release key casually because installed-app upgrades depend on certificate continuity.
- Current development version after the randomized scripting change: `0.2.0-alpha3` / versionCode 3.
