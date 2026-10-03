# Validation checklist

## CI gate

A development change is not build-verified until the branch workflow passes unit tests, Android lint, debug build, and release build. Do not describe an in-progress/cancelled workflow as green.

Local equivalent:

```bash
./gradlew test lintDebug :app:assembleDebug :app:assembleRelease
```

## Physical-device gate

CI cannot validate MediaProjection, Accessibility reliability, overlay windows, package foreground events, or coordinate alignment. For runtime changes test on the Galaxy S24 Ultra:

1. Cold-start TapScript and confirm Accessibility + overlay state.
2. Start an app-scoped profile; grant capture; verify target app launches.
3. Confirm OCR/variables/actions and live logs update.
4. Pause/resume manually, switch away from the target, return, and verify pause ownership.
5. Open the floating workspace over the target app and exercise visual geometry picking/editing.
6. Stop the run and confirm capture notification/service disappears and history remains readable.
7. Repeat capture start/stop several times and rotate/resize where relevant.
8. If any crash occurs, collect TapScript's persisted crash diagnostic before changing code.

## Release discipline

- Never commit signing material.
- Preserve the existing release certificate for upgrade compatibility.
- A debug APK (`dev.tapscript.app.debug`) is a separate app from the signed release package (`dev.tapscript.app`).
- When giving the user an APK, state whether it is debug or signed release and provide its SHA-256.
