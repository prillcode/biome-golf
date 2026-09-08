# Minecraft Golf Dev Server

A Fabric dedicated server for realistic testing and LAN play. Runs via Docker Compose; portable across Docker hosts (this Linux machine, or a Windows 11 mini-PC).

## Requirements

- Docker + Docker Compose plugin
- On Linux: your user must have Docker daemon access (one-time setup):

```bash
sudo usermod -aG docker $USER   # then log out and back in
```

## Start / stop

```bash
# from the repository root:
./scripts/dev-server-sync.sh     # build the mod and sync its JAR into dev-server/mods/
./scripts/dev-server-up.sh       # start the server (detached; Compose copies /mods into /data/mods)
./scripts/dev-server-logs.sh     # follow server logs
./scripts/dev-server-restart.sh  # restart
./scripts/dev-server-down.sh     # stop
./scripts/dev-server-reset.sh    # delete the dev world (asks for confirmation)
```

`dev-server-up.sh` also runs the sync step so the running server always matches the built mod.

## Connecting

1. Install Fabric Loader on your Java Edition client and add the mod (from `build/libs/`) to your `mods/` folder.
2. Point the client at the host's LAN IP, port `25565` (localhost:25565 on the Docker host itself).
3. LAN clients need a Minecraft account (the server enforces online mode).

To find the host's LAN IP: `ip addr` on Linux, `ipconfig` on Windows.

## Data

- Server world and configuration live in the named Docker volume `minecraft-golf-data` (persistent across restarts and image updates; survives bind-mount permission quirks).
- The mod is mounted read-only from `dev-server/mods/`.
- `dev-server-reset.sh` removes the volume — this is the only sanctioned way to delete the dev world.

## Configuration

Server settings (version, memory, game mode, MOTD, etc.) are in `dev-server/docker-compose.yml`:

- `VERSION` pins the Minecraft version — keep it in sync with `gradle.properties`.
- `MEMORY` is the JVM heap.
- `MODE` / `DIFFICULTY` / `MOTD` are gameplay-facing.
- Fabric API is downloaded automatically from Modrinth through `MODRINTH_PROJECTS`.
- `RCON_CMDS_STARTUP` fixes the development world at daytime with clear weather so gameplay tests are repeatable. Minecraft 26.2 uses the namespaced `minecraft:advance_time` and `minecraft:advance_weather` gamerules.

## Notes

- The server image is `itzg/minecraft-server` — a standard, widely used Fabric server image.
- Do not commit anything under `dev-server/mods/` or the Docker volume data (see `.gitignore`).
