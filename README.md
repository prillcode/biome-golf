# Minecraft Golf

A Fabric mod for Minecraft Java Edition that adds multiplayer golf to ordinary Minecraft worlds — arcade-realistic, skill-based, and played through real terrain.

> **Current milestone: M4 — Holes, Cups, Boundaries, and Scoring** (M0–M3 are complete)

## Project purpose

Small groups (1–4 players, families and friends) build golf courses into Minecraft terrain and play them together — cliffs, caves, water, ice, slime, whatever the world offers. See `docs/PRD.md` for the product vision and `docs/ARCHITECTURE.md` for the technical design.

The current public name "Minecraft Golf" is temporary; the mod id (`minecraft_golf`) and package (`com.prillcode.minecraftgolf`) are chosen to be easy to rename later.

## Requirements

- **Java 25** (JDK)
- Docker + Docker Compose (for the integration server)
- Git

## Quick start

```bash
./gradlew build        # compile + package
./gradlew test         # unit tests
./gradlew runClient    # launch a Minecraft dev client with the mod
./gradlew runServer    # launch a Fabric dev server
```

First Gradle run downloads dependencies. `runServer` needs `run/eula.txt` with `eula=true` (created once, gitignored).

## Docker dev server

A Fabric dedicated server for realistic testing and LAN play. See `dev-server/README.md`.

```bash
./scripts/dev-server-sync.sh      # build + copy mod JAR into dev-server/mods/
./scripts/dev-server-up.sh        # start the server (detached)
./scripts/dev-server-logs.sh      # follow logs
./scripts/dev-server-restart.sh   # restart
./scripts/dev-server-down.sh      # stop
./scripts/dev-server-reset.sh     # delete the dev world (interactive confirmation)
```

Connecting from the LAN: point a Java Edition client with Fabric Loader + the mod installed at the host's LAN IP, port `25565`. See `dev-server/README.md`.

## Repository structure

```text
minecraft-golf/
├── AGENTS.md              # instructions for coding agents
├── README.md
├── build.gradle
├── settings.gradle
├── gradle.properties      # pinned Minecraft/Fabric versions
├── gradlew / gradlew.bat
├── gradle/wrapper/
├── docs/                  # authoritative planning documents
│   ├── PRD.md
│   ├── ARCHITECTURE.md
│   ├── MILESTONES.md
│   └── INITIAL-PROMPT.md
├── src/
│   ├── main/              # common + server-safe code (com.prillcode.minecraftgolf)
│   │   ├── java/
│   │   │   └── com/prillcode/minecraftgolf/
│   │   │       ├── MinecraftGolf.java
│   │   │       └── domain/
│   │   └── resources/     # fabric.mod.json, assets/
│   ├── client/            # client-only code (com.prillcode.minecraftgolf.client)
│   │   └── java/
│   └── test/              # plain-JVM unit tests (JUnit 6)
├── dev-server/            # Docker Fabric integration server
│   ├── docker-compose.yml
│   ├── mods/              # synced mod JARs (gitignored)
│   └── README.md
├── scripts/               # dev-server lifecycle helpers
├── .github/workflows/     # CI
└── run/                   # Loom dev runtime data (gitignored)
```

Planned package layout (created as needed, per `docs/ARCHITECTURE.md`): `domain/`, `entity/`, `item/`, `course/`, `round/`, `network/`, `config/`, `client/`.

## Verification ladder

```text
unit tests  →  gradle build  →  Loom client/server  →  Docker dev server  →  manual play
```

See `AGENTS.md` for the full agent workflow rules.

## License

MIT — see `LICENSE`.
