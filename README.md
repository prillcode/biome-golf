# Minecraft Golf

A Fabric mod for Minecraft Java Edition that adds multiplayer golf to ordinary Minecraft worlds — arcade-realistic, skill-based, and played through real terrain.

> **M8.9 — Practice Range is complete.** M0–M8.9 are complete, and M9 remains
> deferred. See `docs/M8.9-CLOSEOUT.md`.

Ready Golf play flow: use `/golf round list` to see every open lobby and finalized
course. Create a shared round with `/golf round create <courseId>`, join a specific
lobby with `/golf round join <roundId>`, and have that lobby's coordinator start it
with `/golf round start`. Multiple independent rounds may coexist, including on the
same course and hole. Golfers play Ready Golf without forced turns.
After completing each hole, use the on-screen next-hole prompt, clickable chat action,
or `/golf nexthole` after each non-final hole. The server preserves cumulative scoring and
automatically prints the final scorecard at the end of the course. When the player
does not belong to a lobby or round, solo play is available alongside unrelated
Ready Golf rounds with `/golf hole start [hole]` or
`/golf course play [course-id] [hole]`. Explicit course play replaces an existing solo
attempt, including a completed attempt, and can start at an authored hole directly:
`/golf course play re9 8`. Invalid replacement requests leave the current attempt intact.

During active play, `/golf hole restart` resets only the current hole.
`/golf round leave` is the single exit action: it leaves a lobby, abandons solo
play, withdraws from an active multiplayer round, or exits a completed round and
returns that golfer to the Overworld world spawn. After the round completes, one
remaining golfer can use `/golf round restart` to restart Hole 1 for every
remaining golfer.

`/golf course play` without an ID and `/golf hole start [hole]` remain guarded and do not
replace an attempt. Ready Golf participants and lobby members must use `/golf round leave`
before explicit solo course play; it never silently changes multiplayer state.

Operators can author additional courses on existing terrain. Use
`/golf course create <id> <display name>`, `/golf course edit <id>`, then capture
each hole with `/golf hole tee <number>`, `/golf hole cup <number>`,
`/golf hole par <number> <par>`, and two opposite corners with
`/golf hole bounds <number>`. Inspect with `/golf course status <id>`, finalize
with `/golf course finalize <id>`, and select with `/golf course select <id>`.
Clone an existing course into an independent draft with
`/golf course clone <source-id> <new-id> [display name]`.
Hole bounds are optional when authoring; omit `/golf hole bounds` to leave that
hole unbounded for out-of-bounds purposes.
Operators can persist the server/world fallback with
`/golf course default set <id>`, inspect it with `/golf course default status`,
and remove it with `/golf course default clear`. A runtime selection takes
precedence over this default and resets when the server stops.
Players can restore missing clubs with `/golf clubs equip`.

## Project purpose

Small groups (1–4 players, families and friends) build golf courses into Minecraft terrain and play them together — cliffs, caves, water, ice, slime, whatever the world offers. See `docs/PRD.md` for the product vision and `docs/ARCHITECTURE.md` for the technical design.

The current public name "Minecraft Golf" is temporary; the mod id (`minecraft_golf`) and package (`com.prillcode.minecraftgolf`) are chosen to be easy to rename later.

Release history is maintained in [`CHANGELOG.md`](CHANGELOG.md); releases are published
from `v*` tags (see [`docs/RELEASING.md`](docs/RELEASING.md)).

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

To deploy the built mod to a launcher client, run `./scripts/deploy-client.sh [game-dir]`.
It defaults to `~/.minecraft`; pass a game directory (or `mods/` directory) to target a
custom launcher instance.

## Docker dev server

A Fabric dedicated server for realistic testing and LAN play. See `dev-server/README.md`.

For a production-oriented Google Compute Engine deployment, domain setup, backups,
and end-user mod distribution, see
[`docs/GCP-DEPLOYMENT-AND-MOD-DISTRIBUTION.md`](docs/GCP-DEPLOYMENT-AND-MOD-DISTRIBUTION.md).

```bash
./scripts/dev-server-sync.sh      # build + copy mod JAR into dev-server/mods/
./scripts/dev-server-up.sh        # start the server (detached)
./scripts/dev-server-logs.sh      # follow logs
./scripts/dev-server-restart.sh   # restart
./scripts/dev-server-down.sh      # stop
./scripts/dev-server-reset.sh     # delete the dev world (interactive confirmation)
```

Connecting from the LAN: point a Java Edition client with Fabric Loader + the mod installed at the host's LAN IP, port `25565`. See `dev-server/README.md`.

Players can drop a practice ball with `/golf practice ball` and remove their own
unassigned practice balls with `/golf practice clear`.
Operators can define a persisted practice range: `/golf practice tee set` saves the
standing position as the practice tee and `/golf practice tee` returns to it, while
`/golf practice target set <1-8>`, `/golf practice target clear <1-8>`, and
`/golf practice target list` place, remove, and list cup-and-flag targets.
The administrative `/golf clear` command remains restricted and clears all loaded
golf balls.

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
