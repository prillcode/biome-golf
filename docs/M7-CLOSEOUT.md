# M7 — MVP Hardening and Family Playtest: Closeout Record

Status: **Complete** on branch `feature/m7-mvp-hardening`.

M7 hardened the M5/M6 MVP for family play, resolved the known course-integrity
and next-hole UX findings, fixed session-discovered network/HUD/scoring defects,
and validated the three-hole Ready Golf loop in two-player Docker LAN play. On
2026-09-10 the user accepted the documented residual manual-evidence gaps and
made the product decision to proceed to V1/M8.

## Delivered

| Area | Result |
|---|---|
| Course integrity | Server-side Fabric block-break guard protects a configurable 12-block radius around every authored tee and cup, including the cup/flag. Ordinary Survival players can still alter terrain outside those zones; operators and the idempotent repair command remain effective. |
| Advance UX | A no-text-input client prompt offers **Go to next tee** and **Not now** when the server-authored completion barrier opens. The existing clickable chat action and `/golf nexthole` remain fallbacks; advancement stays player-initiated, server-authoritative, barrier-gated, and exactly once. |
| Gameplay profile | The portable Docker profile defaults to Survival with Peaceful difficulty. M7 did not add a custom survival subsystem or club-exclusive block breaking. |
| Protocol safety | Hole-state schema changes use versioned payload identifiers so stale clients do not decode incompatible packets. Distribution guidance now explicitly requires fully restarting Minecraft after replacing a JAR. |
| Final-round clarity | The retained final HUD is explicitly labeled `ROUND COMPLETE`, keeps the authoritative final score visible, and provides replay/exit guidance. |
| Scoring clarity | Completed-round `/golf hole status` now reads final scorecard totals rather than double-counting the retained final hole; a regression test pins the 4/3/3 = 10-stroke, −2 result. |
| Presentation | All seven club items render in inventory, first person, and third person on the primary client without missing textures. Flat 2D item presentation is accepted for the MVP. |
| Evidence capture | `docs/M7-PLAYTEST.md` records decisions, findings M7-F01–F09, prioritized defects, manual checks, the two-player LAN session, and explicit remaining gaps. |

## Family/LAN evidence

Two family players (`PrLLager207` and `pomzii_YT`) completed two three-hole rounds
on the Docker LAN server. Server logs demonstrated:

- separate player-owned balls and independent stroke totals;
- near-overlapping Ready Golf shots with different clubs;
- independent ball flight, rest, camera/travel flow, and penalties;
- rejection of an early advance while only 1/2 active golfers were terminal;
- prompt fanout after the second golfer completed;
- exactly one coordinated transition at each Hole 1→2 and Hole 2→3 barrier;
- automatic Hole 3 finalization, player-specific scorecards, and shared results;
- a retained, explicit `ROUND COMPLETE` HUD after loading the current client.

The family immediately played another full round, which is concrete replay evidence.
The younger player learned enough of the swing, club selection, putting, and Ready
Golf flow to finish both rounds. No session evidence justified changing the accepted
ball physics, club tuning, or course layout in M7, so S4 intentionally made no tuning
changes.

## Verification

### Automated and static

- `./gradlew test` — **PASS**
- `./gradlew clean build` — **PASS**
- 201 tests, 0 failures, 0 errors, 0 skipped
- completed-round cumulative-status regression added in
  `ActiveHoleServiceStatusTest`
- `net.minecraft.client` imports under `src/main/java` — 0
- `git diff --check` — clean before closeout documentation

### Loom

- Dedicated server loaded Minecraft 26.2/Fabric, the three-hole course, seven clubs,
  shot and hole-state networking, the M7 next-hole action, commands, and the course
  guard; it reached `Done (2.305s)` and shut down cleanly.
- Client initialized the common and client entry points, loaded `minecraft_golf`
  resources, created the item/block/GUI atlases, and started the sound engine. The
  missing optional Linux `flite` narrator library and dev Realms authorization warning
  are known non-gameplay environment limitations.

### Docker integration

- Container: healthy; dedicated server reached `Done (2.551s)`.
- Gameplay environment: `MODE=survival`, `DIFFICULTY=peaceful`.
- Stable JAR SHA-256: `d277373c610b8464f822ed89469448c567219bb0df56577093fa93d002e10d76`.
- Build artifact, staged Docker mod, live `/data/mods` JAR, and laptop client mod
  matched that SHA at verification time; the same artifact was sent to `MINIPC-ZJO6I`
  over the tailnet.
- `/golf dev preparecourse`: 574,612 planned blocks, first pass repaired 6 gameplay
  changes, second pass changed 0; layout remained `minecraft_golf:m5_ocean_campus`
  v11 within the documented campus envelope.

## Accepted residual gaps

The user explicitly chose to close M7 and continue development with these remaining
manual-evidence gaps recorded rather than represented as passes:

- individual restart, deliberately missing ball, and withdrawal isolation were not
  rerun with both clients during M7;
- the full disconnect matrix (offline advancement, reconnect after withdrawal,
  disconnect in flight, and all-disconnected recovery) retains M6 unit/code evidence
  but was not exhaustively replayed over two M7 clients;
- solo start/restart/abandon/replay was accepted earlier but not repeated after the
  final two-player session;
- second-client third-person club presentation was not explicitly observed;
- no physical four-player LAN session was run; four-player domain coverage remains
  automated rather than on-wire;
- numeric experience ratings and a direct answer about proposing new holes were not
  captured.

These are evidence gaps, not known MVP-critical defects. Any recurrence during V1
work should be triaged against the preserved M6 server-authority and isolation
contracts.

## Accepted limitations and deferred work

- One active round per server, in-memory round state, and no server-restart round
  persistence remain intentional MVP limits.
- A broader/configurable authored protection model may be reconsidered if the accepted
  12-block vicinity proves too small.
- Hole 3 length and Driver/Fairway Wood rollout remain tuning candidates without enough
  M7 evidence to justify changes.
- Wind, advanced lies, cinematic camera, landing preview, richer presentation,
  alternate modes, authoring tools, and the remaining M8 candidates were not pulled
  into M7.

## V1 decision

**Proceed to V1/M8.** The two-player family session completed the course repeatedly
without state corruption or forced turns; server authority, independent scoring,
barrier transitions, finalization, course protection, and the Survival/Peaceful
profile behaved as intended. The family voluntarily replayed, and the user explicitly
chose to move forward. The evidence supports planning M8, while the residual checks
above remain visible risk rather than hidden completion claims.

## M8 handoff

M8 is ready for planning, not immediate unbounded implementation. Prioritize its
candidate scope from observed player value and define explicit exit criteria first.
Preserve the M7 stable baseline and all M6 server-authority/concurrency contracts.
Do not assume every candidate feature belongs in V1.
