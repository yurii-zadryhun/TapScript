# TapScript — Product Discovery Questionnaire

**Status:** Open questions; no answer is assumed.
**Instructions:** Respond by IDs (e.g. Q-001). Group-by-group discussion is preferred; this is a discovery backlog, not a mandatory form.

## Product direction

- **Q-001** What single problem does TapScript solve better than existing autoclickers?
- **Q-002** Is it an automation IDE, a consumer autoclicker, a script marketplace, or all three equally?
- **Q-003** Which feature should make users recommend TapScript to someone else?
- **Q-004** What use cases are explicitly out of scope?
- **Q-005** What are the main 12-month success criteria?
- **Q-006** Is the primary target hobbyists, developers, or nontechnical users?
- **Q-007** Should TapScript be open source, source available, or closed source?
- **Q-008** Should the product be positioned as general automation or game automation?

## Personas & scenarios

- **Q-009** Who is the primary author persona?
- **Q-010** Who runs projects without understanding TypeScript?
- **Q-011** What are the three strongest end-to-end user scenarios?
- **Q-012** What is the most important first-run experience?
- **Q-013** How are projects discovered by a nontechnical user?
- **Q-014** How much configuration can an installer be expected to perform?
- **Q-015** Are users expected to automate one app at a time or coordinate across apps?

## Platforms & distribution

- **Q-016** Is Android the only execution target for the foreseeable future?
- **Q-017** Should tablets and foldables be first-class?
- **Q-018** What is the minimum supported Android version?
- **Q-019** What should differ between Google Play and GitHub APK builds?
- **Q-020** Should app updates be optional for already installed offline projects?
- **Q-021** Will Google Play compliance alter the product's core workflows?
- **Q-022** Does web ever execute scripts, or only edit and simulate them?

## Projects & content

- **Q-023** Is the public sharing unit a project rather than a script?
- **Q-024** Does a project have one or multiple executable entry points?
- **Q-025** Which files can a project contain?
- **Q-026** Can a project be both a runnable app and a reusable library?
- **Q-027** Should projects support forks, templates and dependency imports?
- **Q-028** What are the expected limits on project size and assets?
- **Q-029** What project metadata is visible publicly?
- **Q-030** What is the ownership/licensing model for published projects?

## Execution behavior

- **Q-031** Does a project run only when started by the owner?
- **Q-032** Should it support scheduled starts, triggers, events or hotkeys?
- **Q-033** Can multiple projects run concurrently on one device?
- **Q-034** What happens when a script crashes or Android revokes capture?
- **Q-035** Should scripts survive app restarts or device reboots?
- **Q-036** Should execution state be persisted, and for how long?
- **Q-037** How should users pause, resume and emergency-stop execution?
- **Q-038** Does the runtime need an interactive debugger in v1?

## Automation capabilities

- **Q-039** Which capabilities are required for the first useful project?
- **Q-040** Should OCR be available offline by default?
- **Q-041** Does click/swipe support absolute, relative and target-based coordinates?
- **Q-042** Do scripts need Android accessibility-tree selectors beyond pixels?
- **Q-043** Are image matching, color sampling and template recognition first-class?
- **Q-044** Should scripts launch apps and navigate between them?
- **Q-045** Should randomization be a built-in API with configurable distributions?
- **Q-046** Will scripts need clipboard, notifications or network requests?

## Screen objects

- **Q-047** Should points/areas be author-created entities with stable IDs and slugs?
- **Q-048** Should regions be global, project-local or scoped to a screen?
- **Q-049** How are different resolutions, aspect ratios and orientations handled?
- **Q-050** How are visual targets defined when UI moves?
- **Q-051** Should screen objects be exported as TypeScript symbols?
- **Q-052** Should their backing data be declarative JSON, classes or generated modules?
- **Q-053** Can users override screen calibration per device?

## Authoring & editor

- **Q-054** Is code-first the default or should a visual builder be equally important?
- **Q-055** Should mobile editing be fully featured or optimized for quick fixes?
- **Q-056** What editor features are essential at launch?
- **Q-057** Should all changes autosave locally?
- **Q-058** How should web edits reach an offline phone?
- **Q-059** Should projects have a built-in preview/simulator?
- **Q-060** What quality of errors, logs and diagnostics is required?
- **Q-061** Do we need live device pairing from the web editor?

## Accounts, cloud & offline

- **Q-062** Can anyone create and run a project without signing in?
- **Q-063** Does publishing require a verified account?
- **Q-064** What is the desired Google OIDC experience on each platform?
- **Q-065** What data is stored only on device?
- **Q-066** What data is synchronized by default?
- **Q-067** What happens to local work when account login changes?
- **Q-068** How are offline edit conflicts resolved?
- **Q-069** Should a user manage multiple devices from one account?

## Security & trust

- **Q-070** Can installed public projects request network or storage access?
- **Q-071** Should projects declare permissions/capabilities in a manifest?
- **Q-072** What user confirmation is needed for first run and after upgrades?
- **Q-073** Should secrets be project-scoped and inaccessible to other projects?
- **Q-074** How do we prevent untrusted scripts from exhausting CPU or battery?
- **Q-075** What audit information should users see before running a project?
- **Q-076** Are project sources always publicly inspectable?
- **Q-077** What controls apply to potentially abusive automation?

## Community & monetization

- **Q-078** Do public projects have reviews, ratings, comments and reports?
- **Q-079** Are private/unlisted projects supported?
- **Q-080** Should creators publish new releases separately from editable drafts?
- **Q-081** How are dependencies updated and pinned?
- **Q-082** Should other people be able to contribute changes through pull requests?
- **Q-083** What moderation capacity is realistic initially?
- **Q-084** Do creators eventually sell projects or accept donations?
- **Q-085** Will the platform have subscriptions, paid features or remain free?

## Quality, ops & roadmap

- **Q-086** What should the MVP prove to you personally?
- **Q-087** Which three vertical slices should be built first?
- **Q-088** What performance/battery impact is acceptable during OCR?
- **Q-089** What is the supported compatibility window for old APKs?
- **Q-090** Which features must work without any backend?
- **Q-091** How much analytics/telemetry should the app collect, if any?
- **Q-092** How will we measure runtime compatibility and crash rates?
- **Q-093** What decisions must be answered before implementation begins?
