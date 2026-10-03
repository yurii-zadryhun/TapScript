# TapScript

TapScript is a local-first Android visual automation toolkit: capture the screen, recognize small regions, extract structured values, evaluate rules or JavaScript, and dispatch taps/swipes.

The project is intentionally built as a set of small, replaceable components. Android platform code, OCR, orchestration, persistence, and scripting live in separate modules so a recognizer or script engine can be swapped without rewriting the app.

> **Status:** `v0.2.0-alpha1` real-device validation build. The core capture/OCR/decision/action pipeline is implemented together with visual authoring, app-scoped automation, persistent run history, and draggable floating runtime controls. The current feature branch is intentionally kept in a draft PR until the overlay and coordinate behavior are validated on a physical device.

## Why this stack

- **Kotlin + native Android APIs** for `MediaProjection`, foreground services, accessibility gestures, lifecycle, and Compose.
- **ML Kit Text Recognition v2** for fast on-device OCR.
- **ROI-first recognition**: OCR only the configured rectangles, not the whole screen.
- **Rhino JavaScript** as an embedded escape hatch for complex logic.
- **No-code rules** for common compare-and-act workflows.
- **Normalized coordinates** (`0.0..1.0`) so profiles survive resolution changes.
- **Manual dependency graph** instead of a DI framework: explicit dependencies, no annotation processing, easy tests.

## Modules

```text
app                 Compose UI + application wiring
engine-api          domain models and ports
engine-core         runner, extraction, rules, command execution
platform-android    MediaProjection + AccessibilityService + package launch
recognition-mlkit   ML Kit OCR implementation
scripting-rhino     JavaScript sandbox/command adapter
storage-json        JSON profile + run-history repositories
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full flow and [docs/RUNTIME_EXPERIENCE.md](docs/RUNTIME_EXPERIENCE.md) for the current device-facing UX.

## Quick start on Ubuntu

Requirements:

- Android Studio current stable
- JDK 17+
- Android SDK / Build Tools matching the project configuration
- a physical Android device is strongly recommended

Clone the repository, open the root folder in Android Studio, let Gradle sync, then run `app`.

CLI:

```bash
./gradlew test :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The custom `gradlew` bootstrap downloads the pinned Gradle distribution on first CLI use. CI performs unit tests, Android lint, debug build, and release build on every PR.

## First-run flow

1. Open **TapScript**.
2. Enable **TapScript Automation** in Android Accessibility settings.
3. Tap **Allow screen capture** and approve Android's MediaProjection dialog.
4. Grant **Display over other apps** if you want floating controls and live on-target picking.
5. Create or open a profile and choose **Whole screen** or a target application.
6. For app-scoped profiles, pick OCR regions, taps, and swipes directly on the target app. Screenshot-based authoring remains available as a fallback.
7. Choose **Rules** or **JavaScript** logic and start the session.

When a profile is scoped to one application, TapScript pauses OCR/actions while another app is in the foreground and automatically resumes when the selected app returns. Those transitions are included in the run log.

A seeded “Loot evaluator demo” profile demonstrates OCR extraction and JavaScript decisions.

## Runtime UX

The optional draggable floating bubble shows automation state without forcing an app switch. Tap it for compact controls such as Pause/Resume, Stop, Open TapScript, or Hide bubble. Its position persists across uses.

Completed runs are stored locally in **Run history** with start/end time, duration, outcome, actions, errors, and detailed log entries. Recent logs are also shown live while a profile runs.

The next major UX step is an overlay workspace/profile editor so most configuration can happen above the target app rather than returning to the TapScript activity.

## Example script

```javascript
const caps = {
  attackSpeed: 40,
  doubleHit: 40,
  critDamage: 100
};

function score(item) {
  if (!item) return 0;
  return (item.attackSpeed || 0) / caps.attackSpeed * 100
       + (item.doubleHit || 0) / caps.doubleHit * 100
       + (item.critDamage || 0) / caps.critDamage * 70;
}

if (score(vars.candidate) > score(vars.equipped)) {
  tap("equip");
} else {
  tap("sell");
}
waitMs(120);
```

Scripts cannot access Android APIs directly. They can only emit the small command set exposed by TapScript (`tap`, `swipe`, `waitMs`, `log`) and read `vars`.

## Validation status

GitHub Actions currently runs:

```text
unit tests
Android lint
assembleDebug
assembleRelease
artifact upload
```

The `v0.2.0-alpha1` feature branch has passed that pipeline. Overlay behavior, foreground-app transitions, MediaProjection lifecycle, and coordinate alignment still require physical-device testing because they depend on Android/vendor runtime behavior.

See [docs/TESTING.md](docs/TESTING.md).

## Signing a release APK

No Google developer account is required for sideloading a properly signed APK.

1. Create or securely retain a release keystore.
2. Copy `signing.properties.example` to `signing.properties` and fill it in.
3. Run:

```bash
./gradlew :app:assembleRelease
```

The release build automatically uses that signing config when the file exists. Never commit the keystore or `signing.properties`.

See [docs/SIGNING.md](docs/SIGNING.md).

## Performance model

TapScript deliberately does **not** OCR every display frame. The runtime uses a conflated latest-frame source, a minimum recognition interval, change detection, and per-region OCR.

See [docs/PERFORMANCE.md](docs/PERFORMANCE.md).

## Platform note

TapScript relies on sensitive Android capabilities such as Accessibility, MediaProjection, and optional overlay windows. Android and Google Play apply additional security and policy controls around such capabilities. The code does not attempt to hide automation, bypass integrity checks, or evade platform protections.
