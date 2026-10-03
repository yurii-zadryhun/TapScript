# Runtime experience

This document describes the `v0.2.0-alpha1` real-device UX pass.

## Floating controls

TapScript can show a draggable bubble above other apps after the user grants Android's **Display over other apps** permission.

The bubble is deliberately compact. Its status dot reflects the current automation state and its position is persisted. Tapping it opens quick controls for opening TapScript, pausing/resuming the active profile, stopping the profile, or hiding the bubble.

The bubble is not meant to become a second full application UI. The next iteration will use a dedicated overlay workspace for editing and inspection while keeping the target app visible underneath.

## Profile scope

A profile can run against either:

- **Whole screen** — TapScript does not restrict automation based on the foreground package.
- **Specific app** — the user selects a launchable app by name. TapScript observes foreground-package changes from its accessibility service. When another app becomes active, the profile is paused and a log entry is written. Returning to the selected app automatically resumes the profile.

This gate is intentionally evaluated before OCR and decision execution so no tap/swipe command is emitted while the wrong app is active.

## Live visual authoring

For app-scoped profiles, the editor can open the target app and temporarily place a transparent authoring overlay above it.

Supported pick modes:

- tap point;
- swipe path;
- OCR rectangle.

Coordinates are converted to normalized `0..1` geometry and stored in the profile. The user normally sees the visual picker rather than raw coordinates; numeric editing remains available under the advanced controls.

A screenshot-based picker remains available as a fallback, especially for whole-screen profiles.

## Persistent run history

Each completed run is stored locally as JSON under the application's private files directory. A record contains profile identity, start/end time, duration, final outcome, and the captured automation log entries.

The history viewer allows recent runs to be reopened after the automation stops. Tap/swipe execution, runtime errors, target-app pause/resume transitions, and script log messages flow through the same logger.

The current retention strategy caps the in-memory log list per run but does not yet enforce an age or disk-size policy. Export, filtering, and retention settings are follow-up work.

## Pause ownership

Runtime pause requests are tokenized. Manual pause and future overlay-workspace pause are independent owners. Releasing one token cannot accidentally resume a session still paused by another owner.

This is important for the upcoming on-top editor: opening or closing a workspace must never override an explicit user pause.

## Current device-validation targets

Before merging this work into `master`, validate on a physical device:

1. Grant overlay permission and enable the bubble.
2. Drag it around the screen and reopen its menu.
3. Select a target app and pick a tap, swipe, and OCR rectangle directly on that app.
4. Run the profile, switch to another app, and confirm `PAUSED` plus a history log entry.
5. Return to the target and confirm automatic resume.
6. Stop the run and reopen its complete log from Run history.
7. Rotate the target app and check visual picker/capture coordinate alignment.
