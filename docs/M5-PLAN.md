# M5 — Single-Player Course Experience: Bounded Plan

**Status:** Complete. See `docs/M5-CLOSEOUT.md`. The product roadmap revision lives in `docs/MILESTONES.md`.

This is a **bounded, risk-first** plan for M5. It deliberately does not turn the milestone into an open-ended "perfect single-player forever" effort. Slice scope and verification evidence must stay clear; slices may be combined only when their risks and proof remain unambiguous.

## Purpose

Prove the full-sized, single-player golf course loop — practice range, exactly three authored holes ordered as Hole 1 par 4, Hole 2 par 3, and Hole 3 par 5, sequencing, transitions, scorecard, replay, and tuning — before multiplayer concurrency multiplies uncertain gameplay.

## Product constraints

- **Server authority.** Course state, ball position, shot results, and scoring remain server-owned. Clients send intent and display authoritative snapshots.
- **Client/server separation.** No `net.minecraft.client` imports under `src/main`; client-only code stays under `src/client`.
- **No general procedural generator.** Use deterministic, authored, versioned generation behind an explicit operator command.
- **Accepted automatic next-shot travel.** Playtesting found chat-driven travel too clumsy. After an active owned ball comes to a natural rest, the server automatically moves the player to a validated safe standing position near it. Penalty recovery, hole completion, missing/moving balls, and cross-dimension state never trigger travel.
- **No terrain mutation outside the declared footprint.** Exact bounds must be documented before any terrain is rewritten.
- **Approved fixed development campus.** The development layout uses the Docker world's known seed-specific ocean practice area and natural coastal forest with overall envelope `X[-640..448]`, `Y[32..192]`, `Z[-256..640]`. Generation mutates only explicitly declared authored subregions inside that envelope, not the entire envelope. The scored holes preserve natural trees, slopes, caves, shorelines, and water except for reviewed tee/green overlays and targeted legacy-flat cleanup. `/golf dev preparecourse` must preflight the target and reject unsafe/non-development terrain. Loom Singleplayer testing uses the documented Docker world seed `-1928790872702396508`.
- **Preserve M4.5 contracts.** Start/restart/abandon/status/Pick Up remain safe; practice balls are player-owned and only allowed outside a hole attempt; operator `/golf spawn` remains an unowned debug tool; HUD stays driven by typed server snapshots; accepted cup speed, Putter tuning, penalties, stroke cap, and camera restoration are preserved.

## Bounded slices

The slices below are ordered risk-first. Each defines its own goal, deliverables, verification, and exit criteria. Slices S1 and S2 carry the highest structural risk and are done first.

### S1 — Course domain, configuration, sequencing, and scorecard contracts

- **Goal:** Define and test the Minecraft-free course model, per-hole state, cumulative scorecard, and transition rules before any world generation.
- **Deliverables:**
  - `CourseDefinition` (id, name, dimension, ordered holes) and per-hole metadata (id, number, par, tee, cup, boundary, layout version).
  - Course-level state: current hole, per-hole score, cumulative score, completion, replay/reset.
  - Deterministic sequencing: start course, advance only after the current hole is terminal, next-hole transition, final completion.
  - Pure-domain unit tests for scoring, cumulative scorecard, sequencing, and invalid transitions.
- **Verification:** `./gradlew test`
- **Exit criteria:** Course and scorecard contracts are expressed as plain Java value types with focused tests; no world/entity dependencies leak in.

### S2 — Versioned bounded generation framework and outside-footprint safety proof

- **Goal:** Build the deterministic, versioned, bounded generation framework with an explicit operator action and an independent outside-footprint safety proof.
- **Deliverables:**
  - An explicit operator command (e.g. `/golf dev preparecourse`) that builds or restores the course.
  - Versioned layout identity; idempotent rebuild; clear reset behavior; actionable failure messages.
  - Declarative/reviewable layout definitions rather than thousands of opaque ad hoc block commands.
  - Documented course bounds and a test proving no changes occur outside the declared footprint.
  - Compatibility with both `runClient` Singleplayer and the Docker dev server.
- **Verification:** `./gradlew test`
- **Exit criteria:** Generation is deterministic, idempotent, versioned, bounded, and verified to preserve terrain outside its footprint.

#### Approved authored subregions

Layout `minecraft_golf:m5_ocean_campus` (currently version 11) may mutate only operations wholly
contained by these inclusive subregions. The command requires development seed
`-1928790872702396508`; the generator then preflights every planned block before its
first write and rejects stateful blocks (containers, signs, and other block entities).

| Region | X | Y | Z |
|---|---:|---:|---:|
| Practice range | `[-304..-80]` | `[62..96]` | `[-224..32]` |
| Legacy flat Hole 1 cleanup | `[-48..208]` | `[62..96]` | `[-224..-96]` |
| Legacy flat Hole 2 cleanup | `[-48..160]` | `[62..96]` | `[-64..64]` |
| Natural Hole 1 — par 4 | `[-384..-176]` | `[48..128]` | `[384..528]` |
| Natural Hole 2 — par 3 | `[-384..-256]` | `[48..128]` | `[384..464]` |
| Natural Hole 3 — par 5 | `[-400..-176]` | `[32..128]` | `[384..528]` |

These are permissions for reviewed authored operations, not instructions to rewrite
the full boxes. Unlisted space in the overall campus envelope remains untouched.

### S3 — Generated practice range

- **Goal:** Generate one bounded practice range with useful test areas; it is not part of the scored round and remains available outside an active course attempt.
- **Deliverables:** Tee/driver lane, distance markers/targets, putting green, short-game/wedge target, fairway/rough contrast, bunker/sand, water recovery, and ice/slime or other Minecraft-native surface lanes.
- **Verification:** `./gradlew test`; manual practice-range validation via `runClient`.
- **Exit criteria:** A player can practice all club types on the range with no active course attempt.

### S4 — Generated Hole 1 (par 4)

- **Goal:** Generate the authored opening par 4 hole.
- **Deliverables:** Driver tee shot, fairway positioning, approach shot, bunker and/or water risk, and ordinary scoring flow.
- **Verification:** `./gradlew test`; manual completion of Hole 1.
- **Exit criteria:** The par 4 validates driver, approach, and scoring flow with meaningful risk.

### S5 — Generated Hole 2 (par 3)

- **Goal:** Generate the authored par 3 hole.
- **Deliverables:** Short iron/wedge accuracy, approach control, putting, and a concise but meaningful hazard decision.
- **Verification:** `./gradlew test`; manual completion of Hole 2.
- **Exit criteria:** The par 3 is completable with a clear hazard decision and validates short-game clubs.

### S6 — Generated Hole 3 (par 5)

- **Goal:** Generate the authored par 5 hole.
- **Deliverables:** Realistic longer-shot strategy, multiple-shot pacing, elevation or route choice, Minecraft-native terrain/surface interaction, and no excessive empty travel.
- **Verification:** `./gradlew test`; manual completion of Hole 3.
- **Exit criteria:** The par 5 validates the full club set and feels meaningfully different from the par 3 and par 4.

### S7 — Single-player transitions, HUD, replay, and recovery

- **Goal:** Wire course sequencing into the server lifecycle and client HUD, with replay/reset and safe recovery.
- **Deliverables:** Start course, current-hole state, a server-authoritative `/golf nexthole` action with a clickable completion prompt, next-tee transition, three-hole cumulative scorecard, final completion, replay/reset without stale balls or scores, safe recovery from missing balls or interrupted state, and clear HUD state throughout the round.
- **Verification:** `./gradlew clean test build`; Loom client HUD/resource/camera checks.
- **Exit criteria:** A player can play all three holes back-to-back, see accurate per-hole and cumulative score, and replay without stale state.

### S8 — Physics, club, and travel tuning across full-sized holes

- **Goal:** Tune club distances, physics, camera, and pacing across realistic hole lengths.
- **Deliverables:** Club-distance and travel-pacing findings; automatic server-authoritative safe travel after natural ball rest; camera restoration on short and long shots; penalty/Pick Up/cap/restart/abandon/missing-ball recovery across the course.
- **Verification:** Manual `runClient` gameplay; Docker integration; record findings.
- **Exit criteria:** Full-sized holes feel playable and readable; automatic next-shot travel improves pacing without triggering for penalties, completion, or invalid ball state.
- **Recorded playtest finding:** Non-putter shots currently fly too low and roll too far after landing, especially Driver, Fairway Wood, and Long Iron. In S8, raise every non-Putter trajectory while preserving full-power carry distance, reduce post-landing rollout, and preserve the accepted Putter feel and distinct putting behavior.
- **Accepted tuning baseline:** Full-power non-Putter range targets now measure carry at first landing (150/125/100/85/60/42 blocks), use progressively higher 14–24 block apex targets, and carry a server-selected lofted-shot landing profile that limits horizontal landing energy and rollout without changing global `NORMAL`/green friction. Putter retains the standard profile and its existing surface-driven roll.
- **Manual acceptance finding:** All clubs feel substantially better, with readable higher flights and controlled rollout; the player completed all three holes successfully. The Hole 3 par 5 currently plays short—a well-hit Driver left roughly a half-Wedge approach and enabled an eagle—but its route and overall playability are accepted for M5. Defer further length adjustment and slightly greater Driver/Fairway Wood rollout relative to irons to later playtest tuning.

### S9 — Loom, Docker, documentation, and manual acceptance

- **Goal:** Full verification ladder and final acceptance.
- **Deliverables:** `./gradlew test`, `./gradlew clean test build`, no `net.minecraft.client` imports under `src/main`, Loom dedicated-server boot/registration logs, Loom client startup/HUD/resource/camera checks, Docker JAR identity and healthy server, repeated course preparation proving idempotence, Docker daytime and paused time/weather verification, and documentation updates (`dev-server/README.md`, `README.md`, `MILESTONES.md`).
- **Verification:** Full ladder plus manual completion of Hole 1 par 4, Hole 2 par 3, Hole 3 par 5, scorecard, replay, and recovery.
- **Exit criteria:** All M5 exit criteria pass and the experience is completable using documented controls without developer intervention.

## Travel-to-ball evaluation (check-in)

Full-course playtesting established that chat-driven travel interrupts shot flow. The accepted M5 behavior is **automatic, server-authoritative next-shot travel**.

- Trigger only when physics naturally brings the active, player-owned ball to rest.
- Search for a safe supported standing position near the authoritative ball before moving the player.
- Never trigger from tee placement, penalty recovery, hole completion, missing/moving balls, or cross-dimension state.
- If no safe destination exists, leave the player in place and provide actionable feedback.
- Preserve ordinary Minecraft traversal outside active shot-to-shot course play.

## Check-in points

Pause and consult the user when:

- large-course bounds or terrain mutation require a material user decision,
- travel-to-ball UX requires acceptance,
- architecture must deviate from `docs/ARCHITECTURE.md`,
- subjective gameplay acceptance is required,
- or a GitHub-facing action needs approval.

## Non-goals (do not build in M5)

Multiplayer players, Ready Golf concurrency, shared multiplayer round state, traditional turns, scramble or other game modes, matchmaking, public-server infrastructure, arbitrary course generation, course-authoring UI, persistent global statistics, a large course library, wind, and cinematic camera.

## Preserved M4.5 contracts

Gameplay state and scoring remain server-owned; clients send intent and display authoritative snapshots; no `net.minecraft.client` imports under `src/main`; start/restart/abandon/status/Pick Up remain safe; player-owned practice balls are allowed only outside a hole attempt; starting/restarting clears only that player's prior balls; operator `/golf spawn` remains an unowned debugging tool; missing-ball recovery remains actionable; the HUD remains driven by typed server snapshots; existing development-hole preparation remains explicit and idempotent; accepted cup speed, Putter tuning, penalties, stroke cap, and camera restoration behavior are preserved. M5's accepted automatic next-shot travel supersedes only the prior no-automatic-teleport constraint.

## GSD note

Product M4.5 is complete (see `docs/M4.5-CLOSEOUT.md`). The canonical GSD milestone M002 remains pending because implementation occurred outside `/gsd auto`; do not force-close M002 with direct completion tools. A new GSD milestone for M5 must be created via `gsd_milestone_generate_id` (never invent or hardcode an ID). Keep task `verify` fields as single bare executable commands; put explanatory prose in task descriptions.
