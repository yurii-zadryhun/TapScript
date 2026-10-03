# Testing strategy

## Unit tests

The project includes tests for the two pieces most likely to silently make wrong decisions:

- regex extraction;
- rule evaluation;
- JavaScript variable mapping and command emission.

Run:

```bash
./gradlew test
```

## Device tests to add next

1. Feed a fixed screenshot into the ML Kit adapter and assert parsed variables.
2. Verify normalized tap targets at 720p, 1080p, and 1440p.
3. Rotate the device during capture and verify capture resize behavior.
4. Cancel MediaProjection from the system UI and verify clean session shutdown.
5. Disable Accessibility mid-session and verify a visible runtime error instead of a crash.

## Golden screenshots

For each game profile, keep a tiny private fixture set of representative screenshots on your development machine. Benchmark the exact ROIs rather than whole-screen OCR. Avoid committing screenshots containing account names or other personal data to a public repository.

## Generated-snapshot validation

Before the archive was produced, the generated source tree was checked for Kotlin/Kotlin-script parser errors, XML well-formedness, TOML parsing, shell-script syntax, and broken relative Markdown links. The generation environment could not perform a real Gradle dependency resolution because outbound DNS was unavailable.

On the development machine, the first definitive check should therefore be:

```bash
./gradlew test :app:assembleDebug
```
