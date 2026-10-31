# TapScript Android

The standalone Gradle project lives in this directory. It currently contains only a Kotlin/Compose launcher with the existing application ID and branding.

```bash
./gradlew :app:assembleDebug
./gradlew test :app:lintDebug
```

The former automation runtime and editor modules were removed in the platform reset. Their implementations remain available in Git history for reference. New runtime modules will be introduced only after requirements and ADRs are accepted.
