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

- Docker start is blocked on this host because the current process cannot access
  `/var/run/docker.sock`; `sudo -n docker` requires a password. The staged JAR is
  ready at `dev-server/mods/minecraft-golf.jar`.
- S1 needs the in-game protection/operator/repair checks in `docs/M7-PLAYTEST.md`.
- S2 needs the two-client early-rejection, prompt, dismissal, exactly-once
  transition, fallback, and Hole 3 checks.
- S3 needs at least one real family/LAN session with two or more golfers.
- S4 tuning is intentionally unchanged until a recorded session supplies evidence.
- The two inherited S5 defects are implemented and await manual acceptance.
- S5 finding M7-F03: an older installed client was disconnected when the M7
  `hole_state` schema added one byte. The changed channel is now versioned as
  `hole_state_v2`, allowing Fabric's `canSend` negotiation to suppress an
  unsupported schema rather than invoke a stale decoder. Reconnect verification
  is pending after both client and server reload the rebuilt JAR.
- S6 closeout, final V1 decision, and M7 completion remain blocked on those manual
  results.
