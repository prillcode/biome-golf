# Development Dedicated Server

A Docker-based Fabric dedicated server for realistic integration testing and family/LAN playtests.

- Minecraft Java Edition 26.2 + Fabric
- port 25565
- persistent named Docker volume `minecraft-golf-data`
- built mod synced into `mods/` and mounted read-only into the container

## Prerequisites

- Docker + Docker Compose
- built mod JAR (see sync below)

## Start the server

```bash
../scripts/dev-server-sync.sh     # build + copy mod into mods/minecraft-golf.jar
../scripts/dev-server-up.sh       # docker compose up -d
```

Watch progress/logs:

```bash
docker compose logs -f
```

The server is healthy when `mc-health` passes (see `docker compose ps`). First boot downloads the
Minecraft server JAR and generates a world, which takes a few minutes.

## Stop the server

```bash
../scripts/dev-server-down.sh     # docker compose down
```

## Rebuild + restart after a mod change

```bash
../scripts/dev-server-sync.sh
../scripts/dev-server-restart.sh
```

## Reset the disposable dev world

The world/runtime data lives in the named Docker volume `minecraft-golf-data`. It is disposable
development state — deleting it does not touch source code, built mods, or any family playtest world
outside this container.

```bash
../scripts/dev-server-reset.sh
```

This script:

1. stops the container,
2. removes **only** the `minecraft-golf-data` volume and `data/` directory if present,
3. requires explicit confirmation.

It never runs automatically as part of a build or restart.

## LAN connections

Other Java Edition clients on the same network can join with `host-LAN-IP:25565`
(see root `README.md`).
