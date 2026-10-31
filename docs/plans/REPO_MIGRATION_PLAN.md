# TapScript — Repository Reset & Migration Plan

**Status:** Proposed change plan, no changes applied to GitHub.  
**Examined branch:** `master` in `yurii-zadryhun/TapScript`, as of 2026-10-31.

## 1. Current audit

The current repository is an Android-only Gradle multi-module project with these root modules:

```text
app/
engine-api/
engine-core/
platform-android/
recognition-mlkit/
scripting-rhino/
storage-json/
```

Supporting files:

```text
.github/workflows/build.yml
.editorconfig
.gitignore
Makefile
README.md
docs/ (legacy design, scripting, UX, testing, signing, etc.)
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/ (versions catalog and wrapper properties)
gradlew, gradlew.bat
signing.properties.example
```

The existing `app` UI is coupled to the old profile/automation engine. The existing CI runs tests, Android lint, debug/release APK builds and artifact upload with hard-coded `app/` paths and `v0.1.0` artifact names. The GitHub workflow exists; **CI green status was not independently verified** and no build was executed in this planning step.

Notable code that is useful as a *reference* for later, but is not needed in a bare shell:

- `platform-android/*` — Accessibility gestures, MediaProjection services, package launching.
- `recognition-mlkit/*` — on-device OCR adapter.
- `engine-core/*` — frame conflation and ROI recognition concepts.
- `app/.../VisualCoordinatePickerDialog.kt` — current region/point picker UX.
- `scripting-rhino/*` — command-only scripting prototype; **not** sufficient sandbox for public scripts.

Git history preserves these implementations; there is no need to keep dead code in the new mainline merely as a reference.

## 2. Safe migration workflow

1. Record current `master` commit SHA, protect/tag baseline (e.g. `pre-platform-reset-2026-10-31`) and create a dedicated `refactor/platform-foundation` branch.
2. Agree on `PRD.md`, ADR-0001 (workspace) and minimal Android shell definition.
3. Use `git mv` to relocate Gradle root/build scripts/resources/launcher to `apps/mobile/` so history follows existing files.
4. Remove obsolete modules and all old profile-driven screen logic, editor screens, demo profiles and old documentation. Keep only resources actively referenced by the bare app.
5. Rewrite `apps/mobile/settings.gradle.kts` to include only `:app`; simplify Android dependencies and manifest to actual minimal app needs.
6. Keep package name/application ID and launcher icon initially (avoids unnecessary install/upgrade changes); keep signing config example, wrapper, `.editorconfig` and license if present.
7. Verify Android standalone Gradle sync, tests/lint and debug build on CI after relocation. **Do not** replace a working release/signing scheme with untested scripts.
8. Add minimal `apps/web` Next.js app and `apps/api` NestJS app, local dev config and `pnpm-workspace.yaml`; they should build independently and respond with a simple route/health endpoint.
9. Add only useful shared packages at this stage (contracts/sdk placeholders with clear purpose), docs/ADRs, root build scripts and independent CI jobs. Do not fabricate engine modules.
10. Open draft PR with exact `git diff --stat`, rationale and CI verification; merge only after a clean validation and architecture review.

## 3. Exact keep/delete guidance for the reset PR

### Keep and move

- `.github/workflows` (adapt to new paths/CI jobs).
- `.editorconfig`, `.gitignore` (extend for Node, Gradle, Android/Next build outputs and credentials).
- `gradle/`, `gradlew`, `gradlew.bat`, `gradle.properties`, root Android `build.gradle.kts`, `settings.gradle.kts` → `apps/mobile/`.
- `app/build.gradle.kts`, proguard rules, launcher manifest/activity, main application class if still needed, minimum Compose screen, theme, launcher icons and referenced resources → `apps/mobile/app/`.
- `signing.properties.example` and Android signing docs → `apps/mobile/` (confirm signing path resolution after move).
- Root `README.md` **rewritten** to platform vision and development instructions.

### Remove from new mainline

- `engine-api/`, `engine-core/`, `platform-android/`, `recognition-mlkit/`, `scripting-rhino/`, `storage-json/` legacy Gradle modules after `:app` has no references.
- Old `app/...` profile editor, dashboard, visual target dialogs, sample profiles, session orchestration and old DI graph.
- Legacy `docs/*` whose descriptions no longer match the reset architecture (replace with PRD, ADRs and new development/release docs).
- Dead Gradle dependencies, ProGuard rules, permissions, resources and code paths created only for the old runtime.
- Old hard-coded CI version labels/artifact source paths and Makefile commands that no longer work.

### Do not delete blindly

- Android launcher activity, theme, icons and package name.
- Signing credentials **configuration example** (never commit actual secrets).
- CI checks and artifacts until equivalent new jobs are proven.
- License, security files or other repository-wide governance metadata if present in future branches.
- Git history or baseline branch/tag.

## 4. Minimal baseline acceptance criteria

- `apps/mobile` opens directly in Android Studio, without pnpm, with one Compose screen containing app identity and no placeholder automation logic.
- `apps/mobile/gradlew :app:assembleDebug :app:lintDebug` succeeds in CI (plus smoke launch if emulator job available).
- `apps/web` builds and renders a minimal landing/auth-placeholder page that does not falsely imply completed Google login.
- `apps/api` starts and returns `/health` without requiring unfinished cloud integrations.
- Root installs JS workspace lockfile reproducibly; root docs explain commands.
- CI jobs are separated by application and report clear failures; Android artifact is downloadable.
- All deleted code remains retrievable from Git history / baseline tag.
- No unrelated feature implementation, no dead modules, and no claim that Google OIDC/runtime/marketplace is implemented.

## 5. Suggested PR sequence

- **PR-01: `docs: define product vision and architecture decisions`** — PRD, architecture, ADR templates and decisions list; no runtime changes.
- **PR-02: `refactor(android): reset to minimal app shell`** — move Gradle root under `apps/mobile`, remove legacy modules, update Android CI; no web/backend yet.
- **PR-03: `chore: bootstrap web, api and workspace tooling`** — Next.js/NestJS/pnpm, healthchecks and CI jobs.
- **PR-04: `feat: define versioned script SDK and runtime contract`** — types, fixtures, validation and protocol; compile/runtime spike separately if uncertain.

Keep the old implementation accessible with a tag, but do not maintain two parallel engines in new mainline.

## 6. Migration risks

| Risk | Mitigation |
|---|---|
| Gradle wrapper bootstrap differs from standard wrapper | Preserve tested wrapper script initially, verify CI with relocated working directory; consider normal wrapper JAR only after proving behavior |
| Signing path changes on `git mv` | Fix project-relative `signing.properties`, keystore references and release build task |
| Hard-coded workflow paths and artifact names | Change tasks, `working-directory`, output paths and upload names together |
| Source package breakage from deleting modules | Replace old activity UI, app graph and manifest refs before removing dependencies |
| Android permissions leftover after reset | Minimal manifest; no Accessibility/MediaProjection service or permissions until they are reimplemented |
| No reproducible baseline | Preserve commit SHA/tag and require CI proof of build before merge |
| Old docs wrongly describe new state | Replace or remove outdated files in same PR |
| Pull request too large to review | Separate documentation, Android reset and TS app bootstrap into focused PRs |

**Update (2026-10-31):** The foundation branch begins the Android shell reset and directory restructure. CI results still need independent validation before merge.
