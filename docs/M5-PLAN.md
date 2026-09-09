# M5 — Single-Player Course Experience: Bounded Plan

**Status:** Approved planning document (GSD decision D018). The product roadmap revision lives in `docs/MILESTONES.md`.

This is a **bounded, risk-first** plan for M5. It deliberately does not turn the milestone into an open-ended "perfect single-player forever" effort. Slice scope and verification evidence must stay clear; slices may be combined only when their risks and proof remain unambiguous.

## Purpose

Prove the full-sized, single-player golf course loop — practice range, exactly three authored holes ordered as Hole 1 par 4, Hole 2 par 3, and Hole 3 par 5, sequencing, transitions, scorecard, replay, and tuning — before multiplayer concurrency multiplies uncertain gameplay.

## Product constraints

- **Server authority.** Course state, ball position, shot results, and scoring remain server-owned. Clients send intent and display authoritative snapshots.
- **Client/server separation.** No `net.minecraft.client` imports under `src/main`; client-only code stays under `src/client`.
- **No general procedural generator.** Use deterministic, authored, versioned generation behind an explicit operator command.
- **No automatic teleport-to-ball.** Only a separately accepted on-demand interaction may replace it.
- **No terrain mutation outside the declared footprint.** Exact bounds must be documented before any terrain is rewritten.
- **Approved fixed ocean campus.** The development layout uses the Docker world's known disposable ocean area with overall envelope `X[-320..448]`, `Y[48..112]`, `Z[-256..512]` (769 × 65 × 769 blocks). This includes tuning headroom beyond the current 150-block Driver carry and avoids cramping the par 5. Generation mutates only explicitly declared authored subregions inside that envelope, not the entire envelope. `/golf dev preparecourse` must preflight the target and reject unsafe/non-development terrain. Loom Singleplayer testing uses the documented Docker world seed `-1928790872702396508`.
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
- **Deliverables:** Start course, current-hole state, next-tee transition, three-hole cumulative scorecard, final completion, replay/reset without stale balls or scores, safe recovery from missing balls or interrupted state, and clear HUD state throughout the round.
- **Verification:** `./gradlew clean test build`; Loom client HUD/resource/camera checks.
- **Exit criteria:** A player can play all three holes back-to-back, see accurate per-hole and cumulative score, and replay without stale state.

### S8 — Physics, club, and travel tuning across full-sized holes

- **Goal:** Tune club distances, physics, camera, and pacing across realistic hole lengths.
- **Deliverables:** Club-distance and travel-pacing findings; camera restoration on short and long shots; penalty/Pick Up/cap/restart/abandon/missing-ball recovery across the course.
- **Verification:** Manual `runClient` gameplay; Docker integration; record findings.
- **Exit criteria:** Full-sized holes feel playable and readable; walking travel is acceptable or an on-demand travel-to-ball interaction is explicitly evaluated and accepted/rejected.

### S9 — Loom, Docker, documentation, and manual acceptance

- **Goal:** Full verification ladder and final acceptance.
- **Deliverables:** `./gradlew test`, `./gradlew clean test build`, no `net.minecraft.client` imports under `src/main`, Loom dedicated-server boot/registration logs, Loom client startup/HUD/resource/camera checks, Docker JAR identity and healthy server, repeated course preparation proving idempotence, Docker daytime and paused time/weather verification, and documentation updates (`dev-server/README.md`, `README.md`, `MILESTONES.md`).
- **Verification:** Full ladder plus manual completion of Hole 1 par 4, Hole 2 par 3, Hole 3 par 5, scorecard, replay, and recovery.
- **Exit criteria:** All M5 exit criteria pass and the experience is completable using documented controls without developer intervention.

## Travel-to-ball evaluation (check-in)

Travel across full-sized holes may be tedious. This is an **explicit, on-demand, server-authoritative** evaluation — never automatic teleport-after-every-shot.

- Requires a deliberate player action.
- Preserves Minecraft traversal as a meaningful part of play.
- If it requires a material UX decision, prepare options and ask the user rather than silently choosing.

## Check-in points

Pause and consult the user when:

- large-course bounds or terrain mutation require a material user decision,
- travel-to-ball UX requires acceptance,
- architecture must deviate from `docs/ARCHITECTURE.md`,
- subjective gameplay acceptance is required,
- or a GitHub-facing action needs approval.

## Non-goals (do not build in M5)

Multiplayer players, Ready Golf concurrency, shared multiplayer round state, traditional turns, scramble or other game modes, matchmaking, public-server infrastructure, arbitrary course generation, course-authoring UI, persistent global statistics, a large course library, wind, cinematic camera, and automatic teleport-to-ball.

## Preserved M4.5 contracts

Gameplay state and scoring remain server-owned; clients send intent and display authoritative snapshots; no `net.minecraft.client` imports under `src/main`; start/restart/abandon/status/Pick Up remain safe; player-owned practice balls are allowed only outside a hole attempt; starting/restarting clears only that player's prior balls; operator `/golf spawn` remains an unowned debugging tool; missing-ball recovery remains actionable; the HUD remains driven by typed server snapshots; existing development-hole preparation remains explicit and idempotent; automatic teleport-to-ball remains out of scope unless replaced by a separately accepted on-demand interaction; accepted cup speed, Putter tuning, penalties, stroke cap, and camera restoration behavior are preserved.

## GSD note

Product M4.5 is complete (see `docs/M4.5-CLOSEOUT.md`). The canonical GSD milestone M002 remains pending because implementation occurred outside `/gsd auto`; do not force-close M002 with direct completion tools. A new GSD milestone for M5 must be created via `gsd_milestone_generate_id` (never invent or hardcode an ID). Keep task `verify` fields as single bare executable commands; put explanatory prose in task descriptions.
