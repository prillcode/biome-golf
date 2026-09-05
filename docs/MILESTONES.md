# MILESTONES.md

## Purpose

This document defines the execution milestones for the Minecraft Golf mod.

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
| M0 | Project Setup | Not Started |
| M1 | Golf Ball Physics | Not Started |
| M2 | Clubs and Shot Execution | Not Started |
| M3 | Three-Click Swing and HUD | Not Started |
| M4 | Holes, Cups, Boundaries, and Scoring | Not Started |
| M5 | Multiplayer Ready Golf | Not Started |
| M6 | Three-Hole MVP Course | Not Started |
| M7 | MVP Hardening and Family Playtest | Not Started |
| M8 | V1 Gameplay Enhancements | Deferred |
| M9 | Course Authoring and Additional Modes | Deferred |

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
- load the Minecraft Golf mod,
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
- Double Par default stroke limit
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

Verify Double Par behavior and Pick Up Ball.

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

# M5 — Multiplayer Ready Golf

## Goal

Deliver the intended 1–4 player multiplayer experience.

## Prerequisites

- M4 complete

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

Implement between-hole coordination sufficient for MVP.

## Required Deliverables

- round state
- player participation state
- multiplayer ball ownership
- concurrent shot handling
- hole completion synchronization
- per-player score state
- multiplayer hole results
- next-hole transition

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

Up to four golfers can complete the same hole using Ready Golf without state corruption or forced turn-taking.

---

# M6 — Three-Hole MVP Course

## Goal

Assemble all completed systems into the PRD-defined MVP experience.

## Prerequisites

- M5 complete

## Scope

Create or configure a three-hole development/test course.

The course should intentionally exercise different mechanics.

Suggested examples:

### Hole 1 — Basic Golf

Simple terrain validating:

- tee shot
- iron/wedge approach
- putting
- normal scoring

### Hole 2 — Terrain Interaction

Include:

- elevation
- sand
- water
- boundary risk

### Hole 3 — Minecraft Golf

Include unusual Minecraft-native interactions such as:

- wall bank shot
- ice
- slime
- cave/cliff/structure interaction

Implement:

- course-level hole ordering
- between-hole transition
- optional teleport-to-next-tee prompt
- final three-hole scorecard

## Required Deliverables

- three configured holes
- course definition
- hole sequencing
- next-tee transition
- final scorecard
- complete multiplayer round

## Verification

Four-player target test:

1. join server,
2. start course,
3. play Hole 1,
4. advance,
5. play Hole 2,
6. advance,
7. play Hole 3,
8. receive final scorecard.

The course must be completable using only documented gameplay controls.

## Non-Goals

Do not implement:

- course-authoring commands
- wind
- cinematic camera
- scramble
- progression
- cosmetics
- large content library

## Exit Criteria

The PRD's three-hole multiplayer MVP definition is satisfied end-to-end.

---

# M7 — MVP Hardening and Family Playtest

## Goal

Determine whether the MVP is genuinely fun and stable enough to justify V1 development.

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

# M8 — V1 Gameplay Enhancements

## Status

Deferred until M7 validates the MVP.

## Goal

Improve depth, presentation, and convenience without changing the core identity.

## Candidate Scope

Prioritize based on playtest findings.

Potential features:

- wind
- advanced terrain/lie effects
- cinematic ball camera
- approximate landing-area indicator
- optional teleport-to-ball
- improved ball identification
- expanded club set
- richer sounds and particles
- refined HUD
- improved server configuration
- optional survival/mob interaction configuration
- traditional turn-based mode

Do not assume every candidate feature belongs in V1.

## Exit Criteria

To be defined after M7.

---

# M9 — Course Authoring and Additional Modes

## Status

Deferred until the core game is proven.

## Candidate Scope

### Course Authoring

Potential commands:

```text
/golf course create <name>
/golf hole create <number>
/golf hole tee
/golf hole cup
/golf hole par <value>
/golf hole bounds ...
```

Support multiple courses in one Minecraft world.

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

The project is currently in pre-repository planning.

Completed planning artifacts:

- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/MILESTONES.md`

The next execution target is:

> **M0 — Project Setup**

The first local agent session should create the repository foundation and complete M0 without implementing golf gameplay.
