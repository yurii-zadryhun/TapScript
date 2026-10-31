# TapScript — Product Requirements Document

**Status:** Draft for architectural review (v0.1)  
**Date:** 2026-10-31  
**Repository:** https://github.com/yurii-zadryhun/TapScript  
**Scope:** Android-first automation platform, web authoring environment, cloud API, and community sharing.

> This is a proposal, not an approved specification. Sections marked **Decision pending** must be settled before implementation. Priority labels describe a proposed delivery sequence, not a commitment to deliver every item in the first version.

## 1. Product vision

TapScript lets users **build, run, debug, reuse, publish, and discover mobile automation scripts**. It combines a native Android automation runtime, developer-grade scripting tools, visual selection of on-screen points and regions, a web-based editor, cloud synchronization, and a community library of reusable scripts.

The product must not be designed as a remote, unrestricted device-control service. On-device automation remains visible, user-controlled, constrained by Android permissions, and subject to clear execution/capability consent.

### Product principles

1. **Local execution, cloud collaboration.** OCR, recognition, screen reading, and touch injection occur on the Android device; the server never needs live screenshots by default.
2. **Offline-capable execution.** An already installed, validated script bundle can run without the API, subject to Android permissions and the active capture session.
3. **One scripting API.** The same versioned API contract powers documentation, type checking, autocomplete, validation, and the Android runtime.
4. **TypeScript authoring; constrained JavaScript execution.** Editor files may be TypeScript/JavaScript; the runtime consumes bundled JavaScript and a dependency lock manifest.
5. **Safe by default.** Third-party scripts are untrusted; installing or upgrading a script never silently grants new capabilities.
6. **No code duplication across editor surfaces.** Web/mobile can have different UI, but share language semantics, data contracts and package compatibility rules.
7. **Explicit versioning.** Script releases, their dependency graphs, SDK compatibility and runtime capabilities are reproducible and diagnosable.
8. **Modularity without premature microservices.** Prefer a modular backend and native Android ports/adapters.

## 2. Users and jobs to be done

| Persona | Primary job |
|---|---|
| Script author | Write/debug scripts, visually pick screen targets, create reusable utilities and publish versions |
| Script user | Discover a script, inspect requested capabilities, install, configure and run it |
| Power user | Maintain a private library across devices, fork scripts and troubleshoot execution |
| Community maintainer | Moderate public content, reports, descriptions and versions |

Core journeys:

1. **Local first-run:** install APK → open app → grant only required Android permissions → define a point/region → write a script → run/pause/stop → inspect logs.
2. **Cross-device authoring:** sign in on Android and web → open same project → edit/save with revision conflict handling → sync and compile → execute on Android.
3. **Community reuse:** browse public listing → review manifest, source, permissions and version → install version with pinned dependencies → configure points/regions → run locally → optionally fork/publish a derivative.
4. **Import reuse:** write a library module → release an immutable version → declare dependency in another project → import typed exports → build a reproducible bundle.

## 3. Release scope and requirement priorities

- **P0 / Foundation:** required to establish the future-proof product architecture and a runnable Android shell.
- **P1 / Core MVP:** script development/execution on a single device plus authenticated account, baseline web editor/sync.
- **P2 / Community:** publishing, search, sharing, installation, version management and moderation.
- **P3 / Advanced:** visual debugger, extended recognizers, richer module ecosystem, optional pairing workflows.

A P0 item does not imply all infrastructure must exist on day one. Scaffolding must not create fake functionality or dead code.

### FR-01: Platform and identity — P0/P1

- Android client: native Kotlin + Jetpack Compose for runtime, permissions and device UI.
- Web client: TypeScript web application for editing, managing, sharing and browsing scripts; **not** a browser-based Android execution engine.
- API: NestJS server owning auth, accounts, projects, releases, assets, synchronization and moderation.
- Google OIDC as initial login provider; account identity is a stable provider subject, **not** an email address.
- Separate first-party session handling for web and mobile; refresh/revocation and device/session listing.
- Permit guest/local-only script creation and execution without signing in, where feasible.
- Clearly distinguish local-only data from synced data; link/merge on first login without silent data loss.
- Support account deletion, export and removal of synced/private assets.

**Acceptance:** User can sign in on both surfaces to the same account; Android automation continues when temporarily offline; logging out revokes cloud session but does not silently erase explicitly retained local work.

### FR-02: Script projects and authoring — P1

- Project contains multiple files (`.ts`, `.js`), metadata, asset references and dependency declarations.
- Editor functionality: syntax highlighting, diagnostics, autocomplete from the versioned SDK, file tree, formatting, search, save, undo, draft/revision history.
- Full source editing from web **and** mobile; web and mobile must produce semantically equivalent bundles.
- Explicit save status and optimistic concurrency handling; conflicts are surfaced rather than overwritten.
- Runtime compilation must not rely on a hidden always-online backend for scripts already installed.
- Script manifest includes ID, display name, entry point, API version, required capabilities, dependencies, target definitions, and runtime compatibility.
- A draft is mutable; a published release is immutable.

**Acceptance:** A user edits an existing project on Android and web, gets type diagnostics, compiles it and runs the same immutable bundle on Android.

### FR-03: Runtime and execution lifecycle — P1

- Android-only execution of script bundles, with explicit start/pause/resume/stop, cancellation and kill switch.
- `async` workflows, non-blocking waits, retries/timeouts and event-based conditions. No main-thread blocking or busy waiting.
- Single execution context owns state, logging, cancellation and capability checks; concurrent automation arbitration is explicit.
- Structured errors with file/line/stack when available; bounded local execution history and logs.
- Script budgets: CPU time, memory, maximum calls/actions, action frequency and total execution duration. Limits must be configurable in implementation, not controllable by untrusted script code.
- Ability to run a script again on previously installed pinned versions (unless permission revoked/incompatible).

**Acceptance:** Infinite loop, runtime exception, lost screen capture and user cancellation all terminate or suspend safely; the device UI and app remain responsive.

### FR-04: Device automation SDK — P1/P3

Proposed capability families:

| Family | Example methods | Priority |
|---|---|---|
| Gestures | `tap`, `longPress`, `swipe`, `scroll`, `multiTap` | P1 |
| Timing | `sleep`, `waitUntil`, `retry`, randomized bounded delay | P1 |
| Screen | `capture`, `crop`, current display metrics/rotation | P1 |
| Text recognition | OCR named region, read text with metadata/confidence | P1 |
| Targets | Resolve point/region by slug, coordinate transforms | P1 |
| Images | Template/icon matching, pixel/color checks, confidence threshold | P2 |
| App control | Open launchable application with user-declared target | P1/P2 |
| Accessibility tree | Element queries, bounds and semantic tap where available | P2 |
| Debug | Structured log, inspect screenshot/regions with explicit consent | P1/P2 |
| Randomness | Bounded randomized tap offsets and wait ranges, optional seed | P1 |
| Other | Text entry, device state events, advanced gestures | P3, platform/privacy review |

All method parameters, return shapes and failure codes must be defined in a versioned TypeScript SDK. Screen reads must return explicit absent/unavailable results rather than fabricated success. Advanced actions require capability review; an exposed SDK API does not imply a guaranteed ability to act in every third-party app.

### FR-05: Visual points, regions and target packs — P1/P2

- User selects a point, rectangle, line/swipe target or template from a screenshot/live frozen frame.
- Targets have stable IDs, human-readable labels, unique slugs scoped to a project/target pack, geometry and provenance.
- Authoring allows normalized coordinates and *explicit* reference frames: entire display, app content area or a recognized anchor.
- Resolution/density/rotation/insets transformation is centralized and tested; target preview shows the resolved tap point/ROI.
- Configurations are reusable and can be overridden per device/profile; scripts refer to semantic slugs, not literal screen pixels.
- Missing targets and incompatible layouts fail visibly, not with arbitrary screen touches.

**Acceptance:** Define `sell-button` in the visual picker, reference it in code and have the gesture land at the expected position on supported device geometry.

### FR-06: Reusable modules and dependencies — P1/P2

- ECMAScript-style exports/imports in authoring files; libraries can be internal project modules or separately versioned shared packages.
- Resolve dependencies at **build/install time** into an immutable bundle plus lock manifest; no live cloud imports during script execution.
- Distinguish scripts (entry points) from libraries (modules with exports, no automatic execution).
- Version constraints, transitive dependency graph, circular dependency handling and source attribution are part of build validation.
- Detect missing/incompatible imports, duplicates, conflicts and unsupported SDK versions before execution.
- Updates to dependencies never silently change installed published bundles.

**Acceptance:** A published script can import a helper module, install and run offline using the exact pinned dependency content.

### FR-07: Cloud synchronization and device management — P1/P3

- Authenticated users synchronize private projects, drafts/versions, target packs and asset metadata as explicitly configured.
- API supports idempotent writes, revision tokens/ETags, incremental sync and conflict detection.
- Local cache/database is the source for execution; remote sync never blocks stop/pause.
- Device registration identifies app/runtime version and supports revoke/disconnect.
- Optional future web-to-device run requests use device-initiated authenticated channel, explicit local confirmation and auditable scope. No unattended remote control in MVP.
- Live screenshots and sensitive execution traces are not uploaded by default.

### FR-08: Publication, discovery and sharing — P2

- Visibility: private, unlisted, public; separate edit rights from install rights.
- Profile, scripts, releases, tags, description, screenshots (opt-in), documentation, license and changelog.
- Browse/search/filter/sort; stable share URLs; follow/save/bookmark/fork (ratings/comments optional follow-ups).
- Publishing creates immutable version with source/bundle/hash/lockfile and declared capabilities.
- Install presents source metadata, third-party dependencies, requested capabilities, warnings and compatibility.
- Report abusive/malicious scripts; moderation controls, takedown, rollback/deprecation and abuse rate limits.
- User-generated scripts are untrusted and never receive Android-wide unrestricted privileges.

### FR-09: Reliability, privacy and security — P0 onward

- Device inputs and screenshot capture must be permission-gated, visible and immediately stoppable.
- Credential storage in Android Keystore-backed facilities; short-lived web sessions protected from XSS/CSRF/session fixation.
- HTTPS, server authorization for every private resource, audit logging for security-sensitive actions, input validation, output escaping.
- No access for arbitrary scripts to Google credentials, auth cookies, host Android objects, Java reflection, raw filesystem, or unrestricted network.
- Capability-based host bridge; explicit permissions requested at install time and on newly introduced capabilities.
- Sandbox threat model includes intentionally hostile public code, infinite loops, memory exhaustion, hostile dependencies, phishing via UI automation and data exfiltration.
- Block script access to sensitive UI contexts as feasible; document limitations honestly.
- Automatic screenshots/logs uploaded to cloud: **disabled by default**.
- Do not bypass OS permissions, security controls, app anti-abuse systems, or accessibility disclosure requirements.

## 4. Draft data model

Use persistent stable UUIDs plus human-readable slugs. Distinguish **ownership**, **version**, **visibility**, and **installation** explicitly.

- `User`, `Identity`, `Session`, `Device`
- `Project`, `ProjectMember` (future), `ProjectDraft`, `ProjectRevision`
- `SourceFile`, `Asset`, `TargetPack`, `TargetDefinition`, `DeviceTargetOverride`
- `Script`, `ScriptRelease`, `ModulePackage`, `PackageRelease`, `DependencyLock`
- `Installation`, `CapabilityGrant`, `ExecutionMetadata` (optional upload)
- `Publication`, `Tag`, `Bookmark`, `ForkReference`, `Report`, `ModerationAction`

**Recommended invariants:** release immutable; dependency lock content-addressed; permission grants never inferred from visibility; uninstall/revoke does not erase authored project; drafts have concurrency revision; publishing is atomic and auditable.

## 5. Non-functional requirements

| Dimension | Requirement / validation |
|---|---|
| Offline | Existing valid installed bundles run without network; first capture permission still respects Android consent rules |
| Performance | Establish real-device benchmarks for capture latency, OCR per ROI, gesture dispatch, compiler time, startup and memory before setting p95 budgets |
| Stability | Runner and UI failures isolated; cancellation works during wait/OCR/gesture; foreground service lifecycle handled |
| Compatibility | Android minimum/target API levels documented and tested; compatibility declared per release/SDK version |
| Security | Reproducible untrusted-script threat-model tests; sandbox escape and resource exhaustion tests mandatory before marketplace |
| Privacy | Data minimization, opt-in diagnostics, retention periods, user export/deletion |
| Quality | Unit tests, integration tests, Android instrumentation tests, security checks, API contract checks, smoke builds |
| Deployment | Independently deploy web/API; independently build/sign Android; maintain staged rollout and rollback |
| Observability | Structured logs and metrics without leaking script content, auth tokens or screenshots |
| Scalability | Modular monolith; storage grows independently of stateless API; background jobs added when load justifies them |
| Accessibility/UX | The editor and controls should remain usable with small screens and standard accessibility practices |

## 6. Business rules and boundaries

- Android automation is **not** a server-side service. The cloud stores/coordinates; the device interprets bundles and performs local actions.
- Public script publishing is a separate trust boundary from private script editing.
- Changing a public release means publishing a new version, not mutating previously installed code.
- A user can always stop a local run, including one initiated via a future paired-device workflow.
- No script is allowed to request arbitrary host-language reflection or direct access to authentication material.
- A capability request is not sufficient by itself: the platform must also enforce Android OS permissions and product distribution policy.
- Any future AI-generated autonomous actions require a new policy and safety review; they are not in scope for the initial product.

## 7. Scope exclusions / non-goals for initial phases

- iOS equivalent to Android Accessibility automation (requires a separate feasibility analysis).
- Root, privileged APIs, integrity bypass or hidden device control.
- Cloud-executed tap/swipe/OCR over a live device display.
- Running arbitrary npm packages, native binaries, scripts from URLs, or unrestricted filesystem/network APIs.
- Microservices, Kubernetes, event sourcing, full collaborative realtime CRDT editing, AI agents or payments in MVP.
- Fully automated Google Play acceptance: distribution viability must be assessed independently.

## 8. Suggested implementation milestones

### M0 — Requirements and architecture gate

Approve PRD, platform distribution strategy, trust boundaries, script language/compiler decision, execution SDK shape, versioning, account/offline model. Write ADRs and spike plans.

### M1 — Repository baseline

Create `apps/mobile`, `apps/web`, `apps/api`, `packages/*`; remove legacy profile engine; preserve a launchable Android shell and build pipeline; add web/API minimal services, healthchecks, CI, lint/test scaffolds.

### M2 — Local vertical slice

Write/typecheck one script, define a point/region, run `tap`, `sleep`, OCR, debug logs, stop/cancel, survive device rotation as specified. Script install/execution works offline on Android.

### M3 — Cloud authoring

Google OIDC, account/session management, private projects, web editor, device sync, optimistic revision conflict handling and reproducible compile/version checks.

### M4 — Community beta

Public/unlisted script releases, metadata and discoverability, install/fork, capability review, dependency locks, moderation/reporting and hostile-code validation.

### M5 — Expanded automation

Advanced recognizers, visual step debugger, app selectors, target overrides, optional audited pairing and controlled remote run requests if approved.

## 9. Definition of done (per vertical slice)

- User journey works end-to-end on real Android device and web where appropriate.
- Contract/types/docs updated together; no undocumented SDK behavior.
- Build, lint, tests, and relevant policy/security checks pass in CI.
- Error states and missing permissions are tested and readable.
- No hidden backend dependency for promised offline functionality.
- Secrets and sensitive screenshots do not leak in logs/artifacts.
- Release version, migrations, rollback and telemetry decision recorded.

## 10. Decisions pending — resolve before code removal

1. **Distribution:** Play Store, direct APK, F-Droid/other, or multiple channels? This materially affects Accessibility policies.
2. **Offline:** Must the Android editor compile new/edited TypeScript bundles offline, or is offline **running installed bundles** sufficient for MVP?
3. **Script language:** TypeScript source with native JS runtime vs Kotlin/DSL alternatives; supported syntax and module system.
4. **Sandbox:** isolate native JS runtime (QuickJS or other), IPC boundary, cancellation, quotas, hostile-code guarantees, supply-chain updating.
5. **Cloud ownership:** local guest mode and what happens when account is deleted or signed out.
6. **Remote execution:** permanently excluded vs future explicit-confirmation feature.
7. **Public content:** licensing, moderation process, legal terms and whether scripts can contain images/assets copied from other apps.
8. **Android scope:** minimum Android API, supported gesture types and accessibility tree exposure; feature compatibility matrix.
9. **Pricing/business model:** free/open-source vs hosted accounts, quotas and paid features; can be postponed but affects data policy.
10. **Target-pack semantics:** per-project, reusable global, per-app and per-device override behavior.

## 11. External platform constraints (checked 2026-10-31)

- Google Play Accessibility policy limits automations to narrowly understood, deterministic human-defined behavior for non-accessibility tools, and requires disclosure/consent; unrestricted autonomous agents are prohibited. https://support.google.com/googleplay/android-developer/answer/10964491
- Android MediaProjection requires user permission per capture session and foreground service type requirements on modern Android. https://developer.android.com/media/grow/media-projection
- Android background foreground-service starts and activity launches have platform restrictions. https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Google ID tokens must be validated server-side (signature, audience, issuer, expiry); Android Credential Manager is the recommended identity entry point. https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation

## 12. Feedback and decisions received on 2026-10-31

This section records product-owner feedback after v0.1. The earlier proposals remain drafts until individually reviewed.

| Topic | Status | Direction / caveat |
|---|---|---|
| Distribution | **Accepted** | Both Google Play and direct APK via GitHub Releases. Validate actual policy before shipping; user-authored scripts are not a blanket exception. |
| Local-first | **Accepted** | Scripts and already installed releases must remain executable offline; cloud use should be optional. |
| Backend-driven changes | **Principle accepted** | Remotely version data, projects, policies and supported capabilities; native Android features still require APK changes. Never execute arbitrary downloaded native code to evade updates. |
| API compatibility | **Accepted** | Version public API and separately negotiate device capabilities, script SDK and bundle/runtime compatibility. |
| OCR abstraction | **Accepted** | Use a recognition port; ML Kit initial adapter candidate, Paddle/other future implementations. |
| TypeScript | **Preferred, spike required** | Author source in TS, compile to JS, evaluate engine choice and hostile-code isolation on Android. |
| Frontend | **Preferred** | Next.js frontend with NestJS as business API, initially static export/client data fetching when feasible. |
| Sharing unit | **Strong proposal** | Publish/share **projects** with files, assets, dependencies, metadata and immutable releases rather than standalone scripts. |
| Screen elements | **Strong proposal** | Typed importable definitions for regions, points, templates and targets; serialized declarative data with generated typed exports. |
| Runtime state | **Required to design** | Isolate project execution with scoped persistent storage, bounded memory, capabilities, lifecycle and cleanup. |
| Google OIDC | **Proposed** | Account authentication across Android and web; guest local authoring remains valid. |
| Mobile implementation | **Phase M1 approved** | Relocate to `apps/mobile` and reset to a minimal compiling Android shell; preserve old code in Git history. |

**Follow-up:** The product questionnaire in `docs/QUESTIONNAIRE.md` is the source of discovery questions. Do not infer agreement with unanswered options.
