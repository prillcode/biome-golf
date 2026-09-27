# Biome Golf

Build and play golf courses in any Minecraft biome with Biome Golf.

Biome Golf is a multiplayer golf mod for Minecraft Java Edition, built with Fabric. It
lets small groups build courses in ordinary Minecraft terrain and play together with
skill-based shots, server-authoritative scoring, and balls that interact with the world.
Cliffs, caves, water, ice, slime, and player-built structures can all become part of a
course.

Modded Java is the supported golf experience. Bedrock and unmodified-Java clients can
join the world as visitors, but cannot play golf. Additional game modes are deferred;
the current focus is the Java golf experience. See the [product brief](docs/PRD.md),
[architecture](docs/ARCHITECTURE.md), and [milestone status](docs/MILESTONES.md).

For gameplay, operator, and course-authoring commands, see the
[command reference](docs/COMMANDS.md).

The public-facing mod name is **Biome Golf**. Its mod id remains `minecraft_golf` and its
Java package is `pro.apdev.biomegolf`. A public server running it **MUST**
have its own identity, distinct from the mod name, and credit the mod as the source of its
golf functionality—for example,
“Golf course building and round play powered by Biome Golf (`minecraft_golf`).” See the
[changelog](CHANGELOG.md) for release history and [release guide](docs/RELEASING.md)
for publishing details.

## Development setup

### Requirements

- Java 25 JDK
- Git
- Docker and Docker Compose for the dedicated integration server

### Local development

Use the Gradle wrapper for all builds and development runs:

```bash
./gradlew test
./gradlew build
./gradlew runClient
./gradlew runServer
```

The first Gradle run downloads dependencies. To run a local development server, create
`run/eula.txt` with `eula=true`; this file is gitignored.

To install a specific built version in a launcher game directory, run
`./scripts/deploy-client.sh <version> [game-dir]`. The version must match an artifact
in `build/libs/`, such as `0.6.1`; the destination filename is
`biome-golf-<version>.jar`. It defaults to `~/.minecraft`; pass either a game directory
or its `mods/` directory to target another instance. Fully restart the game after
deploying so Fabric loads the new JAR.

### Docker integration server

The Docker Fabric server supports dedicated-server and LAN testing. Its setup,
connection details, and data management are in [`dev-server/README.md`](dev-server/README.md).

From the repository root:

```bash
./scripts/dev-server-sync.sh 0.6.1
./scripts/dev-server-up.sh
./scripts/dev-server-logs.sh
./scripts/dev-server-restart.sh
./scripts/dev-server-down.sh
```

Pass the artifact version when syncing. Sync the JAR before starting or restarting after
code changes. For release and player-facing distribution, see
[`docs/RELEASING.md`](docs/RELEASING.md).

### Repository layout

- `src/main/` — common and server-safe code
- `src/client/` — client-only input, HUD, and rendering code
- `src/test/` — unit tests
- `docs/` — product, architecture, milestone, command, and release documentation
- `dev-server/` — Docker-based dedicated server
- `scripts/` — development and deployment helpers

The project verification path is unit tests, Gradle build, Loom client/server startup,
Docker server verification, then manual gameplay checks where applicable. See
[`AGENTS.md`](AGENTS.md) for the repository workflow.

## License

MIT — see [`LICENSE`](LICENSE).
