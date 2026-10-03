# Security and Android platform constraints

## Local-first behavior

TapScript does not require a backend. OCR and decision logic run on-device. Profiles are stored in the app-private files directory.

## Script boundary

JavaScript receives serialized variables and four command functions. It is not handed Android objects or a general Java bridge. The Rhino context uses safe standard objects and an execution deadline.

Profiles are still trusted-owner content, not hostile third-party plugins. Do not treat the scripting layer as a formal security sandbox for untrusted scripts.

## Accessibility

Gesture dispatch uses Android's `AccessibilityService.dispatchGesture`. Android documents accessibility services for assisting users with disabilities, so broad Play Store distribution of a general automation tool has policy implications even though the API works technically.

## MediaProjection

Screen capture always starts through Android's user consent UI and runs in a foreground service with the `mediaProjection` service type. TapScript does not try to retain or replay a projection token across process restarts.

## Package visibility

TapScript does **not** request `QUERY_ALL_PACKAGES`. The optional “Launch app” action sends a launcher intent scoped to the package name already stored in the profile. Android allows starting another app's activity without broad package inventory visibility, so TapScript avoids requesting access to the user's installed-app list.

## Anti-cheat / integrity

A signed APK is not the same thing as a Play-installed APK. TapScript does not spoof install source, Play Integrity, accessibility state, overlays, or automation detection.
