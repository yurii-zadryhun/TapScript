# Development on Ubuntu

## Toolchain

- Android Studio current stable
- JDK 17+ (Android Studio's bundled JDK is fine)
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0+
- `adb`

AGP is pinned to 9.4.0 and the CLI bootstrap uses Gradle 9.6.0.

## Useful commands

```bash
./gradlew test
./gradlew :app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.tapscript.app/.MainActivity
```

## Device debugging

MediaProjection and accessibility gestures are best tested on a physical device. The emulator is useful for UI/editor work, but it does not reproduce every vendor-specific background-process or overlay behavior.

## Adding a recognizer

Implement the port in `engine-api`:

```kotlin
class MyRecognizer : TextRecognizer { ... }
```

Wire it in `AppGraph`. The runner and editor do not need to know which OCR engine is active.

## Adding a command

1. add the command model to `engine-api`;
2. emit it from rule/script adapters;
3. implement execution in `CommandExecutor`;
4. add unit tests;
5. document the scripting function if exposed.

This keeps scripting syntax from directly invoking platform code.
