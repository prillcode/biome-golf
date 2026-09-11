# M7 — Execution Progress

Status: **Agent-executable work complete through the manual playtest gate.**

## Completed implementation

- S1 course protection: Fabric `PlayerBlockBreakEvents.BEFORE` guard, authored
  tee/cup cylindrical zones, configurable code-level radius/vertical extent,
  explicit cup protection, and operator/dev exemption.
- S2 advance UX: server-authored advance eligibility, non-pausing client prompt
  with **Go to next tee** and **Not now**, and a serverbound intent that delegates
  to the existing `ActiveHoleService.nextHole` barrier. The chat action and
  `/golf nexthole` remain available.
- S3 capture structure: repeatable family/LAN checklist, deferred M6 two-player
  matrix, presentation checks, findings log, prioritized defect list, and
  evidence-gated tuning candidates in `docs/M7-PLAYTEST.md`.
- Verification harness: replaced a full copy of the multi-million-entry generated
  layout map in `DevelopmentCourseGeneratorTest` with a direct write-count
  idempotence assertion. This preserves the contract and reduces the full suite
  from minutes/worker EOF failures to seconds.

## Automated verification

- `./gradlew clean build --no-daemon` — PASS
- 198 tests, 0 failures, 0 errors
- `git diff --check` — PASS
- `net.minecraft.client` imports under `src/main/java` — 0
- Loom dedicated server on port 25566 — reached `Done (1.381s)`, registered the
  M7 next-hole networking and course block-break guard, then stopped cleanly
- Loom client — initialized Minecraft Golf client, registered both M7 paths,
  reloaded resources, created item/GUI atlases, and started sound; only the
  previously accepted dev narrator/auth-service warnings appeared
- Built/staged JAR SHA-256 identity —
  `cde1846f5db84573440e0e5af78d7e5f3d11cd7988e8d4130c860fdbe1dabd81`

## Manual and environment gates

- Docker server — healthy with the staged M7 JAR; the `prill` account's Docker
  group membership was restored during verification.
- S1 — accepted on Docker: non-operator Creative protection passed at Hole 1 and
  Hole 2 tees and all three greens/cups, ordinary terrain remained editable,
  operator repair access passed, and preparation repaired deliberate damage
  (`10 changed`) before an idempotent rerun (`0 changed`).
- S2 solo paths are accepted on Docker: prompt dismissal, HUD exactly-once
  advancement, clickable-chat and command fallbacks, and Hole 3 finalization
  without another prompt. Two-client early rejection and all-terminal prompt
  fan-out remain.
- S3 needs at least one real family/LAN session with two or more golfers.
- S4 tuning is intentionally unchanged until a recorded session supplies evidence.
- M7-F05 — Docker's persistent gameplay profile is now Survival + Peaceful; golf
  clubs retain ordinary vanilla block interaction rather than becoming exclusive
  obstacle-removal tools.
- M7-F01 is manually accepted; inherited finding M7-F02 still needs the two-client
  advance-flow acceptance pass.
- S5 finding M7-F03: an older installed client was disconnected when the M7
  `hole_state` schema added one byte. The changed channel is now versioned as
  `hole_state_v2`, allowing Fabric's `canSend` negotiation to suppress an
  unsupported schema rather than invoke a stale decoder. A matching M7 client
  connected successfully after both client and Docker server reloaded the rebuilt
  JAR on 2026-09-10.
- S6 closeout, final V1 decision, and M7 completion remain blocked on those manual
  results.
