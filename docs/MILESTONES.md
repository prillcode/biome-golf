# MILESTONES.md

## Purpose

This document defines the execution milestones for the Biome Golf mod.

It is intended to guide both human development and autonomous/agentic development sessions. Milestones should be completed sequentially unless a documented blocker requires a change in order.

The product requirements are defined in `docs/PRD.md`.

The technical architecture and engineering constraints are defined in `docs/ARCHITECTURE.md`.

This document defines **what should be built when**, how each phase should be verified, and what must remain out of scope until later milestones.

---

# Project Execution Rules

## Sequential Milestones

Milestones should be completed in order.

Do not begin implementation work for a later milestone until the current milestone's required verification and exit criteria have passed.

Small preparatory changes for a later milestone are acceptable only when they are necessary to complete the current milestone cleanly.

---

## Scope Discipline

Agents and developers should not add deferred features simply because they are easy, interesting, or adjacent to current work.

When a useful idea is discovered that is outside the current milestone:

1. document it,
2. add it to an appropriate future milestone or backlog,
3. continue the current milestone.

Avoid speculative abstractions that are not required by the current milestone.

---

## Architecture Invariants

The following rules apply across all milestones:

- Minecraft Java Edition + Fabric remain the target platform.
- Server-authoritative state is preferred for multiplayer gameplay.
- Client-only code must remain isolated from common/server code.
- Dedicated-server compatibility must be preserved.
- Golf simulation logic should remain as plain Java and testable as practical.
- Do not introduce external databases, cloud services, or backend infrastructure for core gameplay.
- Course and round state should use Minecraft-native/local persistence where persistence is required.
- Minecraft's world and block interactions are part of the golf experience, not obstacles to work around.
- Prefer simple implementations that are easy to tune and test.
- Do not prematurely extract a separate `golf-core` Gradle module.
- Do not expand MVP scope without explicitly updating the PRD and this document.

---

# Verification Ladder

Each milestone should use the following verification ladder where applicable.

```text
unit tests
    ↓
Gradle compile/build
    ↓
Loom development client/server
    ↓
Docker dedicated Fabric server
    ↓
manual gameplay verification
    ↓
milestone complete
```

Not every step applies to every individual task, but milestone completion should reach the highest relevant level.

A milestone is not complete merely because the code compiles.

---

# Development Environments

Two local development environments are expected throughout the project.

## Loom / Gradle Development Environment

Used for fast iteration.

Typical commands:

```bash
./gradlew test
./gradlew build
./gradlew runClient
./gradlew runServer
```

Primary uses:

- entity behavior
- physics tuning
- rendering
- HUD work
- local debugging
- rapid iteration

## Docker Fabric Integration Server

Used as the realistic multiplayer and dedicated-server environment.

Primary uses:

- dedicated-server boot validation
- multiplayer networking
- LAN family playtesting
- persistence testing
- server/client separation verification
- release-candidate testing

The Docker host may be the developer's Linux machine or another LAN machine such as an always-on Windows 11 mini-PC running Docker Desktop.

The host operating system should not affect the Fabric server architecture.

---

# Milestone Status

| Milestone | Name | Status |
|---|---|---|
| M0 | Project Setup | Complete |
| M1 | Golf Ball Physics | Complete (see docs/M1-CLOSEOUT.md) |
| M2 | Clubs and Shot Execution | Complete (see docs/M2-NOTES.md) |
| M3 | Three-Click Swing and HUD | Complete (see docs/M3-CLOSEOUT.md) |
| M4 | Holes, Cups, Boundaries, and Scoring | Complete (see docs/M4-CLOSEOUT.md) |
| M4.5 | Single-Player Loop Hardening | Complete (see docs/M4.5-CLOSEOUT.md) |
| M5 | Single-Player Course Experience | Complete (see docs/M5-CLOSEOUT.md) |
| M6 | Multiplayer Ready Golf | Complete (see docs/M6-CLOSEOUT.md) |
| M7 | MVP Hardening and Family Playtest | Complete (see docs/M7-CLOSEOUT.md) |
| M8 | V1 Course and Hole Authoring | Complete (see docs/M8-CLOSEOUT.md) |
| M8.5 | V1 Playability and Round UX Hardening | Superseded; implemented work is in the M8.6 baseline |
| M8.6 | Concurrent Rounds, Join Clarity, and Scoring UX | Complete (see docs/M8.6-CLOSEOUT.md) |
| M8.7 | Shot Variety and Trajectory Control | Complete (see docs/M8.7-CLOSEOUT.md) |
| M8.8 | Distance Display Units | Complete (see docs/M8.8-CLOSEOUT.md) |
| M8.9 | Practice Range | Complete (see docs/M8.9-CLOSEOUT.md) |
| M8.10 | Course Landscape Protection | Complete (see docs/M8.10-CLOSEOUT.md) |
| M8.11 | Swing Meter Precision | Implemented in 0.8.5; manual playtest pending (see docs/M8.11-CLOSEOUT.md) |
| M8.12 | Off-Tee Driver Discipline | Implemented in 0.8.5; manual playtest pending (see docs/M8.12-CLOSEOUT.md) |
| M8.13 | HUD Visibility and Command Help | Implemented in 0.8.5; client restart/manual acceptance pending (see docs/M8.13-CLOSEOUT.md) |
| M8.14 | Decked Driver Rollout | Implemented in 0.8.5; manual in-game flight confirmation pending (see docs/M8.14-CLOSEOUT.md) |
| M8.15 | Golf, World, and Builder Player Modes | **Complete in 0.9.0.** Flight-only Golf; Creative Builder with palette starter kit (approved 2026-09-30); verified over tests, Loom, Docker, and authenticated in-game play (see docs/M8.15-CLOSEOUT.md) |
| M9 | Additional Game Modes | Deferred |
| M10 | Client-Light / Bedrock Compatibility | **SHELVED (re-shelved 2026-09)**; Java experience prioritized; client-light code gated/unsupported (see docs/M10-PLAN.md) |
| M10.3 | Tier 1 Cross-Play (Java-Safe) | **Tier 1 implemented, then play shelved and repurposed as Bedrock visitor mode (2026-09)**; client-light clients watch (`/golf spectator`) or join the world (`/golf spectator leave`) and are invited to Java (see docs/BEDROCK-VISITOR-MODE-PLAN.md). |

---

# M0 — Project Setup

## Goal

Create a clean, reproducible Fabric mod project that can be developed locally, tested automatically, launched through Fabric Loom, and deployed to a Docker-based dedicated Fabric server for multiplayer/LAN testing.

This milestone establishes the development harness before any golf gameplay is implemented.

## Prerequisites

- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/MILESTONES.md`

## Scope

### Fabric Project Scaffold

Create the base Fabric project using the architecture-defined Minecraft generation and Java toolchain.

Include:

- Gradle wrapper
- Fabric Loom
- Fabric Loader
- Fabric API
- mod metadata
- base mod initializer
- client initializer
- resource structure
- logging
- version properties

Exact dependency patch versions should be pinned during repository creation using currently supported Fabric versions.

### Source Separation

Establish explicit separation between:

- common/server-compatible code
- client-only code

The project structure must make accidental dedicated-server loading of rendering, HUD, or client classes difficult.

### Package Structure

Create the initial package layout described by `docs/ARCHITECTURE.md`.

Packages may initially be empty or contain placeholders, but the layout should support:

- domain/game logic
- entities
- items
- rounds
- courses
- networking
- client HUD/rendering
- configuration

Avoid creating speculative layers that have no current use.

### Testing

Establish a plain-Java unit testing framework.

At minimum:

- one example unit test
- test command documented
- CI executes tests

The architecture should make future physics and rules logic testable without requiring a running Minecraft instance wherever practical.

### CI

Add a minimal continuous-integration workflow that verifies:

```bash
./gradlew test
./gradlew build
```

CI does not initially need to launch Minecraft or Docker.

### Docker Fabric Development Server

Create a dedicated development server environment under a directory such as:

```text
dev-server/
├── docker-compose.yml
├── mods/
├── scripts/
└── README.md
```

The server should use a Fabric-compatible Minecraft server container.

The configuration should include:

- Java Edition server
- Fabric
- project Minecraft version
- persistent local world/data
- port `25565`
- reasonable development memory allocation
- health checking where practical
- development-friendly server properties

Do not include unrelated Paper/Spigot infrastructure.

Do not include Geyser or Bedrock compatibility in MVP development infrastructure.

### Mod Sync Workflow

Provide a deterministic workflow for deploying the built mod JAR to the Docker server.

For example:

```text
./gradlew build
        ↓
identify final remapped mod JAR
        ↓
copy to dev-server/mods/minecraft-golf.jar
        ↓
restart Docker server
```

Do not blindly mount the entire Gradle output directory if it can expose multiple or incorrect artifacts.

Provide a script such as:

```bash
./scripts/dev-server-sync.sh
```

or equivalent.

### Docker Lifecycle Scripts

Provide simple commands or scripts for common integration-server operations.

Expected operations:

```text
build/sync mod
start server
stop server
restart server
view logs
reset development world
```

Names may vary, but the workflow should be obvious and documented.

### Resettable Test World

Provide a safe development-world reset workflow.

Reset operations must clearly distinguish disposable local development data from any future persistent family/test world.

Never delete server data implicitly as part of a normal build or restart.

### LAN Development Support

Document how another Java Edition client on the household LAN can connect to the Docker server.

This should support future deployment of the Docker server to an always-on LAN machine such as a Windows 11 mini-PC running Docker Desktop.

### Agent Instructions

Create an `AGENTS.md` file with concise repository-specific instructions for autonomous coding sessions.

It should include:

- required prerequisite docs
- current milestone rule
- build/test commands
- Docker verification workflow
- client/server separation rules
- no-scope-creep rule
- requirement to stop on verification failures or architectural deviations

## Required Deliverables

- working Fabric project
- Gradle wrapper
- pinned build configuration
- common and client source separation
- base mod initializer
- base client initializer
- unit test framework
- example test
- CI workflow
- `dev-server/docker-compose.yml`
- deterministic built-mod sync workflow
- Docker lifecycle scripts
- development world reset script
- development server README
- `AGENTS.md`
- root README with basic developer startup instructions

## Verification

M0 is complete only when all applicable checks pass.

### Automated

```bash
./gradlew test
./gradlew build
```

Both must succeed from a clean checkout.

### Loom

```bash
./gradlew runClient
```

The Minecraft client must launch with the mod loaded.

```bash
./gradlew runServer
```

The Fabric development server must launch without mod initialization errors.

### Docker

The built mod must be copied into the Docker integration server using the repository-provided workflow.

The Docker server must:

- start successfully,
- load Fabric,
- load the Biome Golf mod,
- reach a healthy/running state,
- show no client-only class loading errors.

### Manual

A Java Edition client with the compatible Fabric mod installed must be able to connect to the Docker server.

## Non-Goals

Do not implement:

- golf balls
- golf clubs
- swing mechanics
- HUD
- courses
- scoring
- round state
- wind
- progression
- course authoring

## Exit Criteria

The repository is a reliable development environment on which golf gameplay can safely be built.

A fresh developer or agent should be able to clone the repository, read the documentation, run the tests, launch the mod locally, and boot the Docker Fabric server without inventing missing setup steps.

---

# M1 — Golf Ball Physics

## Goal

Prove the central technical and gameplay premise:

> A golf ball moving through a Minecraft world can feel satisfying, predictable, and fun.

Do not build the golf game around the physics system until this milestone succeeds.

## Prerequisites

- M0 complete

## Scope

Implement a custom golf-ball entity with server-authoritative physical state.

Core behavior:

- spawn ball
- launch from a position
- initial velocity
- gravity
- world collision
- bounce
- rolling
- friction
- stop detection
- stable resting state

Implement a small initial surface-behavior system.

At minimum test meaningful differences for:

- normal ground/grass-like surface
- sand
- ice
- slime
- honey

Minecraft block collision must remain central to the design.

Shots should be able to interact with:

- terrain
- walls
- cliffs
- structures
- unusual block surfaces

## Physics Design Requirements

Avoid introducing an external physics engine.

Prefer explicit, tunable coefficients for:

- gravity influence
- air drag
- bounce/restitution
- rolling resistance
- stop threshold
- per-surface behavior

Keep the core calculations as isolated and testable as practical.

## Developer/Test Controls

Provide simple temporary developer controls or commands that make it easy to:

- spawn a golf ball
- launch at known velocities
- inspect velocity/state
- reset/remove test balls

These are development tools, not production UX.

## Required Deliverables

- golf-ball entity
- ball registration
- client rendering
- physics state
- collision handling
- surface behavior
- stop detection
- physics unit tests where practical
- development launch/test controls

## Verification

### Automated

Physics-related tests pass.

```bash
./gradlew test
./gradlew build
```

### Dedicated Server

Docker Fabric server boots with the entity implementation.

No client rendering code is loaded by the dedicated server.

### Manual Gameplay

Verify:

- airborne trajectory looks believable
- ball collides with terrain
- ball bounces
- ball transitions into rolling
- rolling slows naturally
- ball reliably stops
- ice behaves differently from normal ground
- sand/honey slow the ball
- slime produces intentionally unusual bounce
- walls can be used for bank shots
- elevation naturally affects movement

## Non-Goals

Do not implement:

- complete clubs
- swing meter
- scorekeeping
- cups
- multiplayer rounds
- wind
- advanced lies
- polished HUD

## Exit Criteria

A developer can repeatedly launch a ball through Minecraft terrain and the behavior is satisfying enough to justify building the golf game around it.

If the ball is not fun to hit and watch, stop and tune this milestone rather than progressing.

---

# M2 — Clubs and Shot Execution

## Goal

Turn the physics prototype into an actual golf-shot system.

## Prerequisites

- M1 complete

## Scope

Implement physical Minecraft golf club items.

Initial club set:

- Driver
- Fairway Wood
- Long Iron
- Mid Iron
- Short Iron
- Wedge
- Putter

Club data should define relevant shot properties such as:

- nominal distance
- launch angle
- velocity multiplier
- accuracy characteristics
- intended use

Implement camera-direction aiming.

Implement server-authoritative shot requests.

The client should communicate shot intent; the server should validate and execute the resulting launch.

Implement ownership rules so players can only intentionally strike their own active golf ball.

Implement simple club melee behavior.

## Required Deliverables

- club item definitions
- club registration
- club property model
- player golf-ball ownership
- basic aiming
- shot request networking
- server-side shot execution
- club melee behavior

## Verification

Verify:

- each club can hit the player's ball
- different clubs produce meaningfully different trajectories/distances
- putter produces low rolling shots
- driver produces long high-speed shots
- server owns the resulting ball launch
- clients cannot arbitrarily dictate final ball state
- players cannot intentionally strike another player's ball
- clubs function as simple melee items
- dedicated server remains stable

## Non-Goals

Do not implement:

- three-click meter
- polished shot HUD
- courses
- scoring
- wind
- advanced lie penalties

## Exit Criteria

A player can choose a club, aim using the Minecraft camera, and intentionally hit their own ball with predictable club-specific results.

---

# M3 — Three-Click Swing and HUD

## Goal

Create the skill-based shot interaction defined by the PRD.

## Prerequisites

- M2 complete

## Scope

Implement the three-click swing:

1. start meter,
2. set power,
3. set accuracy.

Implement a lightweight HUD showing:

- selected club
- approximate club distance
- swing meter
- power state
- accuracy zone
- current shot state

The accuracy result should influence left/right deviation.

The system should remain understandable to younger/casual players.

Implement a simple post-shot ball-follow camera suitable for MVP.

Do not implement the final cinematic camera yet.

## Required Deliverables

- swing state machine
- power calculation
- accuracy calculation
- shot deviation
- HUD
- input handling
- basic ball-follow camera
- tests for deterministic shot calculation where practical

## Verification

Verify:

- three-click flow works consistently
- partial-power shots work
- good timing produces accurate shots
- poor timing produces visible misses
- meter remains responsive in multiplayer
- server validates final shot parameters
- HUD does not leak client code into dedicated server
- follow camera remains playable and returns control correctly

## Non-Goals

Do not implement:

- cinematic camera
- landing-area prediction
- wind
- scoring
- advanced terrain lies

## Exit Criteria

A player can understand and intentionally influence aim, power, and accuracy through the full shot interaction.

---

# M4 — Holes, Cups, Boundaries, and Scoring

## Goal

Turn individual golf shots into a playable golf hole.

## Prerequisites

- M3 complete

## Scope

Implement:

- tee location
- cup/flag
- physical hole completion
- hole metadata
- par
- stroke counting
- simplified playable boundary
- water penalty
- out-of-bounds penalty
- Double Par + 2 default stroke limit
- Pick Up Ball
- golf scoring terminology
- hole-complete state

For MVP, use the simplest robust boundary representation, likely an axis-aligned configured region.

Do not build the final course-authoring system.

Hole definitions may initially be configuration/data driven.

## Required Deliverables

- hole definition model
- tee handling
- cup/flag implementation
- hole-out detection
- boundary detection
- simplified hazard recovery
- scoring
- stroke cap
- score terminology
- single-hole lifecycle

## Verification

A single player must be able to:

1. start at the tee,
2. hit multiple shots,
3. remain within a defined hole,
4. incur water/OOB penalties,
5. physically sink the ball,
6. receive the correct hole score.

Verify Double Par + 2 behavior and Pick Up Ball.

## Non-Goals

Do not implement:

- course authoring commands
- multiple game modes
- Ready Golf multiplayer orchestration
- wind
- cinematic camera
- advanced rules-engine fidelity

## Exit Criteria

One complete golf hole can be played from tee to cup with correct basic scoring and boundaries.

---

# M4.5 — Single-Player Loop Hardening

## Goal

Make the configured single-player hole repeatable, understandable, and recoverable without developer assistance before multiplayer adds concurrency.

## Prerequisites

- M4 complete

## Scope

Implement, in bounded increments:

1. explicit lifecycle and practice-mode contracts,
2. restart, abandon, missing-ball recovery, and equipment guidance,
3. typed server-to-client hole-state synchronization with a client-only HUD,
4. a repeatable, non-destructive development hole and documented reset flow,
5. integration hardening and manual single-player acceptance.

Preserve server authority for gameplay state and scoring. Practice remains unrestricted when no hole is active. The player remains at the shot origin while the client-only camera follows the ball; do not add automatic teleport-to-ball.

## Required Deliverables

- player-accessible start, restart, abandon, status, and Pick Up behavior
- clear equipment availability or actionable in-game guidance
- explicit active-hole versus practice behavior
- authoritative hole HUD state: hole/par, strokes/cap, penalties, score-to-par, lifecycle status, and final result
- safe cleanup and recovery for restart, abandon, missing ball, disconnect/reconnect, dimension errors, and configuration errors
- repeatable development-hole setup that does not destructively rewrite arbitrary terrain
- tests and documentation for repeated play and reset behavior

## Verification

- `./gradlew test`
- `./gradlew clean test build`
- no `net.minecraft.client` imports under `src/main`
- Loom dedicated server boot and registration logs
- Loom client startup and resource/model checks
- Docker JAR identity, healthy server, and development time/weather controls
- manual single-player loop covering practice, start/restart/abandon, equipment, HUD updates, accepted/rejected shots, Pick Up, missing-ball recovery, completion, replay, and camera restoration

## Non-Goals

Do not implement:

- multiplayer players or Ready Golf
- round orchestration or multiple-hole progression
- course-authoring commands or UI
- traditional turns or additional modes
- wind, prediction, or cinematic camera
- automatic teleport-to-ball
- persistent global statistics
- speculative abstractions for M5

## Exit Criteria

A player can launch the mod, start and finish the configured hole repeatedly without developer assistance, understand the complete authoritative hole state through the UI, and recover safely from expected lifecycle failures.

---

# M5 — Single-Player Course Experience

## Goal

Prove the complete single-player, full-sized golf course experience — a generated practice range, exactly three authored holes ordered as Hole 1 par 4, Hole 2 par 3, and Hole 3 par 5, course sequencing, between-hole transitions, a final three-hole scorecard, replay/reset, and full gameplay tuning across realistic hole lengths — before multiplayer concurrency multiplies uncertain gameplay.

This milestone replaces the former "Multiplayer Ready Golf" plan, which is moved to M6 and applied only after the single-player course loop is proven (GSD decision D018).

## Prerequisites

- M4.5 complete

## Scope

### Course domain and configuration

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

### Deterministic practice range

Generate one bounded practice range containing useful test areas such as:

- tee/driver lane
- distance markers and targets
- putting green
- short-game or wedge target
- fairway/rough contrast
- bunker/sand
- water recovery
- ice/slime or other Minecraft-native surface lanes where useful

The practice range is not part of the scored three-hole round. Practice remains available outside an active course attempt.

### Three authored generated holes

Generate exactly three intentionally different holes:

- **Hole 1 — Par 4** — driver tee shot, fairway positioning, approach shot, bunker and/or water risk, and ordinary golf scoring flow.
- **Hole 2 — Par 3** — short iron or wedge accuracy, approach control, putting, and a concise but meaningful hazard decision.
- **Hole 3 — Par 5** — realistic longer-shot strategy, multiple-shot pacing, elevation or route choice, Minecraft-native terrain/surface interaction, and avoiding excessive empty travel.

The holes should validate the full club set and feel meaningfully different.

### Course progression

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

### Next-shot travel

Full-sized-hole playtesting found manual walking and chat-driven travel too tedious. After an active owned ball naturally stops, automatically move the player to a server-validated safe standing position near the ball.

Constraints:

- keep the server authoritative
- trigger only from natural physics rest, never tee placement, penalties, or completion
- reject missing, moving, unowned, completed, or cross-dimension ball state
- leave the player in place with actionable feedback when no safe destination exists
- preserve ordinary Minecraft traversal outside active shot-to-shot course play

## Required Deliverables

- server-owned course model with exactly three ordered holes
- course-level state (current hole, per-hole score, cumulative score, completion, replay/reset)
- deterministic generated practice range (not part of the scored round)
- three authored generated holes ordered par 4, par 3, par 5
- single-player course sequencing and between-hole transitions
- final three-hole scorecard
- replay/reset behavior
- safe recovery from missing balls or interrupted state
- automatic server-authoritative safe travel after natural ball rest
- explicit operator course-generation command with documented bounds

## Verification

- `./gradlew test`
- `./gradlew clean test build`
- no `net.minecraft.client` imports under `src/main`
- focused unit tests for course state, sequencing, scoring, and layout calculations
- tests for malformed course configuration and invalid transitions
- tests for deterministic generation plans
- tests proving outside-footprint preservation where practical
- Loom dedicated-server boot and registration logs
- Loom client startup, HUD, resource, and camera checks
- Docker JAR identity and healthy server
- repeated course preparation proving idempotence
- Docker daytime and paused time/weather verification
- manual practice-range validation
- manual completion of Hole 1 par 4, Hole 2 par 3, and Hole 3 par 5
- scorecard and replay validation
- club-distance and travel-pacing findings
- camera restoration on short and long shots
- penalty, Pick Up, cap, restart, abandon, and missing-ball recovery across the course

## Non-Goals

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

## Exit Criteria

M5 is complete only when one player can:

1. explicitly generate or restore the bounded development course,
2. use the practice range,
3. start the three-hole course,
4. complete Hole 1 (par 4),
5. transition to and complete Hole 2 (par 3),
6. transition to and complete Hole 3 (par 5),
7. receive an accurate per-hole and cumulative scorecard,
8. recover safely from expected lifecycle failures,
9. replay the course without stale balls or scores,
10. complete the experience using documented controls without developer intervention.

The generated layout must also prove deterministic output, idempotent rebuild, documented version and bounds, no changes outside the declared footprint, valid tee/cup support, intended surfaces and hazards, and safe Docker reset/rebuild behavior.

---

# M6 — Multiplayer Ready Golf

## Goal

Deliver the intended 1–4 player multiplayer experience, applied to the already-proven three-hole single-player course built in M5.

## Prerequisites

- M5 complete

## Scope

Implement server-authoritative round state for multiple golfers.

Default behavior is Ready Golf.

Each player should have independently tracked:

- active ball
- stroke count
- hole status
- score
- shot state

Players may hit independently without waiting for turns.

Players should see other golfers and active golf balls.

The hole advances only when every golfer has:

- holed out, or
- completed via Pick Up / stroke cap.

Implement between-hole coordination sufficient for MVP, applied to the three-hole course proven in M5.

The three-hole course itself is built in M5; M6 adds only the multiplayer concurrency layer on top of that proven course.

## Required Deliverables

- round state
- player participation state
- multiplayer ball ownership
- concurrent shot handling
- hole completion synchronization
- per-player score state
- multiplayer hole results
- next-hole transition across the three-hole course

## Verification

Test with at least two clients whenever practical and validate up to four during manual LAN testing.

Verify:

- players may hit concurrently
- one player's shot does not corrupt another player's state
- each ball remains correctly owned
- all clients see authoritative ball motion
- scoring is independent
- hole does not advance early
- disconnected/reconnected player behavior fails safely
- Docker server remains authoritative

## Non-Goals

Do not implement:

- traditional turn mode
- scramble
- matchmaking
- public-server infrastructure
- persistent global statistics

## Exit Criteria

Up to four golfers can complete the proven three-hole course using Ready Golf without state corruption or forced turn-taking.

---

# M7 — MVP Hardening and Family Playtest

## Goal

Determine whether the MVP is genuinely fun and stable enough to justify V1 development.

## Playtest findings (from M6 S4 two-player session)

- **Course destructibility.** In creative mode any golfer can break any block,
  including digging holes in greens and destroying the cup/flag. The ability to
  clear in-the-way trees/rocks is fun Minecraft flavor worth keeping, but tee
  boxes, greens, and the cup/flag must be protected. Candidate fix: a server-side
  block-break guard keyed to course metadata (tee/green/cup plus a configurable
  vicinity radius) via Fabric's block-break cancel event — avoid a full
  world-guard framework. Recovery today is `/golf dev preparecourse` (idempotent).
- **Advance UX.** The post-hole flow already offers a clickable chat
  "[Go to next tee]" action plus `/golf nexthole`, but playtesters want either a
  no-text-input prompt (e.g. a HUD button) or full auto-advance once all golfers
  are terminal. Full auto-advance would deviate from the M6 player-initiated
  `/golf nexthole` contract, so treat it as a deliberate M7 product decision.

### Recorded M7 decisions (2026-09-10)

- **S1 protected-zone scope:** tee/cup vicinity only. The block-break guard
  protects a configurable-radius vicinity around each hole's authored tee and cup
  positions; greens are protected via cup vicinity, tee boxes via tee vicinity,
  and the cup/flag block is always protected. No new green metadata is authored
  in M7.
- **S2 advance UX:** HUD button. Advance stays player-initiated and
  barrier-gated via a no-text-input HUD prompt/button that invokes the existing
  server-side `nextHole` barrier; `/golf nexthole` and the clickable chat action
  remain as fallbacks. Full auto-advance is rejected for M7 (would be a recorded
  M6-contract deviation if revisited).
- **Docker gameplay profile:** Survival mode with Peaceful difficulty. Golf clubs
  participate in ordinary vanilla block interaction; M7 does not restrict
  obstacle clearing exclusively to clubs.

## Prerequisites

- M6 complete

## Scope

Prioritize playtesting, tuning, bug fixing, and usability rather than new features.

Run repeated family/LAN sessions.

Capture observations around:

- ball feel
- swing difficulty
- shot pacing
- club balance
- putting
- camera usability
- multiplayer synchronization
- hole pacing
- scoring clarity
- course navigation
- Minecraft terrain interactions

Tune physics and controls based on actual play.

Fix:

- crashes
- desync
- stuck balls
- impossible recovery states
- misleading HUD behavior
- scoring errors
- major usability problems

## Success Questions

Use the PRD success criteria, especially:

- Is hitting the ball satisfying?
- Do players understand the three-click swing?
- Does Ready Golf keep the round moving?
- Are Minecraft interactions fun?
- Do players want to replay the course?
- Do players start proposing/building new hole ideas?
- Does the family voluntarily want to play again?

## Required Deliverables

- documented playtest findings
- prioritized defects
- tuning changes
- resolved MVP-critical issues
- stable MVP build
- decision on whether to proceed to V1

## Non-Goals

Do not use this milestone as an excuse to add the deferred V1 feature list.

## Exit Criteria

The MVP is stable enough for repeated family play and has demonstrated enough fun/replay value to continue development.

---

# M8 — V1 Course and Hole Authoring

## Status

Complete. M8 delivered the bounded operator-facing course and hole authoring slice,
including persistence, validation, selection, and playability verification. The
closeout record is in `docs/M8-CLOSEOUT.md`.

## Goal

Let operators and builders create and play additional Minecraft-native courses and
holes without changing the core identity or weakening the existing server-authority
and course-integrity contracts.

## Bounded Scope

### Course and hole authoring

- Add operator/admin commands to create and manage course metadata on existing
  Minecraft terrain:
  - `/golf course create <name>`
  - `/golf course edit <id>`
  - `/golf hole tee <number>`
  - `/golf hole cup <number>`
  - `/golf hole par <number> <value>`
  - `/golf hole bounds <number>` (run twice for opposite corners)
- Support more than one named course in a world.
- Validate course and hole definitions before they can be played.
- Preserve authored tee, cup, par, dimension, and boundary metadata through the
  repository's Minecraft-native/local persistence approach.
- Keep course protection and recovery behavior compatible with authored locations.
- Provide commands or status output to inspect, select, and reset authored course
  definitions safely.
- Preserve the existing deterministic three-hole course as a regression and replay
  fixture while new authoring is added.
- Support selecting an authored course for the existing single-player and Ready Golf
  flows where the current round contracts permit it.

### Authoring usability

- Give clear feedback for invalid command order, missing tee/cup/par/bounds, duplicate
  names or hole numbers, unsupported dimensions, and unsafe definitions.
- Document the command workflow and the limits of the bounded authoring system.
- Add focused unit and integration coverage for creation, validation, persistence,
  selection, reset, and playability.

## Explicitly Deferred From M8

The following remain outside this bounded milestone:

- visual course editor or polished authoring UI
- arbitrary procedural course generation
- course marketplace, sharing service, or external backend
- Scramble and other alternate game modes
- wind, advanced lies, cinematic camera, landing preview, teleport-to-ball, chip
  shots, expanded clubs, and presentation-polish candidates
- multiple tee boxes per hole (red/white/blue, per-player tee selection at round
  start; explicitly WITHOUT per-tee par — one par per hole regardless of tee).
  Requested during early M8 family course-building; deferred so S2 authoring stays
  simple. Touches HoleDefinition, round state, HUD distances, transitions, and both
  JSON serializers, so it deserves its own focused slice.

These may be reconsidered in a later V1 polish milestone after authoring has been
validated through play.

## Exit Criteria

M8 is complete when an operator can define, validate, save, select, and play a
second course with at least one authored hole using documented commands, while the
existing three-hole course still completes correctly for single-player and Ready
Golf play. Invalid definitions fail safely, authored course state survives the
documented restart/reset boundary, course protection remains enforced, and no
client-only code enters the dedicated-server path.

---

# M8.6 — Concurrent Rounds, Join Clarity, and Scoring UX

## Status

Complete. See `docs/M8.6-CLOSEOUT.md` for the implementation, verification, and
family playtest record.
M8.5 is not a separate active milestone; its committed implementation is the M8.6
baseline, and relevant remaining verification has been folded into M8.6.

## Goal

Allow multiple independent Ready Golf rounds and unrelated solo attempts to coexist,
make lobby discovery and lifecycle actions complete through server commands/chat, clarify
active-hole scoring presentation, and fix completed-solo reconnect without weakening
server authority or expanding into matchmaking, alternate modes, or full client-light
migration.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.6-PLAN.md`.

---

# M8.7 — Shot Variety and Trajectory Control

## Status

Complete. See `docs/M8.7-CLOSEOUT.md` for the implementation, tuning, verification, and
the remaining manual-playtest boundary.

## Goal

Add server-authoritative trajectory choice (Standard, Chip, Stinger, and Flop) and a
server-authoritative near-cup Tap-In action without changing golf scoring, the round
lifecycle, or the single server-authoritative shot model. The Standard shot remains the
default.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.7-PLAN.md`.

---

# M8.8 — Distance Display Units

## Status

Complete. See `docs/M8.8-CLOSEOUT.md` for the implementation, verification, and the
remaining manual HUD boundary. M8.8 was explicitly authorized as an independent
presentation slice before M8.7 closeout; M8.7's scope was unchanged.

## Goal

Display golf distances in familiar yards by default (fixed `1 block = 1.75 yards`
conversion) while keeping blocks authoritative for gameplay, networking, physics,
scoring, and course geometry, and expose server-authoritative current-shot travel in the
Hole HUD.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.8-PLAN.md`.

---

# M8.9 — Practice Range

## Status

Complete. See `docs/M8.9-CLOSEOUT.md` for the implementation, verification, and the
remaining manual command boundary. The practice range was built alongside M8.8 but is
scoped and closed separately as a server-authoritative operator feature.

## Goal

Provide a persistent, server-authoritative practice range with a saved tee and up to
eight cup-and-flag targets, without changing scoring, rounds, or hole state.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.9-PLAN.md`.

---

# M8.10 — Course Landscape Protection

## Status

Complete. Automated, Loom, Docker/RCON, and the in-game operator/non-operator
break/place/TNT matrix all pass. See `docs/M8.10-CLOSEOUT.md`. M9 remains deferred.

## Goal

Add an operator-authored whole-course landscape perimeter that blocks block break, block
place, and TNT detonation inside it, with a per-course lock that removes the operator
exemption, effective for both drafts and finalized courses.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.10-PLAN.md`.

---

# M8.11 — Swing Meter Precision

## Status

Implemented in 0.8.5. See `docs/M8.11-CLOSEOUT.md`. Small, independently shipped
bug-fix milestone.

## Goal

Make the three-click accuracy meter's perfect value exactly reachable and make the drawn
perfect zone agree with the resolver, so a dead-center click produces zero deviation and a
click inside the green zone flies straight.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.11-PLAN.md`.

---

# M8.12 — Off-Tee Driver Discipline (Lie-Aware Shot Context)

## Status

Implemented in 0.8.5. See `docs/M8.12-CLOSEOUT.md`. Playtest-gated magnitudes.

## Goal

Introduce a bounded, server-authoritative lie context so a Driver hit from anywhere other
than a tee box launches lower and travels shorter, closing the driver-off-the-deck par-5
exploit while leaving the choice legal and leaving the full rough/bunker lie system
(PRD §12) deferred.

## Exit Criteria

Use the final success criteria and full verification ladder in `docs/M8.12-PLAN.md`.

---

# M8.13 — HUD Visibility and Command Help

## Status

Implemented in 0.8.5; automated, Loom, and Docker checks passed. The JARs are staged
in both local clients, but in-game acceptance remains pending client restart. See
`docs/M8.13-CLOSEOUT.md`.

## Goal

Let players hide and restore both client HUD panels together, and make `/golf help`
accurately direct players to the full player and operator command references.

## Scope

- Add the `H` keybind and server `/golf hud` command with clientbound toggle intent to
  toggle both HUDs. Visibility remains client-only; do not register a shadow client root.
- Keep visibility session-local and automatically reveal on a club-in-hand right-click
  swing attempt, including the no-ball feedback path.
- Keep `/golf help` concise for players and add `/golf help admin`, clearly labelled as
  operator/gamemaster-only while remaining discoverable by everyone.
- Add a regression test that every registered top-level `/golf` literal is mentioned on
  one of the help pages; correct stale command-registration logging.
- Update the command guide and changelog.

## Verification

Run `./gradlew test`, `./gradlew build`, Loom client and server, Docker dedicated-server
sync/restart/health, both local client deployments, and manual in-game HUD/help checks.

## Exit Criteria

Both HUDs toggle in unison through H and `/golf hud`, shot attempts reveal them, the two
help pages cover the command tree, and the documented verification ladder is complete.

---

# M8.14 — Decked Driver Rollout

## Status

Implemented in 0.8.5. Automated flight checks and Docker verification passed; manual
in-game flight confirmation remains pending. See `docs/M8.14-CLOSEOUT.md`.

## Goal

Give the decked Driver's Standard shot a little runner-style rollout after landing without
removing its distance penalty or changing explicit Stinger behavior.

## Scope

- Raise landing horizontal retention for decked Standard Driver shots from 0.25 to 0.40.
- Keep Standard's rolling-friction multiplier, the deck launch/accuracy penalties, and
  decked Stinger profile unchanged.
- Verify simulated rollout increases, the decked shot remains shorter than a teed Driver,
  and it remains shorter than a standard Fairway Wood.

## Exit Criteria

Automated simulation proves the runner adds distance over the former deck profile while
preserving both distance comparisons, and the exact artifact passes build, Loom, Docker,
and installed-client deployment verification. Manual in-game feel is tracked separately.

---

# M8.15 — Golf, World, and Builder Player Modes

## Status

**Complete — shipped in 0.9.0.** Golf is flight-only (Survival plus mayfly and
invulnerable, normal inventory) per the resolved product decision; World is ordinary
Survival. Builder is **true Creative for operators** (approved 2026-09-30): instant
break/place, flight, and the full item catalogue, with the curated palette as an
inventory starter kit (one of each configured block plus the Driver-through-Wedge club
set) applied on entry and via `/golf builder restock`. The earlier isolation-based
Builder (snapshot/restore, perimeter-scoped edits, export blocks, one-click erase) is
superseded; legacy World snapshots are restored once and retired. `docs/M8.15-PLAN.md`
records the full contract; `docs/M8.15-CLOSEOUT.md` records acceptance, including the
in-game proof from live practice play on the dev server.

## Goal

Give supported Java players a server-owned Golf mode (flight and damage protection with
the normal Survival inventory), an explicit World mode (ordinary Survival), and a
fast operator Creative Builder whose curated palette defines the starting inventory —
while keeping the authored course landscapes protected and visitors gated.

## Scope

- `/golf mode golf|world|status`, `/golf mode build` (lists authored courses as a
  Builder reference), and `/golf builder course <courseId> | restock | palette reload`,
  with confirmed abandonment of an unfinished attempt before switching.
- World-scoped mode persistence (`data/minecraft_golf_player_modes.json`); supported
  newcomers default to Golf and an explicit World choice is remembered.
- Safe flight exit and reconnect/respawn revalidation.
- Builder = Creative game type for operators only, with the curated palette as the
  inventory starter kit: one of each configured block plus the Driver-through-Wedge
  club set, applied on entry/reconnect and via `/golf builder restock`.
- Creative items carried out of Builder intentionally remain in World play (approved
  trusted-server decision); legacy isolation-era World snapshots are restored once then
  deleted.
- Practice balls always launch from the full tee profile, so a Builder can measure
  hole lengths from anywhere in a course; the off-tee Driver penalty still applies to
  scored golf balls.
- `config/minecraft_golf/builder_palette.json` with `/golf builder palette reload`;
  invalid replacements retain the last valid palette. The default palette covers 30
  landscaping blocks (sand, soil, stone, logs, leaves, saplings, bushes, grass, flowers)
  plus the pale-oak fence/button marker items.
- The authored landscape perimeter (M8.10) still protects every course from everyone;
  a locked perimeter denies even the Creative Builder. Visitors never receive Builder.

## Exit Criteria

Mode transitions are failure-atomic; solo and every Ready Golf participant receive Golf
through commands and GUI paths; Builder items cannot leak into World play; palette
reloads never erase terrain; visitors stay gated; and the documented verification ladder
(including authenticated two-player checks) passes.

---

# M9 — Additional Game Modes

## Status

Deferred until the next alternate mode is prioritized.

## Candidate Scope

### Scramble

Prioritize Scramble as the first alternate multiplayer game mode.

### Future Modes

Potential later modes:

- Match Play
- Best Ball
- Closest to the Pin
- Longest Drive
- Speed Golf

## Non-Goals

Do not build a course marketplace, public matchmaking service, or external backend as part of this milestone unless the project direction is explicitly changed.

## Exit Criteria

To be defined after V1 priorities are established.

---

# M10 — Client-Light / Bedrock Compatibility (SHELVED)

## Status

**Shelved (re-shelved 2026-09).** Client-light/Bedrock parity is not a goal. The assessment
(`docs/BEDROCK-COMPATIBILITY-ASSESSMENT.md`) concluded **practical with tradeoffs**; a real
Bedrock client connected, but a playtest showed the blockers are input feel and ball-flight
smoothness rather than visuals (the held-use input never saw a release; flight was choppy).
Per the priority order (`good golf > maintainable server > Java experience > client-light >
Bedrock`), development is returning to the **Java experience** and Bedrock is shelved.

**M10.3 Tier 1** was implemented first as the one bounded Java-safe slice: it gates the ball
mirror (modded Java no longer sees the proxy in the all-Java world), removes the shaky server
camera, and adds vanilla presentation fallbacks, all gated on `!canSend(HoleStatePayload)`.
A G1 tap-meter input fix followed. All of it stays in the tree, gated and unsupported. See
`docs/M10.3-CLOSEOUT.md`.

## Why It Is Shelved

The modded Java client is the supported experience. The connection fix, dual clubs, ball
mirror, held-use input, and server ball camera remain in the tree but are gated to clients
that cannot receive `HoleStatePayload`. The vanilla cup/flag swap was **reverted** because it
was the one change that degraded the **modded Java** visuals. The only planned follow-up is
M10.3 Tier 1 (`docs/M10.3-PLAN.md`); camera and Bedrock visual parity are not planned.

## Candidate Scope

1. Geyser/Floodgate prototype spike (evidence gate, no features).
2. Vanilla-compatible shot input plus a command fallback, funnelling into the unchanged
   `ShotService`; keep the three-click meter as an optional Java enhancement.
3. Ball representation that does not require a custom client entity.
4. Server-side vanilla presentation fallbacks (action bar / boss bar / chat / scoreboard)
   alongside the existing payloads.
5. An architecture test enforcing "no `net.minecraft` imports in the core domain packages"
   instead of splitting Gradle modules.

## Non-Goals

- Console (Xbox/PlayStation/Switch) Bedrock deployment.
- Mixin-based protocol tricks, a Hydraulic dependency, or a mandatory Bedrock pack.
- Course marketplace, matchmaking, or any external backend.

## Exit Criteria

For M10 as a whole: none (shelved). For the bounded **M10.3 Tier 1** slice: a modded Java
player and a Bedrock player complete the same round side-by-side with consistent
server-authoritative scores, the modded Java experience is demonstrably unchanged, and no
Bedrock visual parity is claimed. See `docs/M10.3-PLAN.md`.

---

# Backlog — Deferred Work

Deferred work that is not assigned to a milestone. Keep it short; delete entries when they ship.

## Deprecate `/golf dev preparecourse`

`/golf dev preparecourse` is M5-era development scaffolding: it rebuilds the fixed
`minecraft_golf:m5_ocean_campus` v11 layout inside the seed-specific envelope
`X[-640..448] Y[32..192] Z[-256..640]`. It predates authored courses and world sync, and
the current workflow no longer starts from a generated campus:

- The known-good starting state for a local world is now its **baseline**, captured by
  `world-sync.sh pull` and restored by `dev-server-reset.sh`. That supersedes
  `preparecourse` as the recovery path — exact, offline, and with no 574k-block replan.
- Course work happens on the live world pulled into the dev server, not on a generated
  practice campus.

Deprecate first, delete later:

1. Mark the command deprecated in `docs/COMMANDS.md`.
2. Delete the command, the M5 layout generator, and the dev-only code paths that exist
   to serve it.
3. Prune the stale references across `docs/M5-*.md`, `docs/M6-*.md`, `docs/M7-*.md`, and
   the M5 anchor in this file.

No structural blocker: `CourseBlockBreakGuard` exempts operators by permission level and
only mentions `preparecourse` in a javadoc comment, and no test references the command.

## New-world bootstrap for the local dev server — complete

`scripts/dev-server-new-world.sh <world-name> <seed>` registers and generates a fresh
local-only world, then captures its pristine baseline. See
[apcode-dev world sync workflow](https://github.com/prillcode/apcode-dev/blob/main/docs/WORLD-SYNC-WORKFLOW.md#baselines-and-reset).

## Consume `GET /mc/servers` from the site — complete

`apps/biome-golf-site` is an Astro promo/docs scaffold for the mod. Its optional
BirdieBiome server-status section renders the public lifecycle-only fleet listing using
a server-side request to `https://api.apcode.dev/mc/servers`; it requires no API token
and adds no browser CORS dependency. Brand and full documentation remain future work.

## Biome Golf site branding and documentation

The deployed Astro site is a useful placeholder, not the finished public identity or
wiki. Future work:

- Align the visual identity with the Biome Golf Modrinth project logo and branding.
- Build substantive player-facing docs/wiki pages for installation, gameplay, and
  course creation.
- Reference the canonical `docs/COMMANDS.md` command guide from the site and keep it
  discoverable without duplicating content that can drift.
- Keep the BirdieBiome public server listing as a secondary way to try the mod, not
  the focus of the site.

---

# Milestone Completion Protocol

When an agent or developer completes a milestone:

1. Run all milestone verification.
2. Fix any failed verification before proceeding.
3. Confirm architecture invariants remain intact.
4. Confirm no deferred features were unintentionally introduced.
5. Update the milestone status table.
6. Record notable implementation decisions.
7. Record deferred issues or future ideas.
8. Commit the verified milestone state.
9. Prepare a concise handoff for the next milestone.

If verification fails or implementation requires an architectural deviation:

**Stop progression.**

Document the failure or proposed deviation before continuing.

---

# Current Starting Point

M0–M8.10 are implemented (see the milestone closeout records through
`docs/M8.10-CLOSEOUT.md`). M8.5 is superseded as a standalone plan, and its implemented
work forms part of the completed M8.6 concurrent-round hardening milestone. M8.7 added
server-authoritative shot variety and tap-in; M8.8 added the yard distance display; M8.9
added the operator-authored practice range; M8.10 added operator-authored whole-course
landscape protection (break/place/TNT) with a per-course lock, verified automatically, over
Docker/RCON, and in-game as both operator and non-operator. M9 — Additional Game Modes
remains deferred until an alternate mode is explicitly prioritized. M10 — Client-Light /
Bedrock Compatibility was assessed (`docs/BEDROCK-COMPATIBILITY-ASSESSMENT.md`, verdict:
practical with tradeoffs) and a Bedrock prototype ran. Its one bounded slice, **M10.3 Tier 1**,
shipped (ball-mirror gate, vanilla fallbacks, G1 tap input), but a Bedrock playtest showed the
remaining blockers are feel rather than visuals, so Bedrock is **shelved again**: the modded
Java client is the supported target, development is returning to the Java experience, and the
remaining client-light/Bedrock code is gated, unsupported, and not pursued further. See
`docs/M10.3-CLOSEOUT.md`.

**M8.11 (Swing Meter Precision)** and **M8.12 (Off-Tee Driver Discipline)** are implemented
and shipped in **0.8.5**: the accuracy meter now samples its perfect centre exactly and the
drawn zone matches the resolver, and a Driver away from a tee launches lower and shorter
with a `(deck)` HUD label. Automated tests, Loom client, Docker dedicated server, and the
installed-client deployment all pass; the remaining manual boundary is meter feel and a
real par-5 tee-vs-deck playtest. See `docs/M8.11-CLOSEOUT.md` and `docs/M8.12-CLOSEOUT.md`.
