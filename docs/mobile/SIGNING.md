# Android signing

TapScript is planned for direct GitHub Releases as well as Google Play.

1. Create and back up a release keystore outside Git.
2. Copy `apps/mobile/signing.properties.example` to `apps/mobile/signing.properties`.
3. From `apps/mobile`, run `./gradlew :app:assembleRelease`.
4. Verify using `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk`.

CI currently produces **unsigned** release APKs, not installable signed public releases. Signing/publishing workflow and Google Play configuration still need an ADR.
