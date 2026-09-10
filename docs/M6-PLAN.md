# M6 — Multiplayer Ready Golf: Bounded Risk-First Plan

**Status:** Planned. Implementation has not started. Baseline: `main` at `c60b028` with 172 passing tests and M5 accepted.

This plan adds 1–4 player Ready Golf to the proven M5 course without redesigning the course, physics, swing, or single-player flow. Work is ordered by multiplayer corruption risk: first prove two-player ownership and state isolation, then prove the all-participants-terminal barrier, and only then expand into concurrency, reconnect behavior, coordinated travel, and four-player acceptance.

## Purpose

Allow up to four golfers to play the existing three-hole course concurrently. Every golfer keeps an independently owned ball, hole state, HUD snapshot, and score while the server owns the shared participant roster, current hole, advancement barrier, and final results.

## Existing seams and required change

- `HoleLifecycle` already keys `PlayerHoleSession` by player UUID and enforces assigned-ball ownership.
- `GolfBallEntity`, `ShotService`, penalties, cup detection, and automatic next-shot travel already route authoritative changes through the ball owner.
- `HoleStatePayload` is already sent to one player and can remain the source of that golfer's HUD values.
- `PlayerCourseState` already models one golfer's ordered scores and replay reset.
- The unsafe M6 seam is `ActiveHoleService.courseStates`: each player currently starts and advances independently. M6 must put those per-player states under one server-owned Ready Golf round whose current hole advances atomically for the participating group.

Do not create a second physics path, client-side score model, or generic multiplayer framework. Refactor only enough of `ActiveHoleService` to delegate roster and barrier decisions to a Minecraft-free round domain object.

## Locked gameplay contracts

### Authority and ownership

- The server owns round membership, connection/withdrawal status, current hole, every `PlayerCourseState`, ball assignment, strokes, penalties, completion, transitions, and scorecards.
- Clients continue to submit bounded shot intent only. Existing ownership and assigned-ball validation must reject attempts against another golfer's ball, including overlapping tee balls.
- Normal Minecraft entity synchronization remains the ball-motion transport. Do not add custom continuous-position packets.
- Every cleanup, restart, penalty, cup, and travel operation is scoped by player UUID plus assigned ball UUID. No operation may remove, move, score, or replace another golfer's ball.

### Round and participant lifecycle

- M6 supports one active round on the one configured M5 course. Multiple courses, matchmaking, and parallel public-server lobbies remain out of scope.
- Add the bounded multiplayer flow `/golf round create`, `/golf round join`, `/golf round start`, `/golf round leave`, and `/golf round status`.
- The lobby creator is its coordinator and is the only player who can start it. If the coordinator leaves or disconnects before play, coordination passes to the next golfer in stable join order; an empty lobby is removed.
- The participant roster is open only in the lobby and fixed once play starts. Late joiners may observe/practice only after the active round ends; they do not enter a hole in progress.
- Preserve `/golf hole start` as the backward-compatible one-player fast path when no multiplayer lobby/round exists. Its M5 start, three-hole progression, finalization, and replay behavior must remain unchanged.
- One golfer leaving or abandoning withdraws only that golfer. The round ends only when no active participants remain or after normal course completion.
- A completed multiplayer round may be replaced by a new lobby. It does not silently admit players or replay itself; the preserved one-player `/golf hole restart` replay remains the solo fast path.

### Ready Golf and advancement

- There is no turn owner and no shot-order check. Any non-terminal participant may take a legal shot whenever their own assigned ball is resting.
- Hole terminal states are hole-out, Pick Up, or stroke cap.
- For Holes 1 and 2, `/golf nexthole` (from any active participant) succeeds only when every currently active participant is terminal. It then transitions all active participants exactly once, replaces only their assigned balls, teleports each to the authored tee transition, and sends each an individual authoritative HUD snapshot.
- A terminal golfer sees a waiting state/message while another active golfer is still playing; the waiting golfer cannot shoot again or force early advancement.
- Hole 3 finalizes the shared round automatically when the last active participant becomes terminal. Every active participant receives multiplayer final results, while each HUD remains based on that player's own score.
- Repeated or near-simultaneous advancement requests are idempotent on the server thread: one transition occurs and stale requests fail safely.

### Disconnect and reconnect

- Disconnect marks an in-progress participant suspended and removes that participant from the advancement barrier; it never mutates another player's state.
- A suspended golfer who reconnects before the group advances resumes the same assigned ball, hole state, score, and HUD after server-side entity validation.
- If the remaining golfers advance while that golfer is offline, the suspended golfer is withdrawn from that round before transition. Their stale ball is removed, they cannot rejoin the round in progress, and they return to practice on reconnect. This bounded rule prevents deadlocks without inventing score penalties or cross-hole catch-up.
- If all golfers disconnect, the in-memory round remains suspended and does not advance by itself; a reconnect can resume it. Server-restart persistence remains out of scope.

### Single-player compatibility

- Existing practice behavior, course generation, Hole 1 replay reset, zero-stroke HUD semantics, automatic Hole 3 finalization, and final scorecard values must remain unchanged for one golfer.
- Existing `/golf hole restart` remains player-scoped recovery. During multiplayer it may replace/reset only the caller's current-hole state and ball; it must not rewind another participant or the shared hole index.
- Existing `/golf hole abandon` becomes participant withdrawal during a multiplayer round and retains the M5 return-to-practice behavior for a solo round.

## Bounded slices

Each slice must pass its verification before the next begins. Stop and document any required deviation from `docs/ARCHITECTURE.md`.

### S1 — Pure Ready Golf round and barrier domain

**Goal:** Express shared membership, per-player progress, terminal checks, and atomic advancement without Minecraft classes.

**Deliverables:**

- A focused round state/policy type under the round/course domain containing a round identity, course, phase (`LOBBY`, `PLAYING`, `COMPLETE`), participant records, and the shared current-hole index.
- Participant status sufficient for active, suspended, and withdrawn behavior, with one `PlayerCourseState` per playing golfer.
- Operations and invariants for create/join/start, player-state update, restart, disconnect/reconnect, withdraw, all-active-terminal, atomic next-hole advance, final-hole completion, and replay/cleanup where needed by the preserved solo flow.
- No hard-coded assumption that map iteration order is score order; use stable join order or another deterministic presentation order.
- Focused unit tests proving:
  - two players begin on Hole 1 with distinct independent state;
  - one player's shot, penalty, restart, Pick Up, or hole-out cannot alter the other player's state;
  - one terminal player cannot advance while another remains active;
  - all terminal players advance together exactly once;
  - the last Hole 3 terminal update completes the round and preserves all scorecards;
  - disconnect, reconnect-before-advance, withdraw-on-advance, all-disconnected suspension, and last-participant withdrawal follow the locked rules;
  - invalid/duplicate transitions and late joins fail without mutation.

**Verification:** `./gradlew test`

**Exit criteria:** The highest-risk Ready Golf invariants are proven in plain Java before Fabric integration changes.

### S2 — Two-player server integration and ownership isolation

**Goal:** Replace independent course progression in `ActiveHoleService` with the shared round policy and prove two-player isolation end to end.

**Deliverables:**

- Lobby/start command wiring and the backward-compatible solo `/golf hole start` path.
- Transactional round start: preflight the course/world and all online participants before committing; clean up newly created balls if any participant setup fails.
- One assigned, player-owned ball and one `PlayerHoleSession` per participant, with targeted spawning, lookup, restart, abandon, and cleanup.
- Owner-routed shot acceptance, cup completion, penalties, Pick Up, missing-ball state, score updates, and per-player HUD snapshots.
- Clear server logs for round create/start, join/leave, suspension/resume/withdrawal, hole transition, rejected early advancement, and completion; no per-tick multiplayer logging.
- Automated tests at the pure seams plus the strongest practical service/integration coverage available without fabricating a second game engine.

**Verification:** `./gradlew test`, then `./gradlew build`

**Exit criteria:** Two connected golfers can start together and manipulating either golfer's state or ball leaves the other golfer unchanged. The one-player start path still behaves as M5.

### S3 — Barrier-driven hole transitions and multiplayer results

**Goal:** Coordinate all participants through the same three holes without early or duplicate advancement.

**Deliverables:**

- Barrier evaluation after every terminal event: hole-out, Pick Up, stroke-cap shot/penalty, withdrawal, and disconnect.
- Waiting feedback that reports completed/active participant counts without exposing authority to the client.
- Atomic Holes 1–2 transitions initiated by the existing next-hole action only after the barrier opens.
- Automatic shared Hole 3 finalization when the last active golfer becomes terminal.
- Server-generated per-hole and final multiplayer result tables in deterministic participant order, while preserving each golfer's existing personal HUD totals.
- Transition rollback or fail-safe behavior that never leaves participants split across holes if ball creation, dimension validation, or teleport preparation fails.

**Verification:** `./gradlew test`, `./gradlew clean test build`

**Exit criteria:** Two golfers with different stroke counts remain on one shared hole, cannot advance early, transition together once, and receive correct independent and shared results through Hole 3.

### S4 — Simultaneous shots, travel, HUD, and visibility isolation

**Goal:** Prove that ordinary Ready Golf concurrency does not cross-wire presentation or movement.

**Deliverables:**

- No turn serialization in `ShotService`; independent legal requests received on adjacent server ticks both launch and score their respective assigned balls.
- Automatic travel remains owner-only and triggers only from that owner's naturally resting active ball. Another ball resting, being penalized, disappearing, or holing out cannot move the wrong player.
- Each client receives only its own hole/HUD snapshot and camera follows only its selected owned ball.
- All clients observe all loaded golf balls through normal entity synchronization.
- If necessary for understandable testing, add only minimal owner identification using already synchronized owner data; do not expand into the deferred cosmetic ball system.
- Repair the Minecraft 26.2 item-definition/resource path responsible for the magenta/black club artifacts. A vanilla tool model may be used briefly as a diagnostic fallback, but is not the intended presentation.
- Replace the one shared placeholder with seven distinct Minecraft-style 2D club sprites and verify inventory, first-person, third-person, and dropped-item presentation. Custom 3D Blockbench club models remain deferred unless later playtesting justifies them.

**Verification:** two Loom clients or one Loom client plus another compatible client on a dedicated server; execute overlapping long shots, short shots, penalties, rests, and camera returns.

**Exit criteria:** Concurrent play is visibly shared but state, HUD, camera, and travel effects remain isolated to each owner.

### S5 — Disconnect, reconnect, and recovery integration

**Goal:** Ensure network churn cannot deadlock or corrupt a round.

**Deliverables:**

- Server join/disconnect hooks that apply the locked suspension policy on the server thread.
- Reconnect-before-advance validation of the assigned entity; send ACTIVE, COMPLETE, or MISSING_BALL snapshots as appropriate.
- Withdrawal and stale-ball cleanup when the group advances without an offline golfer.
- Safe behavior for disconnect while a ball is moving, while waiting terminal, while all players are offline, and during final-hole completion.
- Missing-ball restart and individual restart remain player-scoped after reconnect.

**Verification:** two-client dedicated-server matrix covering disconnect/reconnect before advancement, advancement while one player is offline, reconnect after withdrawal, disconnect during flight, and all-clients-disconnected recovery.

**Exit criteria:** No disconnected golfer blocks active golfers, a timely reconnect restores authoritative state, and a stale reconnect cannot corrupt the advanced round.

### S6 — Full verification and four-player LAN acceptance

**Goal:** Close M6 only with automated, dedicated-server, and real multiplayer evidence.

**Automated and static checks:**

- `./gradlew test`
- `./gradlew clean test build`
- confirm no `net.minecraft.client` imports under `src/main/java`
- `git diff --check`
- record final test count and focused Ready Golf coverage

**Loom and Docker checks:**

- Loom client startup plus HUD, camera, resource/model, and solo M5 regression checks.
- Loom dedicated server reaches `Done` with round, shot, HUD, command, join, and disconnect handlers registered.
- Run `./scripts/dev-server-sync.sh` and `./scripts/dev-server-up.sh`; verify container health and staged/container JAR SHA-256 identity.
- Re-run `/golf dev preparecourse` until it reports zero changes; M6 must not alter M5 layout v11 or its seed/bounds.

**Two-player manual matrix:**

1. Create/join/start one shared round; verify two owned balls and player-specific HUD values.
2. Take overlapping shots with different clubs; verify both launches, cameras, rests, and automatic travel.
3. Give players different stroke/penalty totals; verify independent scoring.
4. Hole out/Pick Up one player first; verify waiting behavior and rejected early `/golf nexthole`.
5. Complete the other player; verify one coordinated transition and correct hole results.
6. Exercise individual restart, missing-ball recovery, and withdrawal without touching the other player.
7. Execute the full disconnect/reconnect matrix from S5.
8. Complete Hole 3; verify automatic shared finalization and accurate per-player three-hole scorecards.
9. Re-run the original one-player course and replay flow; verify Hole 1 reset, zero strokes, and no stale multiplayer state.

**Four-player LAN acceptance:**

- Four golfers join one lobby and complete all three proven holes under Ready Golf.
- Include at least one period with multiple balls moving, one penalty, one Pick Up or cap completion, staggered hole completion, coordinated next-hole travel, and final score comparison.
- Confirm no forced turns, early advancement, ownership leak, HUD/score cross-talk, wrong-player travel, stale balls, or dedicated-server errors.

**Four-player acceptance decision (agreed with user):** the physical four-golfer LAN
acceptance is waived. Four-player correctness is evidenced instead by the existing
four-player domain unit tests in `ReadyGolfRoundTest` plus code review of the
server integration, with the two-player manual matrix as on-wire proof. A true
four-player session may be revisited during M7 family playtesting.

**Exit criteria:** Every M6 verification item in `docs/MILESTONES.md` passes and up to four golfers complete the course without state corruption or forced turn-taking.

## Risk register and stop conditions

| Risk | Mitigation / proof |
|---|---|
| Per-player M5 states drift onto different holes | One shared hole index and atomic barrier transition in the pure round domain. |
| Cleanup or restart deletes another golfer's ball | Require owner UUID and assigned ball UUID; two-player isolation tests and manual checks. |
| One offline golfer deadlocks the round | Suspend immediately; withdraw only if the group advances while they remain offline. |
| Partial multi-player setup/transition splits state | Preflight first, commit once, clean up spawned entities on failure; do not mutate shared index partially. |
| Duplicate next-hole requests transition twice | Server-thread idempotence and stale-phase/index validation. |
| HUD or score leaks between clients | Target snapshots per `ServerPlayer`; different-score two-client test. |
| Automatic travel moves the wrong golfer | Resolve player from authoritative ball owner and verify active assigned ball before teleport. |
| Multiplayer work regresses the accepted solo loop | Keep `/golf hole start` fast path and repeat full M5 replay acceptance. |

Stop and consult the user before implementation proceeds if:

- meeting the barrier or disconnect contracts requires changing server authority;
- a shared transition cannot be made fail-safe without changing the authored M5 course;
- Fabric APIs cannot provide required connection lifecycle handling without a new Mixin;
- a proposed fix expands into persistence, matchmaking, multiple simultaneous rounds, cosmetic systems, or M7 tuning;
- any verification rung fails and cannot be repaired within the current slice.

## Non-goals

Traditional turns, scramble or teams, matchmaking, invitations/permissions beyond the small lobby commands, public-server infrastructure, multiple simultaneous courses/rounds, server-restart round persistence, global statistics/leaderboards, physics or club retuning, course/layout changes, chip shots, wind, cinematic camera, and a general cosmetic ball-identification system.

## Completion record to capture

At closeout, record the final commit, test count, Ready Golf domain decisions, command flow, disconnect semantics, Loom/Docker/JAR evidence, two-player matrix results, four-player LAN findings, solo M5 regression results, and any M7-only usability or tuning observations.

## S6 verification record (2026-09-10)

**Automated and static checks — PASS**

- `./gradlew test`: 187 tests, 0 failures, 0 errors (15 in `ReadyGolfRoundTest`).
- `./gradlew clean build`: SUCCESS.
- `net.minecraft.client` imports under `src/main/java`: 0.
- `git diff --check`: clean.
- S6 regression fix `68447e7`: solo `/golf hole abandon` left the golfer `WITHDRAWN` inside a `COMPLETE` round, so `/golf hole start` answered "course recovery needed" and `/golf hole restart` threw "round must be PLAYING but is COMPLETE". `ActiveHoleService.courseState()` now exposes progress only for `ACTIVE` participants, restoring the M5 return-to-practice solo behavior.

**Loom and Docker checks — PASS**

- Loom client (`runClient`): `Minecraft Golf client initialized`, resource reload includes `minecraft_golf`, `Sound engine started` (title screen) and rendering continued. Only benign dev-environment errors (dev-account authlib 401, narrator `flite` missing, Realms auth, GLFW `X11: Standard cursor shape unavailable`).
- Loom dedicated server: reached `Done` during S5 (initial attempt crashed on the Docker 25565 port conflict; rerun on 25566 succeeded).
- Docker server (post-fix JAR): booted `Done (2.113s)!`; container health `healthy`; staged (`dev-server/mods`) and container (`/mods`) JAR SHA-256 identical: `efc2bde808bf81687f4902c5fb3bf4d06d0fb92016e8f339f3d5d8345468e5b1`. Laptop client JAR and mini-PC copy carry the same SHA.
- `/golf dev preparecourse` idempotence: `minecraft_golf:m5_ocean_campus v11`, envelope `X[-640..448] Y[32..192] Z[-256..640]`, seed check passed. First run repaired 7 drifted blocks (gameplay drift, self-healing as designed); second run reported **0 changed**.

**Solo M5 regression — user-driven, in progress**

- Tests 1–2 (hole start, restart) passed on the S5 JAR. Test 3 (abandon → start) was blocked by the stale-state bug above; fix deployed, re-test pending.

**Two-player manual matrix (items 1–9)** — deferred until a second golfer is available; to be run by the user.

**Four-player LAN acceptance** — waived by user decision (see above); covered by four-player domain unit tests plus server integration code review.
