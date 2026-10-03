# TapScript

TapScript is a local-first Android visual automation toolkit: capture the screen, recognize small regions, extract structured values, evaluate rules or JavaScript, and dispatch taps/swipes.

The project is intentionally built as a set of small, replaceable components. Android platform code, OCR, orchestration, persistence, and scripting live in separate modules so a recognizer or script engine can be swapped without rewriting the app.

> **Status:** working architecture-first MVP. The capture pipeline, ML Kit OCR adapter, regex extraction, rule engine, JavaScript command engine, Android gesture dispatcher, JSON profile storage, Compose profile editor, and screenshot-based visual region/tap/swipe picker are implemented. A compact floating runtime overlay is intentionally left as a documented next-step feature rather than hidden inside a giant unfinished class.

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
storage-json        JSON profile repository
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full flow.

## Quick start on Ubuntu

Requirements:

- Android Studio current stable
- JDK 17+ (AGP 9.4 uses JDK 17)
- Android SDK 37 / Build Tools 36+
- a physical Android device is strongly recommended

Clone/unzip, open the root folder in Android Studio, let Gradle sync, then run `app`.

CLI:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The custom `gradlew` bootstrap downloads Gradle 9.6.0 on first CLI use. A binary wrapper JAR is deliberately not stored in this generated archive.

## First-run flow

1. Open **TapScript**.
2. Open Android Accessibility settings and enable **TapScript Automation**.
3. Tap **Allow screen capture** and approve Android's projection dialog.
4. Create or open a profile.
5. Import a reference screenshot and draw text regions / place tap and swipe targets visually, or edit normalized coordinates directly.
6. Choose **Rules** or **JavaScript** logic.
7. Start the session, then launch the target game/app.

A seeded “Loot evaluator demo” profile demonstrates OCR extraction and JavaScript decisions.

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

Scripts cannot access Android APIs directly. They can only emit the small command set exposed by TapScript (`tap`, `swipe`, `waitMs`, `log`) and read `vars`. This keeps the runtime deterministic and testable.

## Validation status

This generated snapshot has been statically checked for Kotlin/Kotlin-script syntax, XML well-formedness, TOML parsing, and shell-script syntax. A full Gradle compile was not possible in the generation environment because it had no outbound DNS access to download Gradle/Maven dependencies. Run `./gradlew test :app:assembleDebug` after first sync on your Ubuntu machine for the definitive build check.

## Signing a release APK

No Google developer account is required for sideloading a properly signed APK.

1. Create a keystore in Android Studio, or with `keytool`.
2. Copy `signing.properties.example` to `signing.properties` and fill it in.
3. Run:

```bash
./gradlew :app:assembleRelease
```

The release build automatically uses that signing config when the file exists. Never commit the keystore or `signing.properties`.

See [docs/SIGNING.md](docs/SIGNING.md).

## Performance model

TapScript deliberately does **not** OCR every display frame. The runtime uses a conflated latest-frame source, a minimum recognition interval, change detection, and per-region OCR. The default profile interval is 120 ms and can be tuned.

See [docs/PERFORMANCE.md](docs/PERFORMANCE.md).

## Important platform note

Android accessibility APIs are intended by Android for accessibility use cases and have additional policy implications for Play distribution. A private sideloaded tool is technically simpler than publishing a general-purpose game automation utility to Google Play. The code does not attempt to hide automation, bypass app integrity checks, or evade detection.
