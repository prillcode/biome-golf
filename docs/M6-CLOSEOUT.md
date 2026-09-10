# M6 — Multiplayer Ready Golf: Closeout Record

Status: **Complete** on branch `feature/m6-multiplayer-ready-golf`.

M6 adds 1–4 player Ready Golf concurrency to the proven M5 three-hole course without
redesigning the course, physics, swing, or single-player flow. Every golfer keeps an
independently owned ball, hole state, HUD snapshot, and score while the server owns
the shared participant roster, current hole, advancement barrier, and final results.

## Delivered

| Area | Result |
|---|---|
| Round domain | Minecraft-free `ReadyGolfRound`/`ReadyGolfParticipant`/`RoundPhase`/`ParticipantStatus` with round identity, course, `LOBBY`/`PLAYING`/`COMPLETE` phases, per-golfer `PlayerCourseState`, stable join-order presentation, and atomic shared hole index. |
| Round commands | `/golf round create`, `/golf round join`, `/golf round start`, `/golf round leave`, `/golf round status`. Coordinator starts the round; coordinator handoff in stable join order on leave/disconnect; empty lobby removed. Roster locked at start; late joiners may observe/practice only after the active round ends. |
| Solo compatibility | `/golf hole start` remains the backward-compatible one-player fast path with unchanged M5 start, three-hole progression, finalization, and replay. `/golf hole restart` stays player-scoped recovery; `/golf hole abandon` becomes participant withdrawal in multiplayer. |
| Ready Golf play | No turn owner and no shot-order check; any non-terminal participant may shoot whenever their own assigned ball rests. Hole terminal states are hole-out, Pick Up, or stroke cap. One golfer's shot, penalty, restart, Pick Up, or hole-out never mutates another golfer's state. |
| Advancement barrier | For Holes 1–2, `/golf nexthole` (from any active participant) succeeds only when every active participant is terminal, then transitions all once and idempotently. Terminal golfers see a waiting state and cannot force early advancement. Hole 3 finalizes the shared round automatically when the last active golfer becomes terminal. |
| Multiplayer results | Server-generated per-hole and final result tables in deterministic participant order; each HUD remains based on that golfer's own score. Transition rollback/fail-safe keeps participants from splitting across holes if ball creation or teleport preparation fails. |
| Disconnect/reconnect | Join/disconnect hooks apply the locked suspension policy on the server thread. Disconnect suspends an in-progress golfer and removes them from the barrier; timely reconnect resumes the same assigned ball/score/HUD after entity validation; advancing without an offline golfer withdraws their stale state and removes their ball; all-disconnected rounds stay suspended; round ends when no active participants remain. |
| Concurrency isolation | Independent legal shot requests on adjacent ticks both launch; automatic travel is owner-only and triggers only from that owner's naturally resting active ball; each client receives only its own hole/HUD snapshot and follows only its own ball while all clients observe all loaded golf balls via normal entity synchronization. |
| Club sprite repair | Fixed the Minecraft 26.2 item-definition/resource path behind the magenta/black club artifacts; replaced the one shared placeholder with seven distinct Minecraft-style 2D club sprites (driver, fairway wood, long/mid/short iron, wedge, putter) verified across item definitions, models, and textures. |

## Verification

### Automated

- `./gradlew test` — **PASS**
- `./gradlew clean build` — **PASS**
- 187 tests, 0 failures, 0 errors, 0 skipped (15 in `ReadyGolfRoundTest` covering two-player isolation, barrier behavior, hole 3 completion, disconnect/reconnect/withdraw, all-disconnected suspension, last-participant withdrawal, solo replay, coordinator handoff, and invalid-transition safety)
- `net.minecraft.client` imports under `src/main/java` — 0
- `git diff --check` — clean

### Loom

- `runClient`: `Minecraft Golf client initialized`, resource reload includes `minecraft_golf`, `Sound engine started`, rendering continued; only benign dev-environment errors (dev-account authlib 401, narrator `flite` missing, Realms auth, GLFW cursor shape).
- Dedicated server reached `Done` (run on port 25566 during S5 after the initial attempt hit the Docker 25565 port conflict); round, shot, HUD, command, join, and disconnect handlers registered.

### Docker integration server

Verified against the post-fix JAR:

- container booted `Done (2.113s)!`; health `healthy`
- staged (`dev-server/mods`) and container (`/mods`) JAR SHA-256 identical: `efc2bde808bf81687f4902c5fb3bf4d06d0fb92016e8f339f3d5d8345468e5b1`; laptop client JAR and mini-PC copy carry the same SHA
- `/golf dev preparecourse` idempotence: `minecraft_golf:m5_ocean_campus v11`, envelope `X[-640..448] Y[32..192] Z[-256..640]`, seed check passed; first run repaired 7 drifted blocks (gameplay drift, self-healing as designed), second run reported **0 changed** — M6 did not alter M5 layout v11

### Solo M5 regression (user-driven)

- Regression tests 1–2 (hole start, restart) passed on the S5 JAR.
- Test 3 (abandon → start) was initially blocked by the stale-state bug fixed in `68447e7` (solo `/golf hole abandon` left the golfer `WITHDRAWN` inside a `COMPLETE` round); after deploying the fix, the user confirmed test 3 passes against the fixed server (2026-09-10).

### Manual gameplay acceptance

- **Two-player manual matrix (items 1–9): deferred** until a second golfer is available; to be run by the user (see `docs/M6-PLAN.md` for the matrix).
- **Four-player LAN acceptance: waived by user decision**; four-player correctness is evidenced by the four-player domain unit tests in `ReadyGolfRoundTest` plus code review of the server integration, with the two-player manual matrix as on-wire proof. A true four-player session may be revisited during M7 family playtesting.

## Accepted findings and known limitations

- The two-player manual matrix and physical four-player LAN session remain outstanding user-side items; they are tracked in `docs/M6-PLAN.md` and may be revisited during M7 family playtesting.
- M6 S4 two-player playtest findings are recorded in the M7 section of `docs/MILESTONES.md`: creative-mode course destructibility (tee boxes/greens/cup/flag need protection via a server-side block-break guard, keeping in-the-way tree/rock clearing fun) and advance-UX (playtesters want a no-text-input prompt or full auto-advance; full auto-advance would deviate from the M6 player-initiated `/golf nexthole` contract, so it is a deliberate M7 product decision).
- One active round per server on the one configured M5 course; multiple simultaneous rounds, matchmaking, and persistent server-restart round state remain out of scope.
- Round state remains in-memory; server-restart persistence and persistent global statistics remain out of scope.

## M7 handoff

M7 should harden the MVP for repeated family/LAN play on top of the proven Ready Golf round:

- Run repeated family/LAN sessions with two or more golfers, including the deferred two-player manual matrix and an optional true four-player session.
- Resolve the two playtest findings above: a server-side block-break guard protecting course metadata (tee/green/cup plus vicinity radius), and a deliberate advance-UX decision (HUD prompt vs. full auto-advance vs. keeping `/golf nexthole`).
- Keep the M6 contracts intact: server-owned round/score/ball/shot/penalty/completion state, no forced turns, independent scoring, barrier-driven advancement, disconnect/reconnect suspension, owner-only travel, per-player HUD snapshots, and the preserved one-player fast path.
- Revisit Hole 3 length and Driver/Fairway Wood rollout (already flagged in the M5 closeout) and any club-tune findings from playtesting.