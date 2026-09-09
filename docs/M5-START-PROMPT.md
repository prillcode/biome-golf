Continue development of Minecraft Golf by revising the roadmap and planning the next product milestone.

Repository state:
- Path: /home/prill/dev/minecraft-golf
- Current branch: main
- Expected HEAD: a55d0b2
- main should be synchronized with origin/main
- M0–M4.5 are complete
- M4.5 closeout: docs/M4.5-CLOSEOUT.md
- .claude/ and docs/M4.5-START-PROMPT.md are pre-existing untracked content; do not modify, stage, or commit them
- A prepared Docker development server may already be running on port 25565; inspect it before disrupting it
- Do not push, merge, create a PR, or take other GitHub-facing actions without explicit confirmation

## Approved roadmap change

Back-burner multiplayer until the complete single-player course experience is proven.

Replace the current product M5 with:

**M5 — Single-Player Course Experience**

Move **Multiplayer Ready Golf** to M6.

The new M5 must deliver:

- one deterministic generated practice range
- exactly three generated/authored golf holes:
  - one par 3
  - one par 4
  - one par 5
- single-player course sequencing
- between-hole transitions
- final three-hole scorecard
- replay/reset behavior
- full gameplay tuning and acceptance across realistic hole lengths
- evaluation of optional on-demand travel-to-ball behavior

The purpose is to prove full-sized golf gameplay, club distances, hazards, putting, camera behavior, traversal, scoring clarity, and round pacing before adding multiplayer concurrency.

This direction is recorded as GSD decision D018.

## Read before changing anything

1. AGENTS.md
2. docs/PRD.md
3. docs/ARCHITECTURE.md
4. docs/MILESTONES.md
5. docs/M4.5-CLOSEOUT.md
6. README.md
7. dev-server/README.md
8. Relevant M4 and M4.5 implementation files
9. Existing course, hole, configuration, scoring, HUD, camera, and development-generation tests

## First actions

1. Verify main is at a55d0b2 and synchronized with origin/main.
2. Confirm the working tree is clean except for the expected untracked paths.
3. Inspect the Docker server and prepared development world without resetting it.
4. Create feature branch `m5-single-player-course` from main.
5. Review the current hole configuration, lifecycle, HUD, development-hole builder, Docker preparation script, and scoring contracts.
6. Update the product roadmap and README on the feature branch.
7. Produce a concise, bounded milestone plan.
8. Commit the roadmap/planning changes before beginning implementation.
9. Do not write gameplay implementation until the documentation and bounded plan are complete.

## Required roadmap edits

Update docs/MILESTONES.md:

- Mark M4.5 complete and preserve its closeout reference.
- Replace M5 “Multiplayer Ready Golf” with “Single-Player Course Experience.”
- Make M5 current/in progress.
- Move Multiplayer Ready Golf to M6.
- Make revised M6 depend on the new M5.
- Adapt M6 so Ready Golf is applied to the already-proven three-hole course.
- Preserve M7 as the later MVP hardening/family playtest milestone.
- Keep M8 and M9 deferred.
- Update milestone prerequisites, deliverables, verification, exit criteria, and cross-references consistently.
- Remove any duplicate three-hole-course responsibility left behind by the old M6.
- Do not silently renumber unrelated later milestones.

Update README.md:

- Set the current milestone to the revised M5.
- Link to docs/MILESTONES.md and docs/M4.5-CLOSEOUT.md as appropriate.

Preserve docs/M4.5-CLOSEOUT.md unchanged.

## M5 scope

### 1. Course domain and configuration

Implement a server-owned course model containing exactly three ordered holes.

Each hole needs:

- stable identifier and number
- par
- tee and cup
- playable boundary
- generated-layout identity/version
- transition metadata as needed

Add course-level state for:

- current hole
- per-hole score
- cumulative score
- course completion
- replay/reset

Keep domain behavior Minecraft-free where practical.

### 2. Deterministic practice range

Generate one bounded practice range containing useful test areas such as:

- tee/driver lane
- distance markers and targets
- putting green
- short-game or wedge target
- fairway/rough contrast
- bunker/sand
- water recovery
- ice/slime or other Minecraft-native surface lanes where useful

The practice range is not part of the scored three-hole round.

Practice remains available outside an active course attempt.

### 3. Three authored generated holes

Generate exactly three intentionally different holes:

#### Par 3

Focus on:

- short iron or wedge accuracy
- approach control
- putting
- a concise but meaningful hazard decision

#### Par 4

Focus on:

- driver tee shot
- fairway positioning
- approach shot
- bunker and/or water risk
- ordinary golf scoring flow

#### Par 5

Focus on:

- realistic longer-shot strategy
- multiple-shot pacing
- elevation or route choice
- Minecraft-native terrain/surface interaction
- avoiding excessive empty travel

The holes should validate the full club set and should feel meaningfully different.

### 4. Course progression

Implement:

- start course
- current-hole state
- advance only after the current hole is terminal
- next-tee transition
- three-hole cumulative scorecard
- final course completion
- replay/reset
- safe recovery from missing balls or interrupted state
- clear HUD state throughout the round

This milestone is single-player only, but state must remain server-authoritative and structurally suitable for later per-player multiplayer ownership.

### 5. Travel evaluation

Full-sized holes may make walking from the stationary player position to the resting ball tedious.

During M5, evaluate an explicit optional on-demand travel-to-ball interaction.

Constraints:

- do not automatically teleport after every shot
- preserve Minecraft traversal as a meaningful part of play
- keep the server authoritative
- require a deliberate player action
- test whether it improves pacing before accepting it permanently

If this requires a material UX decision, prepare options and ask the user rather than silently choosing.

## Generation constraints

Do not build a general procedural golf-course generator.

Use deterministic authored generation:

- explicit `/golf dev preparecourse` or an equivalently clear operator action
- no terrain generation during ordinary server startup
- bounded and documented footprint
- idempotent rebuild
- versioned layout
- clear reset behavior
- independent verification of key blocks and structures
- preservation of terrain outside the declared footprint
- actionable failure messages
- compatible with both runClient Singleplayer development and the Docker development server

Prefer declarative layouts, structure templates, or other reviewable deterministic definitions over thousands of opaque ad hoc commands.

The generated course may target a known disposable development-world ocean area, but exact large-course bounds must be documented before terrain mutation. Do not destructively rewrite arbitrary player terrain.

General course authoring, arbitrary procedural generation, biome adaptation, and player-facing course editors remain deferred to M9.

## M5 non-goals

Do not implement:

- multiplayer players
- Ready Golf concurrency
- shared multiplayer round state
- traditional turns
- scramble or other game modes
- matchmaking
- public-server infrastructure
- arbitrary course generation
- course-authoring UI
- persistent global statistics
- a large course library
- wind or cinematic camera
- automatic teleport-to-ball

## Preserve M4.5 contracts

- Gameplay state and scoring remain server-owned.
- Clients send intent and display authoritative snapshots.
- Client-only code remains under src/client.
- No net.minecraft.client imports are allowed under src/main.
- Start, restart, abandon, status, and Pick Up remain safe.
- Player-owned practice balls are allowed only outside a hole attempt.
- Starting/restarting clears only that player’s prior balls.
- Operator `/golf spawn` remains an unowned debugging tool.
- Missing-ball recovery remains actionable.
- The HUD remains driven by typed server snapshots.
- The existing development-hole preparation remains explicit and idempotent.
- Automatic teleport-to-ball remains out of scope unless replaced by a separately accepted on-demand interaction.
- Preserve accepted cup speed, Putter tuning, penalties, stroke cap, and camera restoration behavior.

## Recommended bounded slices

Plan risk-first. A reasonable starting structure is:

1. Course domain, configuration, sequencing, and scorecard contracts
2. Versioned bounded generation framework and outside-footprint safety proof
3. Generated practice range
4. Generated par 3
5. Generated par 4
6. Generated par 5
7. Single-player transitions, HUD, replay, and recovery
8. Physics/club/travel tuning across full-sized holes
9. Loom, Docker, documentation, and manual acceptance

Combine slices only when their risks and verification evidence remain clear. Do not turn the milestone into an open-ended “perfect single-player forever” effort.

## M5 exit criteria

M5 is complete only when one player can:

1. explicitly generate or restore the bounded development course,
2. use the practice range,
3. start the three-hole course,
4. complete the par 3,
5. transition to and complete the par 4,
6. transition to and complete the par 5,
7. receive an accurate per-hole and cumulative scorecard,
8. recover safely from expected lifecycle failures,
9. replay the course without stale balls or scores,
10. complete the experience using documented controls without developer intervention.

The generated layout must also prove:

- deterministic output
- idempotent rebuild
- documented version and bounds
- no changes outside the declared footprint
- valid tee/cup support
- intended surfaces and hazards
- safe Docker reset/rebuild behavior

## Verification expectations

- Focused unit tests for course state, sequencing, scoring, and layout calculations
- Tests for malformed course configuration and invalid transitions
- Tests for deterministic generation plans
- Tests proving outside-footprint preservation where practical
- `./gradlew test`
- `./gradlew clean test build`
- Confirm no `net.minecraft.client` imports under `src/main`
- Loom dedicated-server boot and registration logs
- Loom client startup, HUD, resource, and camera checks
- Docker JAR identity and healthy server
- Repeated course preparation proving idempotence
- Docker daytime and paused time/weather verification
- Manual practice-range validation
- Manual completion of par 3, par 4, and par 5
- Scorecard and replay validation
- Club-distance and travel-pacing findings
- Camera restoration on short and long shots
- Penalty, Pick Up, cap, restart, abandon, and missing-ball recovery across the course

## Development workflow

Use the established hybrid workflow:

- automated tests for domain and generation contracts
- runClient Singleplayer for rapid client, HUD, input, rendering, and camera iteration
- authenticated normal Minecraft client against localhost:25565 for Docker integration and final acceptance

Inspect Docker before disrupting it. Never weaken the tracked Docker server’s online-mode authentication.

## GSD caveat

- Product-facing M4.5 is complete in docs/M4.5-CLOSEOUT.md and Git history.
- Canonical GSD milestone M002 remains pending because implementation occurred outside `/gsd auto` and no Attempts were issued.
- Do not force-close M002 with direct completion tools.
- Treat the repository closeout as the product authority while handling the stale canonical state explicitly.
- Before creating the new GSD milestone, call `gsd_milestone_generate_id`; never invent or hardcode an ID.
- Keep task `verify` fields as single bare executable commands. Put explanatory prose in task descriptions, not verification commands.

## Check-in policy

Proceed autonomously through documentation, planning, tests, and implementation unless:

- large-course bounds or terrain mutation require a material user decision,
- travel-to-ball UX requires acceptance,
- architecture must deviate,
- subjective gameplay acceptance is required,
- or a GitHub-facing action needs approval.

Do not begin implementation until the roadmap update and bounded M5 plan are complete.
