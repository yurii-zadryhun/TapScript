# Release signing

A Google Play developer account is **not** required to create a valid signed APK for sideloading.

## Create a key

Android Studio:

```text
Build → Generate Signed App Bundle / APK → APK → Create new
```

or Ubuntu CLI:

```bash
keytool -genkeypair \
  -keystore "$HOME/.android/tapscript-release.jks" \
  -alias tapscript \
  -keyalg RSA -keysize 4096 -validity 10000
```

Back this file up. Updating an installed app requires signing future builds with the same key.

## Configure the project

Copy:

```bash
cp signing.properties.example signing.properties
```

Fill the four values. `signing.properties`, `*.jks`, and `*.keystore` are ignored by git.

Build:

```bash
./gradlew :app:assembleRelease
```

When `signing.properties` exists, `app/build.gradle.kts` applies it automatically to the release build.

## Verify

With Android SDK Build Tools installed:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

## Important distinction

A correctly signed sideloaded APK is a normal Android APK, but signing alone does not make it Play-installed or bypass Play Integrity / app-specific anti-automation checks. TapScript does not attempt to spoof those properties.
