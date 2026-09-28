# Biome Golf Dev Server

A Fabric dedicated server for realistic testing and LAN play. Runs via Docker
Compose; portable across Docker hosts (this Linux machine, or a Windows 11
mini-PC). The tracked gameplay profile uses Survival mode with Peaceful
difficulty; Biome Golf does not replace vanilla block-breaking rules outside
protected course zones.

## Requirements

- Docker + Docker Compose plugin
- On Linux: your user must have Docker daemon access (one-time setup):

```bash
sudo usermod -aG docker $USER   # then log out and back in
```

## Choose a testing environment

Biome Golf uses both Loom and Docker because they prove different things.

| Workflow | Best for | Advantages | Limitations |
|---|---|---|---|
| `./gradlew runClient` → Singleplayer | Fast client iteration | Runs current classes automatically; direct logs and debugging; ideal for HUD, input, rendering, and camera checks | Uses an integrated server; world terrain is not inherently repeatable; does not prove dedicated-server login, reconnect, or persistence behavior |
| Docker multiplayer server | Integration and acceptance | Persistent prepared world; real dedicated-server boundary; realistic networking, reconnect, and persistence testing | Requires build/JAR sync and restart; persistent state can become stale; the connecting client must use matching Minecraft, Fabric API, and mod versions |

Recommended hybrid workflow:

1. Run `./gradlew test` and `./gradlew build` for every relevant change.
2. Use `./gradlew runClient` with a cheats-enabled **Singleplayer** world using seed `-1928790872702396508` for rapid client-facing checks. Run `/golf dev preparecourse` once to prepare or restore the M5 campus.
3. Use Docker for dedicated-server integration and final gameplay acceptance after syncing the JAR.

> **Authentication:** the Docker server intentionally uses `online-mode=true`. Loom's `runClient` development identity cannot authenticate to it and reports `Failed to login: Invalid session`. Connect with an authenticated Java Edition client containing matching Fabric Loader, Fabric API, and Biome Golf versions. Do not weaken the tracked Docker server's authentication for Loom testing.

## Start / stop

```bash
# from the repository root:
./scripts/dev-server-sync.sh 0.6.1 # build and sync dev-server/mods/biome-golf-0.6.1.jar
./scripts/dev-server-up.sh       # start the server (detached; Compose copies /mods into /data/mods)
./scripts/dev-server-logs.sh     # follow server logs
./scripts/dev-server-restart.sh  # restart
./scripts/dev-server-down.sh     # stop
./scripts/dev-server-reset.sh    # restore the pulled baseline (asks for confirmation)
```

Pass the version matching the build artifact to the sync step before starting or restarting whenever code changes. The start and restart scripts verify that a versioned Biome Golf JAR is staged, but they do not rebuild it.

## JAR synchronization and identity

```bash
./scripts/dev-server-sync.sh 0.6.1
./scripts/dev-server-restart.sh   # or dev-server-up.sh if stopped

# Optional identity check: these hashes must match after the server starts.
sha256sum dev-server/mods/biome-golf-0.6.1.jar
docker exec minecraft-golf-dev sha256sum /data/mods/biome-golf-0.6.1.jar
```

A healthy container with an old JAR is not valid test evidence. Check startup logs for the Biome Golf registration messages after every sync/restart cycle.

## Connecting

1. Install Fabric Loader on your Java Edition client and add the mod (from `build/libs/`) to your `mods/` folder.
2. Point the client at the host's LAN IP, port `25565` (localhost:25565 on the Docker host itself).
3. LAN clients need a Minecraft account (the server enforces online mode).

For the local authenticated client, run `./gradlew build` and copy the versioned
JAR to `~/.minecraft/mods/` after client-facing changes. Fully restart Minecraft
after copying; an already-running client keeps the previous JAR loaded. This is a
separate deployment step from Docker's `dev-server-sync.sh`.

To find the host's LAN IP: `ip addr` on Linux, `ipconfig` on Windows.

## Bedrock / Geyser prototype (M10.0 spike — SHELVED, optional/unsupported)

**Status:** the client-light/Bedrock effort is shelved; the modded Java client is the only
supported experience. This overlay is kept as an optional experiment only. See
`docs/M10-PLAN.md`.

The Bedrock cross-play spike layers an optional Compose overlay on top of the base
dev server, so the tracked default stays portable and unchanged:

```bash
./scripts/dev-server-geyser-up.sh        # base compose + Geyser/Floodgate + UDP 19132

# the same overlay for logs / stop:
docker compose -f docker-compose.yml -f docker-compose.geyser.yml logs -f
docker compose -f docker-compose.yml -f docker-compose.geyser.yml down
```

What the overlay does (`docker-compose.geyser.yml`):

- adds the Bedrock UDP listener `19132:19132/udp`;
- installs `geyser` and `floodgate` from Modrinth alongside the existing pinned
  Fabric API. Geyser only publishes **beta** builds, so the overlay widens the
  Modrinth downloader to `beta` (`MODRINTH_PROJECTS_DEFAULT_VERSION_TYPE`); the
  Fabric API pin is unaffected;
- after first boot, `dev-server-geyser-up.sh` sets Geyser `auth-type: floodgate`
  (idempotent) so Bedrock players do not need a linked Java account. Geyser's
  generated config lives at `/data/config/Geyser-Fabric/config.yml` in the volume.

Bedrock clients connect to the host on UDP **19132** — LAN IP (e.g.
`192.168.40.194:19132`), Tailscale IP (`100.70.252.108:19132`), or
`localhost:19132` on the Docker host. This is a **spike harness for architectural
evidence only, not a supported deployment**, and no Bedrock/vanilla-Java acceptance is
claimed (see `docs/BEDROCK-COMPATIBILITY-ASSESSMENT.md`, `docs/M10-PLAN.md`).

## Data, restart, and reset

- Server world and configuration live in the directory named by
  `MINECRAFT_DATA_DIR` (default: the Compose volume
  `dev-server_minecraft-golf-data`). `dev-server/.env` points it at the world
  pulled from the live server (e.g. `~/birdie-biome-world`); a bind directory is
  required for `world-sync.sh` to read and write the world directly. The world
  name in `worlds.conf` (apcode-dev) must resolve to this same directory.
- The mod is mounted read-only from `dev-server/mods/`.
- `dev-server-restart.sh` preserves the world, configuration, and other state.
- `dev-server-reset.sh` stops the server and restores the newest **baseline** —
  the pristine state captured by `world-sync.sh pull birdie-biome-world` — after
  an explicit typed
  confirmation. Local work since that pull is discarded, and the script refuses
  when no baseline exists. It never touches the live server, source code, or the
  staged mod JAR.
- After a reset, run `dev-server-up.sh` to start from the restored world.

Baselines live in `<MINECRAFT_DATA_DIR>-baselines` and are pruned to the newest
five by default. See the
[apcode-dev world sync workflow](https://github.com/prillcode/apcode-dev/blob/main/docs/WORLD-SYNC-WORKFLOW.md).
A world with no baseline cannot be reset; starting a new world means a new
directory (and name), not a reset.

Persistent state is useful for repeatable gameplay, but it can retain stale entities or configuration. Prefer a normal restart for code changes and reset only when you want the known-good starting state back.

### Working with the live server world

To pull the live server's world into this dev server, develop locally, then push
the result back, see the [apcode-dev world sync workflow](https://github.com/prillcode/apcode-dev/blob/main/docs/WORLD-SYNC-WORKFLOW.md).
`pull` binds the
world directory into this server via `MINECRAFT_DATA_DIR`, so the dev server runs
exactly the live world; `dev-server-reset.sh` returns to that pulled state.

## Prepared practice area

### M5 three-hole campus

The scored M5 course and separate practice range use layout
`minecraft_golf:m5_ocean_campus` v11 on seed `-1928790872702396508`. Preparation
is explicit, seed-gated, two-phase, and bounded by the overall envelope
`X[-640..448]`, `Y[32..192]`, `Z[-256..640]`; only the authored subregions in
`docs/M5-PLAN.md` are eligible for mutation.

```text
/golf dev preparecourse
/golf course select m5
/golf hole start
/golf nexthole
```

Prepare the layout, explicitly select the finalized authored `m5` course, and start
Hole 1 normally. After a server restart, repeat the course-selection command; the
active course is runtime-only. Use the clickable completion action or `/golf nexthole`
after non-final holes. The final hole automatically finalizes and prints the
scorecard. `/golf dev testhole <1|2|3>` is an
operator-only isolated-hole tool and does not exercise sequencing or the final
scorecard. Repeating `preparecourse` after the vegetation cleanup has settled must
report zero changed blocks.

### M4.5 legacy practice hole

M4.5 uses a fixed 16-block practice hole at the world origin:

| Point | Entity position | Supporting/special block |
|-------|----------------|--------------------------|
| Tee   | `(0.5, 63.25, 0.5)`  | grass floor at `(0, 62, 0)` |
| Cup   | `(16.5, 63.25, 0.5)` | cup block at `(16, 63, 0)` |

After the server is healthy, run the preparation script once (or any time the platform needs restoring):

```bash
./scripts/dev-server-prepare-hole.sh
```

The script:
1. Waits for the container to report `healthy` (polls every 10 s, max 5 min).
2. Verifies that RCON commands are available.
3. Force-loads only the chunks intersecting the bounded practice footprint, so setup also works before a player visits the origin.
4. Runs the mod's operator-gated `/golf dev preparehole` command. The command derives the platform from the configured tee/cup, clears the bounded air column, lays the grass floor, and places the cup at the authoritative block position.
5. Independently verifies four corner/key blocks and exits non-zero on any mismatch.

**Idempotent**: re-running either the script or `/golf dev preparehole` overwrites only the derived practice footprint and leaves everything outside it unchanged. Neither is called on normal server startup. The command intentionally rejects sloped tee/cup configurations rather than flattening a larger or ambiguous area.

**Physics note**: `grass_block` resolves to `SurfaceDefinition.NORMAL` (fairway baseline). The floor surface top is at Y=63; ball entity centers rest at Y=63.25 (0.25-radius ball). The cup capture ellipsoid is ±0.34 XZ × ±0.45 Y centred on `(16.5, 63.25, 0.5)`.

## Configuration

Server settings (version, memory, game mode, MOTD, etc.) are in `dev-server/docker-compose.yml`:

- `VERSION` pins the Minecraft version — keep it in sync with `gradle.properties`.
- `MEMORY` is the JVM heap.
- `VIEW_DISTANCE` and `SIMULATION_DISTANCE` are set to 16 so long golf shots remain
  loaded while the player stays at the shot origin. Clients should use a render
  distance of at least 16 for the post-shot camera to see the full flight.
- `MODE` / `DIFFICULTY` / `MOTD` are gameplay-facing.
- Fabric API `0.160.0+26.2` is pinned and downloaded from Modrinth through
  `MODRINTH_PROJECTS`; keep it matched with `gradle.properties` and the client.
- `RCON_CMDS_STARTUP` fixes the development world at daytime with clear weather so gameplay tests are repeatable. Minecraft 26.2 uses the namespaced `minecraft:advance_time` and `minecraft:advance_weather` gamerules.

## Manual integration checklist

Before accepting a Docker gameplay run:

1. Sync the current JAR and restart the server.
2. Confirm matching local/container JAR hashes and healthy server status.
3. Confirm Biome Golf registration messages and no relevant startup errors.
4. Confirm the prepared tee, cup, boundary, daytime, and weather state.
5. Exercise practice mode and the active-hole lifecycle, including recovery and replay scenarios required by the current milestone.
6. Record any persistent-world contamination; reset to the pulled baseline when a known-good starting state is required.

## Notes

- The server image is `itzg/minecraft-server` — a standard, widely used Fabric server image.
- Do not commit anything under `dev-server/mods/` or the Docker volume data (see `.gitignore`).
