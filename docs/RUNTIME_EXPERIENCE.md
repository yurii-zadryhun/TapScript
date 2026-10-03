# Runtime experience

TapScript's runtime UX is built around staying over the target app instead of repeatedly switching back to the main Activity.

## Floating controls

After `Display over other apps` permission is granted, TapScript shows a compact draggable rail with a status indicator. Tapping it opens quick controls for Workspace, Pause/Resume, Stop, and `× Close TapScript`. There is no separate Hide Bubble mode: closing TapScript is the operation that removes its runtime UI and services.

## Overlay workspace

The on-top workspace provides Live, Profile, and Logs areas while the target app remains visible underneath. It can inspect runtime metrics, variables, OCR observations, freeze a capture frame, edit profile settings, and launch visual pickers.

Overlay-hostile child windows should be avoided; new editing UI should prefer inline overlay-safe surfaces.

## Profile scope

A profile can target either the whole screen or a specific package. App-scoped profiles use Accessibility foreground-package events. When the selected app is not active, recognition/actions pause; returning to it resumes processing. Manual and system/UI pause reasons are independent tokens.

## Capture lifecycle

Capture is requested as part of Start Profile. Stopping the profile stops capture. The MediaProjection consent UI is Android-owned and remains mandatory.

Android 14+ currently requests default-display capture explicitly, so the system's single-app sharing choice is disabled. Optional single-app projection is planned and must preserve correct capture/interaction coordinate mapping.

## Visual authoring

OCR rectangles, tap targets, and swipe paths can be picked directly on a dimmed overlay above the real target app. Existing configured geometry is drawn for context. Screenshot-based authoring remains a fallback.

The remaining important UX step is true edit-in-place for existing geometry with selection handles/redraw while preserving the item's id.

## Logs and history

Runs persist start/end/outcome/logs as local JSON. Active logs are checkpointed to disk during execution. Script `log()` messages, actions, target-app pause transitions, and runtime errors use the same logging path. Long-run viewer ergonomics (filter/search/export/follow) are still planned.

## Script variability

Both deterministic and explicit randomized commands are supported. `tapRandom`, `swipeRandom`, and `waitRandom` add bounded execution-time variability while leaving the underlying named targets unchanged. See `docs/SCRIPTING.md`.
