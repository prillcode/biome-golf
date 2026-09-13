# Minecraft Golf Mod — Architecture

**Status:** Initial architecture baseline  
**Target platform:** Minecraft Java Edition 26.2 + Fabric  
**Primary language:** Java  
**Primary audience:** 1–4 players on private/family Fabric servers  
**Related document:** `docs/PRD.md`

---

## 1. Purpose

This document defines the initial technical architecture for the Minecraft Golf mod described in the PRD.

The architecture is intentionally optimized for:

- a small but genuinely playable multiplayer MVP,
- server-authoritative gameplay,
- deterministic and testable golf simulation,
- Minecraft-native world interaction,
- clean separation between game rules and Fabric integration,
- incremental development by AI coding agents,
- easy local server/client playtesting,
- and future growth without prematurely building public-server infrastructure.

The architectural goal is not to create a generic game framework. It is to create the smallest clean structure that supports a fun Minecraft golf game and remains easy to evolve.

---

# 2. Architecture Principles

## 2.1 Server Authoritative

The Minecraft server is the source of truth for gameplay state.

The server owns:

- active rounds,
- participating players,
- active hole,
- stroke counts,
- golf-ball ownership,
- authoritative ball position and velocity,
- shot validation,
- shot execution,
- hole completion,
- penalties,
- stroke limits,
- course boundaries,
- scorecards,
- and round completion.

Clients provide player intent and presentation.

A client may request:

```text
Select club
Change aim
Start swing
Set power
Set accuracy
Take shot
Pick up ball
```

The client must not be allowed to submit an arbitrary resulting ball position or score.

This prevents client disagreement and keeps multiplayer behavior predictable.

---

## 2.2 Keep Golf Logic Separate From Minecraft Integration

The core golf simulation and rules should be represented by plain Java domain objects wherever practical.

Avoid embedding all gameplay logic directly into:

- Fabric event callbacks,
- Minecraft entity classes,
- networking handlers,
- renderers,
- or Mixins.

Preferred direction:

```text
Minecraft / Fabric Integration
          │
          ▼
     Golf Application
          │
          ▼
       Golf Core
```

The deeper a class is in the architecture, the less it should know about Minecraft.

This is especially important for AI-agent development because it gives agents small, well-defined, testable units of work.

---

## 2.3 Minecraft Remains Responsible for the World

The mod should not create a parallel terrain or collision engine.

Minecraft remains responsible for:

- world geometry,
- blocks,
- block collision shapes,
- entity positioning,
- chunks,
- dimensions,
- player movement,
- and most world interactions.

The golf simulation adds golf-specific interpretation on top of those systems.

For example:

```text
Minecraft collision says:
ball contacted a block.

Golf surface system says:
that block behaves like sand.

Golf physics says:
reduce horizontal velocity and bounce.
```

---

## 2.4 Prefer Fabric APIs Before Mixins

Use Fabric API hooks and normal Minecraft extension points wherever possible.

Mixins should be used only when the required behavior cannot reasonably be implemented through public APIs, events, registered content, networking, or normal subclassing.

Every Mixin should have:

- a narrowly defined purpose,
- an explanatory comment,
- and preferably a test or explicit manual verification step.

Avoid broad invasive Mixins early in development.

---

## 2.5 Configuration Over Hard-Coding

Values likely to change during playtesting should not be buried throughout game logic.

Examples include:

- club distances,
- club loft,
- ball drag,
- gravity multiplier,
- restitution/bounce,
- rolling friction,
- stopping threshold,
- surface behavior,
- shot-meter timing,
- accuracy dispersion,
- course scale,
- and maximum strokes.

These values should live in centralized definitions or configuration structures.

The MVP does not require an end-user settings UI.

---

# 3. Technology Baseline

## Minecraft

Target Minecraft Java Edition **26.2** for the initial project.

The project should pin a specific Minecraft version rather than use floating version ranges during development.

## Fabric Loader

Use the current stable Fabric Loader compatible with the selected Minecraft version.

## Fabric API

Use Fabric API for supported lifecycle, event, item, entity, command, networking, rendering, and other mod integration points.

## Fabric Loom

Use the current Loom plugin recommended by Fabric for Minecraft 26.2.

Minecraft 26.x uses the newer unobfuscated development workflow, so the project should follow the current Fabric 26.2 template rather than older Yarn/remapping examples intended for Minecraft 1.21.x and earlier.

## Java

Use **JDK 25**, matching the current Fabric development requirements for the Minecraft 26.x generation.

## Build Tool

Use **Gradle** through the checked-in Gradle Wrapper.

Developers and agents should use:

```bash
./gradlew ...
```

rather than depending on a machine-installed Gradle version.

## IDE

The architecture should not depend on a particular IDE.

IntelliJ IDEA is the most convenient Java/Fabric IDE, but Aaron's agent-driven terminal workflow and editors should work using Gradle tasks alone.

---

# 4. Proposed Repository Structure

Keep the MVP as a **single Fabric mod project** rather than starting with a multi-module Gradle build.

Recommended structure:

```text
minecraft-golf/
├── README.md
├── PRD.md
├── ARCHITECTURE.md
├── AGENTS.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/apdev/golf/
│   │   │       ├── MinecraftGolfMod.java
│   │   │       ├── golf/
│   │   │       ├── course/
│   │   │       ├── round/
│   │   │       ├── ball/
│   │   │       ├── club/
│   │   │       ├── surface/
│   │   │       ├── item/
│   │   │       ├── entity/
│   │   │       ├── command/
│   │   │       ├── network/
│   │   │       ├── persistence/
│   │   │       └── config/
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       ├── assets/<mod-id>/
│   │       └── data/<mod-id>/
│   ├── client/
│   │   ├── java/
│   │   │   └── com/apdev/golf/client/
│   │   │       ├── MinecraftGolfClient.java
│   │   │       ├── input/
│   │   │       ├── hud/
│   │   │       ├── render/
│   │   │       └── camera/
│   │   └── resources/
│   └── test/
│       └── java/
│           └── com/apdev/golf/
│               ├── golf/
│               ├── round/
│               ├── course/
│               └── surface/
├── dev-server/
│   ├── docker-compose.yml
│   ├── mods/
│   │   └── .gitkeep
│   ├── sync-mod.sh
│   ├── reset-world.sh
│   └── README.md
├── scripts/
│   ├── dev-server-build.sh
│   ├── dev-server-up.sh
│   ├── dev-server-down.sh
│   └── dev-server-logs.sh
└── run/                    # generated/local only; gitignored
```

The exact Java package can be changed during repo creation, but it should be stable once development starts.

### Why a single module?

A separate `golf-core` Gradle module is tempting, but it adds build and source-set complexity before we know whether the project needs it.

Instead, establish architectural separation using Java packages and dependency discipline first.

If the simulation later becomes sufficiently independent or reusable, it can be extracted into a module without redesigning the domain model.

---

# 5. Source-Set Separation

Use Fabric/Loom client source-set separation.

Server-safe gameplay code belongs in `src/main`.

Client-only code belongs in `src/client`.

Client-only examples include:

- HUD rendering,
- shot-meter rendering,
- aim visualization,
- client input capture,
- client-side camera following,
- ball rendering helpers,
- and future cinematic camera logic.

The dedicated server must be able to load the mod without accidentally loading Minecraft client classes.

This should be treated as a CI/build requirement from the beginning.

---

# 6. Major Runtime Components

```text
┌──────────────────────────────────────────────────────┐
│                     Minecraft Client                  │
│                                                      │
│ Input → Shot Meter → HUD → Rendering → Camera        │
└───────────────────────┬──────────────────────────────┘
                        │ Fabric custom payloads
                        ▼
┌──────────────────────────────────────────────────────┐
│                     Minecraft Server                  │
│                                                      │
│  Network Handlers                                    │
│        │                                             │
│        ▼                                             │
│  Round Manager ────── Course Manager                 │
│        │                    │                        │
│        ▼                    ▼                        │
│  Shot Service        Boundary / Surface Resolver     │
│        │                                             │
│        ▼                                             │
│  Golf Ball Entity ↔ Ball Physics                     │
│        │                                             │
│        ▼                                             │
│  Score / Hole Completion / Penalties                 │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

# 7. Golf Core

The golf core represents rules and calculations that do not require rendering or Fabric callbacks.

Candidate domain objects:

```text
ClubDefinition
ShotInput
ShotParameters
ShotResult
BallState
SurfaceDefinition
HoleDefinition
CourseDefinition
PlayerHoleState
PlayerRoundState
RoundState
Scorecard
```

Where possible these should be immutable records/value objects.

Example conceptual API:

```java
ShotParameters resolveShot(
    ClubDefinition club,
    ShotInput input,
    BallState ball,
    SurfaceDefinition surface
);
```

The result may define an initial velocity and other shot state that the server-owned golf ball entity then uses.

The core should avoid dependencies on Minecraft classes such as `ServerPlayer`, `Level`, or entity classes unless there is a compelling reason.

Vectors may either use a tiny internal value type or Minecraft's vector type at the integration boundary. The preferred direction is an internal golf-domain vector if that remains simple.

---

# 8. Shot System

## 8.1 Client Responsibilities

The client handles the interactive three-click swing experience.

The client tracks temporary UI state such as:

```text
IDLE
AIMING
POWER_METER
ACCURACY_METER
SUBMITTING
```

The client sends the finalized shot intent to the server.

Example logical payload:

```text
player
ball id
club id
aim yaw
aim pitch
power value        0.0–1.0
accuracy value    -1.0–1.0
```

The exact wire format should use Fabric's current typed custom-payload networking APIs.

## 8.2 Server Validation

Before executing a shot, the server validates:

- player is in an active round,
- player has not completed the hole,
- requested ball belongs to the player,
- ball is currently stationary,
- player is allowed to hit,
- club exists and is allowed,
- power and accuracy values are within legal ranges,
- ball is within the active hole boundary,
- and the request is not an obvious duplicate/replay.

Ready Golf means no turn-order check is normally required.

## 8.3 Shot Resolution

The server converts shot intent into physical parameters.

Conceptually:

```text
club definition
+ aim direction
+ power timing
+ accuracy timing
+ current ball lie
+ elevation/world context
---------------------------
initial velocity
```

For MVP, avoid complicated stochastic dispersion.

A repeatable input should produce a repeatable result given the same relevant state.

---

# 9. Ball Architecture

## 9.1 Golf Ball as a Minecraft Entity

The active golf ball should be implemented as a custom Minecraft entity.

Reasons:

- natural server-side position synchronization,
- world/chunk integration,
- block collision access,
- entity lifecycle,
- rendering support,
- multiplayer visibility,
- and easier ownership/state association.

The entity is authoritative on the server.

## 9.2 Ball State

Important server-side state includes:

```text
ball UUID/entity id
owner player UUID
round id
hole number
position
velocity
stationary/moving state
last legal shot position
last boundary-safe position
stroke association
```

Do not store scoring authority solely on the ball entity. Round state belongs to the round manager.

## 9.3 Physics Tick

The ball updates on Minecraft's server tick.

At a high level:

```text
1. Read current velocity.
2. Apply gravity while airborne.
3. Move through Minecraft collision handling.
4. Detect block/world collision.
5. Resolve bounce/restitution.
6. Resolve surface friction while grounded.
7. Apply rolling resistance.
8. Detect hazards/boundaries.
9. Detect cup interaction.
10. Stop when velocity remains below threshold.
```

Minecraft runs at 20 ticks per second under normal conditions, so golf physics must feel stable at that simulation rate.

If normal entity movement is insufficient for fast shots, implement swept/sub-step collision checks rather than simply increasing entity size.

This should be validated early because high-speed tunneling is one of the highest technical risks in the project.

---

# 10. Physics Strategy

The goal is **arcade-realistic predictability**, not a professional rigid-body simulator.

Use a deliberately small set of tunable constants.

Potential parameters:

```text
gravity scale
air drag
bounce restitution
ground rolling friction
minimum moving velocity
maximum initial ball speed
surface modifiers
```

Avoid pulling in an external physics engine for MVP.

External physics libraries would introduce complexity around reconciling their collision world with Minecraft's block geometry.

Minecraft should remain the collision world.

### Fast-ball collision risk

Driver shots may move farther than one collision-relevant unit per tick.

The implementation must test for skipped collisions.

Preferred escalation path:

1. Start with Minecraft entity movement/collision.
2. Measure behavior with maximum-speed shots.
3. If tunneling occurs, subdivide movement within a server tick or ray/sweep the path.
4. Resolve the first collision encountered.

Do not prematurely write a complete custom collision engine.

---

# 11. Surface System

Block-to-golf behavior should be data-driven.

Conceptual surface definition:

```java
record SurfaceDefinition(
    String id,
    double rollingFriction,
    double bounceMultiplier,
    double landingHorizontalRetention,
    double shotPowerMultiplier,
    boolean hazard
) {}
```

MVP may only use:

```text
rollingFriction
bounceMultiplier
landingHorizontalRetention
shotPowerMultiplier
hazard
```

Later versions can activate lie-based shot modifiers.

## Surface Resolution

A `SurfaceResolver` maps the block beneath or contacted by the golf ball to a logical golf surface.

Example defaults:

```text
grass-like → fairway/default
sand       → bunker
water      → water hazard
ice        → ice
slime      → slime
honey      → honey
other      → generic
```

Prefer Minecraft block tags over giant lists of specific block IDs where practical.

Future course configuration may override block-based surface interpretation without changing the physics engine.

---

# 12. Clubs

Clubs are registered Minecraft items backed by reusable `ClubDefinition` data.

MVP club set:

```text
Driver
Fairway Wood
Long Iron
Mid Iron
Short Iron
Wedge
Putter
```

A club definition should include values such as:

```text
id
display name
base speed / distance
launch angle
accuracy sensitivity
putting flag
melee behavior
```

Avoid implementing every real-world iron number in MVP.

## Club Items as Weapons

Club items can define basic melee attack behavior through normal Minecraft item mechanics.

This system must remain separate from golf shot calculations.

Swinging a club as a weapon must never mutate golf scoring or accidentally trigger a golf shot.

---

# 13. Cup and Tee

## Cup

The cup should be a custom golf-specific block and/or associated block entity only if state is required.

The cup must provide a reliable server-side hole-out detection region.

A successful hole-out should trigger:

```text
ball enters valid cup region
→ player hole state completes
→ ball stops/is captured
→ score is finalized
→ audiovisual event sent/displayed
```

The visual flag/pin may be represented separately if that makes rendering and collision easier.

## Tee

For MVP, the tee position is primarily course metadata.

A decorative tee marker block may exist, but the architecture should not require every course to use a physically special tee block.

---

# 14. Courses

A `CourseDefinition` describes an existing Minecraft-world course rather than generating terrain.

Conceptual structure:

```text
CourseDefinition
├── id
├── displayName
├── dimension/world identifier
└── holes[]
     ├── number
     ├── par
     ├── tee position
     ├── cup position
     └── boundary
```

## MVP Course Configuration

Course-authoring commands are deferred for MVP. The bounded M8 authoring slice may
add operator-facing commands for defining metadata on existing terrain. Authoring
must remain server-authoritative and use Minecraft-native/local persistence; it must
not introduce a visual editor, arbitrary world generation, external storage, or a
course-sharing service.

For MVP, courses can be loaded from mod/server configuration data.

Prefer a human-readable format, likely JSON, using Minecraft/Fabric-supported serialization where reasonable.

Example conceptual configuration:

```json
{
  "id": "family_test",
  "name": "Family Test Course",
  "dimension": "minecraft:overworld",
  "holes": [
    {
      "number": 1,
      "par": 4,
      "tee": [100, 70, 100],
      "cup": [165, 65, 125],
      "boundary": {
        "min": [80, 50, 80],
        "max": [190, 100, 150]
      }
    }
  ]
}
```

This example is illustrative rather than a frozen schema.

---

# 15. Hole Boundaries

MVP uses an axis-aligned 3D bounding box per hole.

This intentionally avoids polygon editing, corridor graphs, or arbitrary region systems.

```text
min X/Y/Z
max X/Y/Z
```

A ball crossing the boundary becomes out of bounds.

The boundary should allow generous vertical range so cliffs, caves, towers, and dramatic elevation changes remain possible.

### Future

If rectangular boundaries prove too restrictive, the interface should allow later implementations such as:

- multiple boxes,
- polygons,
- composed regions,
- or course-builder-defined zones.

Do not build these for MVP.

---

# 16. Round Management

A server-side `RoundManager` owns active rounds.

A round has an explicit lifecycle.

Suggested state machine:

```text
LOBBY
  ↓
STARTING_HOLE
  ↓
PLAYING_HOLE
  ↓
HOLE_COMPLETE
  ↓
STARTING_HOLE   (next hole)
  ↓
ROUND_COMPLETE
```

Each participating player has independent hole state during `PLAYING_HOLE`.

Example:

```text
RoundState
├── roundId
├── courseId
├── currentHole
├── status
└── players
     └── PlayerRoundState
          ├── playerUuid
          ├── totalScore
          └── currentHoleState
               ├── strokes
               ├── completed
               ├── pickedUp
               └── ballEntityId
```

Ready Golf does not serialize players into a strict shot order.

The hole completes when every active golfer has either:

- holed out,
- picked up,
- or reached the stroke limit.

---

# 17. Stroke Counting

A stroke is committed by the server when a valid golf shot is accepted and executed.

Do not count UI attempts, cancelled swings, or rejected network requests.

Default maximum strokes:

```text
(par × 2) + 2
```

This should be configurable at the round/course/server level later.

For MVP, a global/default configuration is sufficient.

---

# 18. Penalties and Recovery

Keep MVP recovery deliberately simple.

## Water

When the ball enters water:

```text
+1 penalty stroke
return to last legal shot position
```

## Out of Bounds

When the ball leaves the hole boundary:

```text
+1 penalty stroke
return to last legal shot position
```

This is intentionally not a full implementation of official golf drop rules.

The architecture should represent penalties explicitly so future recovery strategies can change without rewriting round scoring.

Example:

```text
PenaltyType.WATER
PenaltyType.OUT_OF_BOUNDS
```

---

# 19. Persistence

## MVP Philosophy

Do not introduce an external database.

Minecraft already provides server/world persistence mechanisms suitable for the initial product.

Persist only what is necessary.

### Course definitions

Loaded from server-side configuration/data files.

### Active rounds

MVP may initially keep active rounds in memory if clean handling is defined for server shutdown/restart.

Before calling V1 complete, active or recoverable round state can be moved into Minecraft saved data if desired.

### Historical statistics

Not MVP.

No database, cloud API, account service, leaderboard backend, or web service should be introduced.

---

# 20. Networking

Use Fabric's current custom-payload networking APIs.

Network messages should be small, typed, and purpose-specific.

Potential client → server messages:

```text
GolfShotRequest
GolfPickUpRequest
GolfRoundJoinRequest
GolfRoundStartRequest
```

Potential server → client messages:

```text
RoundStateUpdate
PlayerHoleStateUpdate
ShotAccepted
ShotRejected
HoleCompleted
RoundCompleted
HudStateUpdate
```

Do not send continuous authoritative ball positions through custom packets if Minecraft entity synchronization already provides what is needed.

Custom networking should communicate golf semantics, not reinvent entity networking.

---

# 21. Client HUD

The client HUD owns presentation only.

MVP HUD should display enough information to make a shot:

```text
selected club
estimated club distance
current hole / par
current stroke count
power meter
accuracy meter
aim indicator
```

The approximate landing-area indicator was intentionally removed from MVP scope.

This avoids needing a client-side trajectory prediction system before the server physics have stabilized.

It can be added later by sharing deterministic shot math with the client or sending prediction data from the server.

---

# 22. Input Model

Avoid replacing Minecraft's normal movement controls.

Golf input should be activated while the player is preparing to hit their owned stationary ball.

A likely interaction model:

```text
Approach ball
→ activate golf/shot mode
→ choose club
→ aim with camera
→ first click starts meter
→ second click locks power
→ third click locks accuracy
→ send shot request
→ exit shot mode
```

Exact mouse/key bindings should be validated during the prototype rather than frozen in architecture.

The system should allow cancelling shot mode without taking a stroke.

---

# 23. Camera

## MVP

Implement a simple client-only ball-follow experience after a shot.

Favor robustness over cinematic presentation.

Possible first implementation:

- track the player's ball entity,
- orient or temporarily follow its movement,
- return to the player after the ball stops,
- allow cancelling follow mode.

The architecture should keep camera code entirely client-side.

## Post-MVP

Cinematic camera behavior may later include:

- dynamic offsets,
- landing framing,
- easing,
- camera cuts,
- and automatic return transitions.

Do not build a cinematic camera framework for MVP.

---

# 24. Player and Mob Collision

Golf balls should collide with Minecraft world blocks.

For MVP:

- do not allow another golfer's body to intentionally block a golf shot,
- do not make player collision essential to ball physics,
- keep mob-ball interaction out of the MVP critical path.

Mob interaction and survival-round options are post-MVP configuration concerns.

This avoids coupling basic golf physics to Minecraft combat/entity edge cases before the shot experience works.

Golf clubs may still work as ordinary melee items.

---

# 25. Threading

Minecraft gameplay state must be mutated on the appropriate server thread.

Do not introduce asynchronous mutation of:

- entities,
- rounds,
- world blocks,
- or score state.

No asynchronous framework is required for MVP.

The simulation is small enough to execute inside normal server ticks.

---

# 26. Performance Expectations

The primary target is four simultaneous golfers.

The architecture should remain efficient, but do not optimize for hundreds of concurrent golf balls before profiling indicates a need.

Likely hot path:

```text
golf ball tick
→ collision
→ surface lookup
→ movement update
```

Performance rules:

- avoid scanning large world regions every tick,
- resolve only blocks/entities relevant to the moving ball,
- cache immutable definitions,
- stop physics work as soon as a ball is stationary,
- and rely on Minecraft entity/chunk systems rather than duplicating spatial indexing.

---

# 27. Testing Strategy

Testing is a first-class architectural requirement because the project will be developed heavily through coding agents.

Use three levels of testing.

## 27.1 Plain JVM Unit Tests

Most golf-core behavior should be testable without launching Minecraft.

Examples:

- power calculation,
- accuracy deviation,
- club parameter resolution,
- scoring,
- Double Par + 2 calculation,
- round state transitions,
- penalty application,
- boundary mathematics,
- surface modifier calculations.

These tests should be fast and should make up the majority of automated coverage.

## 27.2 Minecraft/Fabric Game Tests

Use Minecraft/Fabric's supported automated game-testing capabilities for integration behavior that requires a world.

Examples:

- golf-ball entity can spawn,
- ball collides with blocks,
- sand changes movement,
- water triggers a hazard,
- boundary crossing triggers recovery,
- cup detects a valid hole-out,
- owner association survives entity lifecycle as required.

Game tests should be targeted rather than attempting to automate every gameplay scenario.

## 27.3 Manual Playtests

Some requirements are inherently experiential.

Manual testing is required for:

- shot meter feel,
- camera comfort,
- driver trajectory,
- bounce satisfaction,
- putting feel,
- multiplayer pacing,
- visual ball identification,
- and overall fun.

These should be tracked using short repeatable playtest checklists instead of relying only on memory.

---

# 28. Local Development Environments

Use two complementary local development environments. Neither replaces the other.

## 28.1 Loom / Gradle Development Environment

Fabric Loom remains the primary inner-loop development environment.

Use it for:

- fast client iteration,
- entity rendering,
- HUD/input development,
- physics tuning,
- command development,
- single-player/integrated-server testing,
- and quick debugging.

Primary commands:

```bash
./gradlew test
./gradlew runClient
./gradlew runServer
```

Agents should prefer this workflow while actively implementing a feature because it avoids unnecessary Docker rebuild/restart overhead.

## 28.2 Docker Fabric Integration Server

The repository should also include a dedicated Docker-based Fabric server for realistic integration and family multiplayer testing.

Use `itzg/minecraft-server` or an equivalent well-maintained image configured with:

```text
TYPE=FABRIC
VERSION=<the exact Minecraft version pinned by this repo>
EULA=TRUE
```

The Docker server is intended for:

- dedicated-server boot verification,
- 2–4 player LAN testing,
- client/server networking validation,
- Ready Golf concurrency testing,
- persistence and world-state testing,
- course testing,
- detecting accidental client-only class usage on the server,
- and family playtests.

The dev server should expose Java Edition on the normal local port:

```text
25565/tcp
```

Bedrock compatibility, Geyser, Floodgate, Paper plugins, proxy infrastructure, and public hosting are explicitly outside the MVP development environment.

## 28.3 Dev-Server Layout

Recommended structure:

```text
dev-server/
├── docker-compose.yml
├── mods/
│   └── .gitkeep
├── sync-mod.sh
├── reset-world.sh
└── README.md
```

`dev-server/data/` may be used instead of a named Docker volume when local file visibility is beneficial, but persistent runtime data must be gitignored.

Prefer a Docker named volume by default unless direct world-file editing becomes useful during course development.

## 28.4 Built-Mod Synchronization

Do not mount the entire Gradle `build/libs` directory directly into `/data/mods`. Gradle may create multiple artifacts and intermediary JARs.

Use an explicit synchronization step that selects the final distributable/remapped Fabric mod JAR and copies it to a stable filename:

```text
build/libs/<versioned-final-mod>.jar
        ↓
dev-server/mods/minecraft-golf.jar
        ↓
Docker mount
        ↓
/data/mods/minecraft-golf.jar
```

Conceptual workflow:

```bash
./gradlew build
./dev-server/sync-mod.sh
cd dev-server
docker compose restart minecraft
```

The sync script must fail clearly if:

- no distributable JAR exists,
- more than one candidate JAR is ambiguous,
- or the expected build did not complete.

Do not silently copy a sources/dev/intermediary JAR.

## 28.5 Docker Compose Expectations

The exact Compose file should be created during project scaffolding, but it should conceptually remain small:

```yaml
services:
  minecraft:
    image: itzg/minecraft-server:latest
    container_name: minecraft-golf-dev
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: FABRIC
      VERSION: "<pinned version>"
      MEMORY: "4G"
      MODE: survival
      DIFFICULTY: peaceful
      ONLINE_MODE: "true"
    volumes:
      - minecraft-golf-data:/data
      - ./mods:/data/mods-local:ro
```

If the selected image has a supported Fabric-mod copy/mount convention, use it. Otherwise add a small initialization step that copies the stable local mod JAR into `/data/mods`.

The Compose configuration should include a Minecraft-aware health check when practical.

## 28.6 Resettable Test World

Provide a deterministic way to discard the development world and start fresh.

Example:

```bash
./dev-server/reset-world.sh
```

The reset operation should:

1. stop the development server,
2. remove only known local development world/runtime data,
3. preserve source-controlled configuration and the built mod,
4. recreate or allow Minecraft to regenerate a clean world,
5. and restart only when explicitly requested by the script contract.

The script must guard against deleting arbitrary paths.

This reset capability is important for testing:

- entity persistence,
- course initialization,
- corrupted/old development state,
- and reproducible agent verification.

## 28.7 LAN Family Testing

When the Docker server is running locally, Aaron's machine can connect through:

```text
localhost:25565
```

Other machines on the same LAN can connect using the host machine's LAN IP and port `25565`, assuming the host firewall permits it.

Each connecting client must run a compatible Fabric client with the Minecraft Golf mod and any required client-side dependencies installed.

The Docker server is not intended to be a production/public deployment architecture. It is a local integration and playtest environment.

---

# 29. Required Developer Commands

The final repo should expose simple Gradle commands for agents and humans.

At minimum:

```bash
./gradlew build
./gradlew test
./gradlew runClient
./gradlew runServer
```

The repo should also provide simple Docker integration helpers, either directly under `dev-server/` or through root-level scripts:

```bash
./scripts/dev-server-build.sh
./scripts/dev-server-up.sh
./scripts/dev-server-logs.sh
./scripts/dev-server-down.sh
./dev-server/reset-world.sh
```

Exact names may change during scaffolding, but there should be one documented happy path for rebuilding the mod and restarting the dedicated Fabric test server.

If Fabric game-test tasks differ for the selected template/version, document their exact command in `README.md` and `AGENTS.md`.

The baseline agent verification should be:

```bash
./gradlew build
```

An agent should not claim a task is complete while the build is failing unless the failure is explicitly documented as pre-existing and unrelated.

---

# 30. Agent-Driven Development Rules

The repo should include an `AGENTS.md` containing implementation guidance.

At minimum, it should instruct coding agents to:

1. Read `docs/PRD.md` and `docs/ARCHITECTURE.md` before major changes.
2. Keep server authority intact.
3. Keep client-only Minecraft classes out of server source sets.
4. Prefer Fabric APIs over Mixins.
5. Keep golf-domain logic independent of Fabric when practical.
6. Add or update tests for deterministic logic.
7. Run `./gradlew build` before completion.
8. For server-impacting work, verify the produced mod can load on the Docker dedicated Fabric server when practical.
9. Treat dedicated-server crashes caused by client-only class leakage as blocking failures.
10. Avoid introducing libraries or infrastructure without architectural justification.
11. Avoid expanding scope beyond the current milestone.
12. Record architectural deviations explicitly rather than silently implementing them.

Development tasks should be small enough that an agent can implement and verify them in one focused session.

---

# 31. Suggested Implementation Sequence

The architecture supports the following development order.

## Phase 0 — Project Foundation

- Fabric project scaffolding
- Java/Gradle configuration
- client/main source sets
- mod registration
- basic test infrastructure
- CI build
- Docker Fabric development server
- mod sync/restart workflow
- resettable development world
- `AGENTS.md`

Success condition:

> Empty mod loads in the Loom client/server environment and in the Docker dedicated Fabric server, and CI/build is green.

---

## Phase 1 — Golf Ball Physics Prototype

- register golf-ball entity
- render simple ball
- spawn ball through dev command
- launch ball with fixed velocity
- gravity
- world collision
- bounce
- rolling friction
- stopping threshold
- basic surface behavior

Success condition:

> A developer can launch a ball through a Minecraft world and its flight, collision, bounce, roll, and stopping behavior are fun enough to continue.

This is the first major go/no-go milestone.

---

## Phase 2 — Clubs and Shot Resolution

- club definitions
- club items
- shot calculation
- camera aiming
- basic shot mode
- server validation
- club-specific trajectories
- initial putter behavior

Success condition:

> A player can deliberately aim and hit useful Driver, Iron/Wedge, and Putter shots.

---

## Phase 3 — Three-Click Swing + HUD

- swing state machine
- power meter
- accuracy meter
- shot submission networking
- club display
- hole/stroke HUD placeholders

Success condition:

> Shot outcomes respond consistently to player timing and feel skill-based.

---

## Phase 4 — Hole and Course Model

- cup
- tee coordinate
- course JSON
- 3-hole test course
- rectangular boundaries
- water hazard
- OOB recovery
- hole-out detection

Success condition:

> One player can complete three configured holes under actual golf rules.

---

## Phase 5 — Multiplayer Round

- RoundManager
- player round state
- player-owned balls
- Ready Golf
- 1–4 player join/start flow
- stroke counting
- Double Par + 2
- Pick Up
- hole advancement
- final scorecard

Success condition:

> Four players can complete the three-hole MVP course together.

This is the formal PRD MVP milestone.

---

## Phase 6 — MVP Polish

- simple ball-follow camera
- improved golf-ball identification
- sounds
- particles
- basic UI polish
- playtest tuning
- bug fixes

Success condition:

> The family wants to immediately play another round.

---

# 32. Continuous Integration

Use GitHub Actions for basic verification from the beginning.

Initial CI should:

```text
checkout
setup required JDK
run Gradle build
cache Gradle dependencies where appropriate
upload build artifacts optionally
```

Do not deploy or publish the mod automatically during MVP development.

CI does not need to boot Docker on every commit initially. Local Docker dedicated-server boot is the integration gate for server-impacting milestones. If dedicated-server regressions become common, add an automated server-start smoke test later.

Releases can later attach the built Fabric JAR to GitHub Releases and optionally publish to Modrinth/CurseForge if desired.

Distribution platform choice is not part of the runtime architecture.

---

# 32. Dependencies

Keep third-party dependencies minimal.

Expected foundational dependencies:

- Minecraft
- Fabric Loader
- Fabric API
- Fabric Loom build tooling
- JUnit/test dependencies supplied or configured for the project

Do not add an external:

- physics engine,
- database,
- dependency injection framework,
- networking library,
- web framework,
- or serialization framework

without a demonstrated need.

Minecraft/Fabric/Java facilities should be preferred.

---

# 33. Configuration

Separate three categories of configuration.

## Game tuning

Developer-controlled values used to tune gameplay:

```text
club parameters
ball constants
surface constants
shot meter timing
```

## Server gameplay configuration

User-facing server options, initially minimal:

```text
max strokes rule
allow pick up
future survival/mob behavior
future teleport behavior
```

## Course configuration

Per-course and per-hole metadata:

```text
course identity
world/dimension
tee
cup
par
boundary
```

Avoid one giant global configuration object.

---

# 34. Logging and Diagnostics

Use standard mod logging through SLF4J/Fabric's normal logging environment.

Useful diagnostic logging includes:

- round started/completed,
- player joined/left round,
- invalid shot rejected,
- hazard recovery,
- hole completed,
- malformed course definition,
- ball physics safety condition triggered.

Avoid per-tick INFO logging from golf balls.

Detailed physics logging should be behind debug-level logging or a development flag.

A future developer command such as `/golf debug ball` may expose:

```text
position
velocity
surface
owner
moving state
```

but is not required for the formal MVP.

---

# 35. Failure and Edge-Case Handling

The server must fail safely when gameplay state becomes inconsistent.

Examples:

## Player disconnects

Remove or suspend the player from the active round without blocking remaining golfers from completing the hole.

Exact reconnect semantics can remain simple for MVP.

## Ball entity disappears

The server should be able to respawn/recover the player's ball from authoritative round state rather than permanently breaking the round.

## Ball becomes stuck

Provide a safe recovery path, ultimately through Pick Up or a development/admin recovery mechanism.

## Chunk unload

Active shots should not silently disappear because a ball moves near a chunk boundary.

This must be evaluated during physics prototyping. Avoid force-loading broad course regions unless testing proves it necessary.

## Server shutdown

An active MVP round may be cancelled on restart if persistence has not yet been implemented, but this behavior must be explicit and must not corrupt course configuration or worlds.

---

# 36. Security / Trust Model

This is not an internet service, but multiplayer requests still require validation.

Never trust client-provided:

- score,
- ball position,
- shot result,
- ownership,
- club stats,
- or completion state.

Clients may provide bounded input intent such as meter timing values and aim direction.

The server resolves authoritative gameplay from that intent.

This also reduces accidental desynchronization between clients.

---

# 37. Compatibility Strategy

The initial project targets one specific Minecraft/Fabric version.

Do not attempt multi-version compatibility during MVP.

Minecraft mod APIs change frequently, and supporting multiple Minecraft versions would significantly increase agent and maintenance complexity.

Upgrade strategy:

```text
finish/stabilize feature on pinned version
→ create explicit Minecraft upgrade task
→ update toolchain/API
→ fix build and game tests
→ manually verify gameplay
```

Compatibility layers should only be considered if the mod gains enough users to justify them.

---

# 38. Distribution Strategy

During development:

- build locally,
- run local Fabric client/server instances,
- share JAR directly with family test machines as needed.

Later:

- GitHub Releases can provide versioned artifacts,
- Modrinth can be considered for public distribution,
- CurseForge can also be supported if useful.

The mod loader remains Fabric regardless of which distribution platform hosts the JAR.

---

# 39. Deferred Architecture

The following should deliberately remain undesigned until needed:

- public matchmaking,
- dedicated backend services,
- cloud persistence,
- global accounts,
- global leaderboards,
- course marketplace,
- progression service,
- analytics platform,
- anti-cheat beyond basic server authority,
- arbitrary polygonal course editor,
- replay storage,
- cross-server tournaments,
- multi-loader Fabric/NeoForge support,
- multi-version Minecraft support.

Do not create abstractions for these hypothetical systems during MVP work.

---

# 40. Key Technical Risks

## Risk 1 — High-Speed Ball Collision

A driver may move the ball far enough per tick to skip narrow geometry.

**Mitigation:** prototype maximum-speed ball flight immediately and add movement substeps/sweeps only if Minecraft's normal collision behavior is insufficient.

---

## Risk 2 — Physics Feel

Technically correct movement may still feel bad.

**Mitigation:** isolate tunable parameters, create repeatable test shots, and schedule manual playtesting early rather than waiting until multiplayer is complete.

---

## Risk 3 — Client/Server Desynchronization

Attempting to simulate authoritative ball physics independently on both sides could create disagreement.

**Mitigation:** server owns the ball; leverage normal Minecraft entity synchronization. Client-side prediction is optional polish, not authority.

---

## Risk 4 — Overengineering Course Systems

Minecraft world/region tooling can quickly become a project by itself.

**Mitigation:** MVP course data is manually configured and boundaries are simple boxes.

---

## Risk 5 — Client-Only Class Leakage

Rendering/camera code accidentally referenced from common/server code can crash a dedicated server.

**Mitigation:** strict client source-set separation plus dedicated-server launch/build verification.

---

## Risk 6 — Scope Creep

Golf naturally suggests many systems: wind, spin, carts, stats, progression, course editors, tournaments, cosmetics, and advanced cameras.

**Mitigation:** use the PRD's MVP definition as a hard gate. The first product question remains whether four people enjoy playing three holes together.

---

# 41. Architectural Decision Summary

| Area | Decision |
|---|---|
| Mod loader | Fabric |
| Minecraft target | 26.2 initially |
| Language | Java |
| JDK | 25 |
| Build | Gradle Wrapper + Fabric Loom |
| Repository | Single Fabric mod project |
| Authority | Server authoritative |
| Ball | Custom Minecraft entity |
| Physics | Custom golf rules layered on Minecraft collision |
| External physics engine | No |
| Multiplayer | Ready Golf first |
| Player target | 1–4, not hard-limited architecturally |
| Client/server split | Strict Fabric client source-set separation |
| Course storage | Human-readable server-side definitions |
| MVP boundary | Axis-aligned box per hole |
| Persistence | No external database |
| Networking | Fabric custom payloads + normal entity sync |
| Testing | JVM unit tests + Minecraft game tests + manual playtests |
| Mixins | Only when Fabric/public extension points are insufficient |
| Multi-Minecraft support | No during MVP |
| NeoForge support | No during MVP |
| Public backend | None |

---

# 42. MVP Architectural Definition of Done

The architecture has succeeded when the implementation supports the following without violating server authority or requiring external infrastructure:

> Four players join a Fabric server, start a configured three-hole course, each receive and control their own golf ball, select physical golf-club items, aim and execute three-click shots, watch server-simulated balls collide with Minecraft terrain and surfaces, play simultaneously using Ready Golf, incur simple water/OOB penalties, hole out into real cup targets, advance through all three holes, and receive a final scorecard.

Everything else is secondary until that loop is fun.

---

# 43. First Repository Handoff

Once `docs/PRD.md` and `docs/ARCHITECTURE.md` are copied into the new local repository, the first native agent session should **not** begin by implementing golf gameplay.

Its first job should be to establish the project foundation:

1. Scaffold a current Minecraft 26.2 Fabric mod from the official Fabric project conventions.
2. Pin compatible Minecraft, Fabric Loader, Fabric API, Loom, Gradle, and JDK versions.
3. Configure main/client source-set separation.
4. Establish the Java package and mod ID.
5. Add unit-test support.
6. Add a minimal GitHub Actions build.
7. Create `AGENTS.md` based on the rules in this architecture.
8. Verify `./gradlew build`.
9. Verify a development client launches.
10. Verify a dedicated development server launches with the mod installed.
11. Commit the clean foundation before starting golf-ball work.

After that foundation is green, Phase 1 should focus almost exclusively on the golf-ball physics prototype.

That sequence intentionally front-loads reproducibility so later AI-agent sessions have a stable environment in which to work.
