# TapScript AI handoff

This directory exists only so a new AI coding session can continue the project without reconstructing the chat history.

Read in this order:

1. `ai/PROJECT.md`
2. `ai/TODO.md`
3. the source files relevant to the task
4. project docs only when they add needed detail (`docs/ARCHITECTURE.md`, `docs/SCRIPTING.md`, etc.)

## Working rules

- Work from the repository state, not from assumptions or old chat summaries.
- Keep code clean, OOP, SRP-oriented, and split responsibilities instead of growing large controller/UI files.
- Do not add diary files, chronological work logs, chat transcripts, duplicated summaries, speculative notes, or generated dumps to `/ai`.
- `/ai` is current-state documentation only. Delete or rewrite stale information instead of accumulating history.
- `ai/TODO.md` is the canonical remaining-work file. **Every completed development action must update it** in the same change set: remove/adjust the finished item, add discovered follow-up work, and refresh its current checkpoint. Do not append a historical changelog.
- Before claiming a code task complete, get GitHub CI green (`tests + lint + debug/release build`). Android runtime behavior that CI cannot prove must stay marked for physical-device validation.
- Do not commit signing keys, passwords, tokens, APK signing credentials, or other secrets.
- Prefer one coherent commit/change set over many tiny CI-triggering commits when practical.
- Keep user-facing UX visual and direct. Prefer icons/compact controls where text adds little value; avoid manual coordinates as the primary flow.

## Active development context

Repository: `yurii-zadryhun/TapScript`

Active branch: `feature/runtime-experience`

Open PR: runtime experience work against `master`.

The physical validation device is a Samsung Galaxy S24 Ultra on modern Android. Treat Samsung/Android runtime behavior as authoritative when it conflicts with assumptions from CI or emulators.
