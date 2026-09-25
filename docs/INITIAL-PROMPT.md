# Minecraft Golf — Initial Repository Handoff Prompt

You are starting a new Minecraft Fabric mod project in a local directory named:

```text
minecraft-golf
```

The mod is named **Minecraft Golf**, with mod id `minecraft_golf`. Keep the mod name and
identifiers stable. A public server running the mod MUST NOT use the name “Minecraft
Golf”; it must have its own identity and credit `minecraft_golf` as the mod powering its
golf functionality.

Your task is to create the repository foundation and complete:

> **M0 — Project Setup**

Do **not** begin golf gameplay implementation.

---

# Required Planning Documents

The repository root already contains these authoritative planning documents:

```text
PRD.md
ARCHITECTURE.md
MILESTONES.md
```

Before doing any implementation work:

1. Read all three documents completely.
2. Create a repository-level documentation directory:

```text
docs/
```

3. Move the planning documents into it:

```text
docs/PRD.md
docs/ARCHITECTURE.md
docs/MILESTONES.md
```

4. Update any repository references you create so they point to the new `docs/` paths.

After the move, these files remain authoritative.

Priority of intent:

1. `docs/PRD.md` defines product behavior and scope.
2. `docs/ARCHITECTURE.md` defines technical architecture and engineering constraints.
3. `docs/MILESTONES.md` defines implementation order and milestone completion criteria.

If these documents conflict materially, stop and document the conflict rather than silently choosing a different architecture.

Do not substantially rewrite these planning documents during M0 merely to match implementation convenience.

---

# Primary Objective

Complete **M0 — Project Setup** exactly as defined in `docs/MILESTONES.md`.

At the end of this session, the repository should provide a clean and reproducible development harness for future Minecraft Golf development.

No actual golf mechanics should exist yet.

---

# Repository Initialization

If the current `minecraft-golf` directory is not already a Git repository:

```bash
git init
```

Use `main` as the primary branch.

Create a suitable `.gitignore` for:

- Gradle
- IntelliJ / editor files
- Minecraft development runtime data
- generated build output
- Docker development-server data
- temporary logs
- local environment files
- disposable development worlds

Do not ignore source-controlled configuration required for another developer or agent to reproduce the environment.

---

# Fabric Project Scaffold

Scaffold a modern Minecraft Java Edition Fabric mod using the versions and toolchain specified by `docs/ARCHITECTURE.md`.

Use the current supported Fabric ecosystem corresponding to the architecture's target Minecraft generation.

Pin exact dependency versions in project configuration.

The project must include:

- Gradle wrapper
- Fabric Loom
- Fabric Loader
- Fabric API
- Java toolchain
- `fabric.mod.json`
- common mod initializer
- client-only initializer
- resources
- logging

Use the stable mod identifier:

```text
minecraft_golf
```

Use the stable base package:

```text
com.prillcode.minecraftgolf
```

If Fabric conventions strongly favor another valid identifier format, document the reason before changing it.

---

# Source Separation

Establish strict client/server separation from the start.

The common/server portion of the project must not reference Minecraft client-only classes.

Client functionality should live in the Fabric/Loom client source set or other architecture-approved client-only location.

The repository structure should make accidental dedicated-server class loading failures difficult.

Follow `docs/ARCHITECTURE.md` closely.

---

# Initial Package Structure

Create a minimal package structure supporting the future architecture.

Do not populate packages with speculative abstractions.

Expected conceptual areas include:

```text
minecraftgolf/
├── MinecraftGolf.java
├── domain/
├── entity/
├── item/
├── course/
├── round/
├── network/
├── config/
└── client/
```

Exact source-set layout may differ where required by Fabric conventions.

Prefer empty packages only where useful; avoid meaningless placeholder classes solely to make directories exist.

---

# Initial Mod Behavior

The mod should do almost nothing.

On common initialization, emit a clear log message similar to:

```text
Minecraft Golf initialized
```

On client initialization, emit a separate client-only log message.

Do not register golf balls, clubs, blocks, commands, HUD elements, networking messages, courses, scoring, or round systems.

---

# Testing

Set up the unit-testing framework selected by the architecture.

Create at least one trivial plain-Java test proving the test harness works.

Example acceptable test target:

```text
a simple utility or placeholder domain value object
```

Do not create fake golf functionality solely to have something to test.

Running:

```bash
./gradlew test
```

must succeed.

---

# Build Verification

Running:

```bash
./gradlew build
```

must succeed from the repository root.

Ensure the final remapped mod JAR can be clearly identified.

Document its output location.

---

# Loom Development Environment

Verify:

```bash
./gradlew runClient
```

The Minecraft client must launch with the mod loaded.

Verify:

```bash
./gradlew runServer
```

The Fabric development server must boot successfully.

Confirm no client-only classes are loaded by the server environment.

Stop the development server cleanly after verification.

---

# Docker Fabric Integration Server

Create:

```text
dev-server/
```

This is the realistic dedicated-server and family/LAN testing environment.

Do not copy the complexity of older Paper/Spigot projects unless it is actually required.

Use a Fabric-compatible Docker Minecraft server approach, preferably based on the proven `itzg/minecraft-server` image unless current compatibility requires an alternative.

The server should use:

```text
Minecraft Java Edition
Fabric
the same Minecraft version as the mod project
port 25565
```

Development defaults should favor easy testing.

For example:

```text
creative mode
peaceful difficulty
reasonable memory allocation
persistent local server data
```

Do not add:

- Paper
- Spigot
- Bukkit plugins
- Geyser
- Floodgate
- Bedrock support
- Multiverse
- external databases
- cloud infrastructure

The Docker environment must remain focused on Fabric mod integration testing.

---

# Docker Host Portability

The Docker integration environment should work on either:

- Linux Docker
- Windows 11 Docker Desktop

Do not rely on Linux-host-specific absolute paths or host-specific Compose assumptions.

The household may eventually run this server continuously on an always-on Windows 11 mini-PC.

This does not change the target platform:

> Minecraft Java Edition + Fabric remains authoritative.

Shell helper scripts may target the primary Linux development environment, but Docker Compose itself should remain portable.

---

# Development Server Layout

Create a layout approximately like:

```text
dev-server/
├── docker-compose.yml
├── mods/
│   └── .gitkeep
├── README.md
└── data/
```

`data/` may be replaced with a named Docker volume if that produces a cleaner and more portable architecture.

Persistent runtime data must not accidentally be committed.

The exact layout should follow the architecture and favor cross-platform Docker compatibility.

---

# Mod Sync Workflow

Create a deterministic workflow that copies only the correct final remapped mod JAR into the Docker server's mod directory.

Do not mount the entire Gradle `build/libs` directory directly if it can contain multiple artifacts.

Preferred workflow:

```text
./gradlew build
        ↓
determine final remapped JAR
        ↓
copy to dev-server/mods/minecraft-golf.jar
        ↓
restart server
```

Create a script such as:

```text
scripts/dev-server-sync.sh
```

The script should:

1. fail on errors,
2. build or validate the expected JAR,
3. remove an old synced project JAR if necessary,
4. copy exactly one correct artifact,
5. print what was copied.

Keep the script simple and readable.

---

# Docker Lifecycle Scripts

Create lightweight repository scripts for common operations.

Suggested commands:

```text
scripts/dev-server-up.sh
scripts/dev-server-down.sh
scripts/dev-server-restart.sh
scripts/dev-server-logs.sh
scripts/dev-server-sync.sh
scripts/dev-server-reset.sh
```

If a smaller script set provides the same clarity, that is acceptable.

Scripts should work naturally on the primary Linux development environment.

Where practical, keep Docker Compose itself portable so the server can also be hosted on Windows Docker Desktop.

Do not introduce a large task runner solely for these commands.

---

# Safe Development World Reset

Provide a safe way to reset the disposable Docker development world.

The reset process must:

- clearly state what data will be removed,
- target only development-server world/runtime data,
- never run automatically during build,
- never run automatically during server restart,
- avoid patterns that could later delete a family or persistent playtest world accidentally.

Document this distinction.

If named Docker volumes are used, make destructive reset commands explicit.

---

# LAN Testing Documentation

Document how another Java Edition Minecraft client on the same household network can connect to the Docker server.

Cover:

- default port `25565`,
- using the Docker host machine's LAN IP,
- matching Minecraft version,
- Fabric Loader requirement,
- Minecraft Golf mod requirement,
- local firewall considerations.

Do not add Bedrock compatibility.

---

# CI

Create a minimal GitHub Actions workflow.

It should run on appropriate pushes and pull requests and verify at least:

```bash
./gradlew test
./gradlew build
```

Use dependency/Gradle caching where straightforward.

Do not add Docker Minecraft startup to CI during M0 unless it is trivial and highly reliable.

Docker dedicated-server startup is currently a local integration requirement.

---

# AGENTS.md

Create a concise root-level:

```text
AGENTS.md
```

This file is intended for future autonomous coding sessions.

It must instruct agents to:

1. read `docs/PRD.md`,
2. read `docs/ARCHITECTURE.md`,
3. read `docs/MILESTONES.md`,
4. determine the current milestone,
5. stay within that milestone,
6. preserve client/server separation,
7. preserve server-authoritative design,
8. run relevant verification,
9. stop on failed verification,
10. stop before architectural deviations,
11. document future ideas rather than implementing them early,
12. never consider compilation alone proof of completion.

Include the standard verification ladder:

```text
unit tests
↓
Gradle build
↓
Loom client/server
↓
Docker Fabric server
↓
manual gameplay verification where applicable
```

Keep `AGENTS.md` useful and concise rather than duplicating the entire planning documentation.

---

# README.md

Create a developer-oriented root README.

Include:

- project purpose
- current project name and identifiers
- Java requirement
- basic build commands
- tests
- Loom client/server commands
- Docker server commands
- repository structure
- planning document references
- current milestone

Reference the planning documents at:

```text
docs/PRD.md
docs/ARCHITECTURE.md
docs/MILESTONES.md
```

Clearly state:

```text
Current milestone: M0 — Project Setup
```

Once M0 is completed, update this to indicate the next milestone is M1.

---

# Documentation Integrity

Do not rewrite the PRD or architecture simply to match implementation convenience.

Minor documentation corrections are allowed only when clearly necessary.

If an architectural assumption proves invalid because of current Fabric tooling:

1. verify the issue,
2. document it,
3. make the smallest necessary change,
4. explain the deviation in the final handoff.

Do not silently redesign the project.

If moving the planning documents into `docs/` causes internal links or path references to become stale, update those path references without changing the substantive planning content.

---

# Git Hygiene

Use focused commits.

A reasonable sequence might include:

```text
docs: organize planning documents
chore: scaffold fabric project
test: add initial test harness
chore: add docker fabric dev server
ci: add gradle verification workflow
docs: add agent and developer setup instructions
```

Exact commit structure is flexible.

Do not commit generated Minecraft runtime data or secrets.

At the end of M0, the working tree should be clean.

---

# GitHub Repository

If GitHub CLI authentication is available, create or connect a repository named:

```text
minecraft-golf
```

under the currently authenticated personal GitHub account.

Do not invent an organization.

Prefer a **private repository initially** unless repository visibility has already been explicitly specified elsewhere.

If repository creation or authentication is unavailable, continue the local setup and document that GitHub publication remains pending.

Do not block M0 solely on GitHub availability.

---

# M0 Verification Checklist

Before declaring M0 complete, verify every applicable item below.

## Planning Documents

Confirm these files exist:

```text
docs/PRD.md
docs/ARCHITECTURE.md
docs/MILESTONES.md
```

Confirm the old root-level copies no longer remain unless there is a documented reason for retaining them.

PASS.

## Tests

```bash
./gradlew test
```

PASS.

## Build

```bash
./gradlew build
```

PASS.

## Client

```bash
./gradlew runClient
```

Minecraft launches with Minecraft Golf loaded.

PASS.

## Loom Server

```bash
./gradlew runServer
```

Fabric server starts with Minecraft Golf loaded.

PASS.

## Docker Server

Build and sync the mod using the repository's documented workflow.

Start the Docker server.

Verify:

- Fabric loads,
- Minecraft Golf initializes,
- the server reaches running/healthy state,
- there are no client-only classloading errors,
- no unrelated plugin infrastructure exists.

PASS.

## Client Connection

Connect a compatible Java Edition Fabric client to the Docker server.

PASS.

---

# Explicit M0 Non-Goals

Do not implement:

- golf-ball entities
- ball physics
- golf clubs
- club registration
- swing mechanics
- power meter
- aiming UI
- shot networking
- courses
- cups
- tees
- scoring
- round state
- wind
- terrain golf rules
- multiplayer gameplay
- progression
- course authoring

If tempted to implement any of these, stop.

They belong to later milestones.

---

# M0 Completion

Once all verification passes:

1. update the milestone status in `docs/MILESTONES.md`:

```text
M0 — Project Setup: Complete
M1 — Golf Ball Physics: Next
```

2. update the README current milestone,
3. ensure all documentation matches the actual repository,
4. ensure the working tree is clean,
5. create a final verified M0 commit if appropriate.

Then produce a handoff summary containing:

## Completed

What was created.

## Key Versions

Record exact:

- Minecraft version
- Java version
- Fabric Loader version
- Fabric API version
- Fabric Loom version
- Gradle version

## Verification Results

Report the outcome of:

```text
./gradlew test
./gradlew build
./gradlew runClient
./gradlew runServer
Docker Fabric server boot
client connection to Docker server
```

## Repository Structure

Summarize important files/directories.

Include the planning-document location:

```text
docs/
├── PRD.md
├── ARCHITECTURE.md
└── MILESTONES.md
```

## Deviations

List any deviations from the planning documents and why they were necessary.

If none:

```text
None.
```

## Next Milestone

State:

> M1 — Golf Ball Physics

Do not begin M1 during this session unless explicitly instructed in a new request.

---

# Definition of Success

This session succeeds when the project has a boring, reliable foundation.

A fresh human developer or autonomous agent should be able to clone the repository, read:

```text
docs/PRD.md
docs/ARCHITECTURE.md
docs/MILESTONES.md
AGENTS.md
```

run the build/tests, launch the Fabric development environment, boot the Docker dedicated server, and understand exactly how future milestone work should proceed.

Do not optimize for visible golf features yet.

Optimize for making every later agent session safe, reproducible, testable, and easy to hand off.
