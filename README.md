# TapScript

**Status: platform redesign / foundation in progress.**

TapScript is a local-first Android automation platform with **TypeScript projects**, on-device execution, visual screen targets, a web IDE, and a backend for project synchronization and community publishing.

> The present branch contains a **minimal Android shell**. The new script runtime, web editor, API backend, Google login and community features are **not implemented**. These are intentionally architecture placeholders, not shipped functionality.

## Repository layout

| Path | Purpose | State |
|---|---|---|
| `apps/mobile/` | Native Kotlin/Jetpack Compose | Minimal Android application |
| `apps/web/` | Next.js frontend | Planned |
| `apps/api/` | NestJS modular API | Planned |
| `packages/contracts/` | API schemas/generated types | Planned |
| `packages/script-sdk/` | Public TypeScript automation API | Planned |
| `packages/script-compiler/` | Compilation and module resolver | Planned |
| `infra/` | Infrastructure and deployment | Planned |
| `docs/` | Requirements/architecture/ADRs | Active drafts |

## Product and engineering documentation

- [PRD](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Product questionnaire](docs/QUESTIONNAIRE.md)
- [Migration plan](docs/plans/REPO_MIGRATION_PLAN.md)
- [Architecture decisions](docs/adr/README.md)
- [Android signing](docs/mobile/SIGNING.md)

## Android development

Open `apps/mobile` directly in Android Studio. No Node toolchain is required for Android.

```bash
cd apps/mobile
./gradlew :app:assembleDebug
./gradlew test :app:lintDebug
```

Android CI builds debug and unsigned release APKs. Automatic signed GitHub Releases and Play deployment are planned but **not yet set up**.

The old profile automation, Rhino engine, ML Kit adapter and Android automation services were removed from the new tree but remain retrievable from Git history. This bare shell requests no Accessibility or screen capture permissions.
