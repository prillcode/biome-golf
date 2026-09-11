# M7 — Execution Progress

Status: **Complete.** See `docs/M7-CLOSEOUT.md` for the final verification,
accepted residual gaps, and V1 decision.

## Completed implementation

- S1 course protection: Fabric `PlayerBlockBreakEvents.BEFORE` guard, authored
  tee/cup cylindrical zones, configurable code-level radius/vertical extent,
  explicit cup protection, and operator/dev exemption.
- S2 advance UX: server-authored advance eligibility, non-pausing client prompt
  with **Go to next tee** and **Not now**, and a serverbound intent that delegates
  to the existing `ActiveHoleService.nextHole` barrier. The chat action and
  `/golf nexthole` remain available.
- S3 capture and execution: repeatable family/LAN checklist, findings log,
  prioritized defects, two complete two-player three-hole Docker rounds, and
  recorded manual-evidence gaps.
- S4 tuning: intentionally unchanged because the sessions did not provide a clear
  reason to alter the accepted course or physics baseline.
- S5 hardening: course protection, advance prompt, versioned hole-state payloads,
  Survival/Peaceful Docker defaults, explicit `ROUND COMPLETE` state, and the
  completed-round command-total correction.
- Verification harness: direct write-count idempotence assertion keeps the
  generated-layout contract covered without copying a multi-million-entry map.

## Final verification

- `./gradlew test` and `./gradlew clean build` — PASS
- 201 tests, 0 failures, 0 errors, 0 skipped
- `git diff --check` — PASS
- `net.minecraft.client` imports under `src/main/java` — 0
- Loom dedicated server — reached `Done (2.305s)` with all M7 handlers registered
- Loom client — client initialized, mod resources/atlases loaded, sound started;
  only the accepted narrator/auth-service environment warnings appeared
- Docker — healthy, Survival + Peaceful, server reached `Done (2.551s)`
- Course preparation — `6 changed`, then idempotent `0 changed`
- Stable JAR SHA-256 —
  `d277373c610b8464f822ed89469448c567219bb0df56577093fa93d002e10d76`

## Manual acceptance and decision

- Course protection and operator repair paths passed.
- Prompt dismissal, two-client prompt fanout, early-advance rejection, command/chat
  fallbacks, coordinated transitions, and final-hole behavior passed.
- Separate balls, concurrent shots, independent scoring, and two full family/LAN
  rounds passed.
- Club rendering passed on the primary client; flat 2D presentation was accepted.
- A full Minecraft restart after JAR replacement restored the versioned hole HUD;
  the explicit final `ROUND COMPLETE` display was accepted.
- The user accepted the remaining recovery/disconnect/four-player/secondary-client
  presentation evidence gaps and chose to close M7 on 2026-09-10.
- Decision: **proceed to V1/M8 planning**.
