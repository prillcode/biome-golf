# Develop Locally, Publish to the Live Server (World Sync)

This is the recommended workflow for adding courses or making landscape edits to
the live server **without authoring on it directly**. You keep the live server
open for players, do the work in a local world, then promote that world back with
a short maintenance step.

For moving a course built in a normal Java Edition instance on the mini-PC into a
local development server, see
[IMPORTING-WORLD-TAILSCALE.md](IMPORTING-WORLD-TAILSCALE.md). This document covers
the other half: local development server -> live/on-demand server.

## Why this is needed

There is no course import/export command. Everything a course needs lives in the
saved world:

| Data                                   | Location in the world save        |
| -------------------------------------- | --------------------------------- |
| Course definitions (tee/cup/par/bounds/landscape) | `world/data/minecraft_golf_authored_courses.json` |
| Course terrain (fairways, greens, hazards, builds) | `world/region/*.mca` (plus `entities/`, `poi/`) |

The in-game authoring commands (`/golf course create`, `/golf hole tee`,
`/golf course landscape ...`, etc. -- see
[COURSE-CREATION-STEPS.md](COURSE-CREATION-STEPS.md)) mutate the live store. So
without a world-sync step, authoring means editing the running server.

The production server keeps its world on a persistent DigitalOcean Volume that is
only attached while a Droplet is running. A Volume can attach to only one Droplet
at a time, which is why promotion goes through the running Droplet.

## The loop

```text
PULL  ->  DEVELOP LOCALLY  ->  PUSH  ->  verify
(live)     (your machine)      (live)
```

- The live server runs normally *between* pull and push.
- Pull and push each briefly stop Minecraft. Players connected at that moment are
  disconnected; the graceful stop saves their progress first, so nothing is lost,
  but schedule both when the server is idle.
- You cannot copy region files while Minecraft is running: chunks may be mid-write.
  Always stop the container before copying the world, in both directions.
- Keep the stopped window short. The session guard runs every 5 minutes and will
  auto-stop an idle server (detaching the Volume), so a long copy can be
  interrupted. The world is small enough that a normal sync finishes in seconds.

## Prerequisites

- SSH access to the Droplet (the operator's key) and `rsync`.
- The operator control script (`mc-ctl.sh`) for start/stop/status, and `doctl`
  authenticated against the DigitalOcean account. These live with the server
  infrastructure, not in this repository.
- A local development environment with the **same Minecraft version and mod
  version** as the live server (currently Minecraft `26.2`, Fabric, the pinned
  Fabric API). The live seed is `-1928790872702396508`.

Throughout, replace:

- `<server-ip>` -- the Droplet public IPv4 (from `mc-ctl.sh status` or `doctl`).
- `<volume-id>` -- the persistent Volume ID (`BIRDIE_BIOME_VOLUME_ID`).

The examples use `~/birdie-world/world` as the local working copy.

## 1. Pull the live world

```bash
# From the apcode-dev repo (mc-ctl.sh lives in apps/api):
cd apps/api
./mc-ctl.sh start birdie-biome
./mc-ctl.sh watch birdie-biome        # wait for status=running

# Flush the world and release the lock.
ssh root@<server-ip> 'docker stop birdie-biome-server'

# Copy the world down (145 MB at the time of writing).
mkdir -p ~/birdie-world
rsync -a --delete --exclude session.lock \
  root@<server-ip>:/mnt/minecraft-golf-data/world/ ~/birdie-world/world/

# Put the live server back the way you found it, then stop it to save cost.
ssh root@<server-ip> 'docker start birdie-biome-server'
./mc-ctl.sh stop birdie-biome
```

If `rsync` is not available on the Droplet, `scp -r` works for a one-shot copy,
or stage a `tar` and pull that. Always exclude `session.lock`.

## 2. Develop locally

Pick one local host for the copy:

- **Gradle server** (fastest iteration on mod code): replace `run/world` with the
  pulled world and run `./gradlew runServer`.
- **Docker dev server** (closest to production): put the world into the
  `dev-server_minecraft-golf-data` volume (see
  [IMPORTING-WORLD-TAILSCALE.md](IMPORTING-WORLD-TAILSCALE.md) for the
  tar-into-volume commands) and use `./scripts/dev-server-up.sh`.

Then build terrain, author/finalize courses, and set the default exactly as in
[COURSE-CREATION-STEPS.md](COURSE-CREATION-STEPS.md). Changes persist to the local
`world/data/minecraft_golf_authored_courses.json` on each successful mutation.

## 3. Push the world back

Do this during a maintenance moment. **Take a snapshot first** so the change is
reversible.

```bash
# 1. Attach the Volume again.
./mc-ctl.sh start birdie-biome
./mc-ctl.sh watch birdie-biome

# 2. Quiesce Minecraft.
ssh root@<server-ip> 'docker stop birdie-biome-server'

# 3. Safety net: a DigitalOcean Volume snapshot.
doctl compute volume snapshot <volume-id> \
  --snapshot-name "birdie-biome-pre-push-$(date -u +%Y%m%dT%H%M%SZ)"

# 4. Write the world back.
#    FULL REPLACE (local copy is authoritative; live changes since the pull are lost):
rsync -a --delete --exclude session.lock \
  ~/birdie-world/world/ root@<server-ip>:/mnt/minecraft-golf-data/world/
#
#    DELTA (recommended if players used the live server while you developed):
#    copy only files your local copy changed; leave the rest of the live world alone.
# rsync -a --update --exclude session.lock \
#   ~/birdie-world/world/ root@<server-ip>:/mnt/minecraft-golf-data/world/

# 5. Reload the new world.
ssh root@<server-ip> 'docker start birdie-biome-server'
```

### Full replace vs. delta

- **Full replace** (`--delete`) is simplest and predictable, but it overwrites
  *everything* that changed on the live server since your pull. Use it when you
  want the local copy to become the world of record and nobody has built since.
- **Delta** (`--update`, no `--delete`) pushes only files where your local copy is
  newer. Live changes in untouched regions are preserved. The risk is narrower:
  if a player edited the same region file you edited (a 32x32-chunk area), your
  version wins for that region. Coordinate so nobody builds in the course area
  while you develop.

Either way, `world/data/minecraft_golf_authored_courses.json` is a single file: if
the live server authored/finalized courses while you developed, one side wins.

## 4. Verify

1. Wait for the container to become healthy.
2. Run `/golf course list` (RCON or in-game) and confirm the expected courses and
   the default.
3. Join and play-test, then leave the world in place.
4. Keep the pre-push snapshot (and any local tarball) until you are satisfied.

## Rollback

- **DigitalOcean snapshot:** create a new Volume from the snapshot
  (`doctl compute volume create <name> --region <region> --size <size>
  --snapshot <snapshot-id>`), point `BIRDIE_BIOME_VOLUME_ID` at it, and restart.
  The old Volume is untouched until you delete it.
- **Local backup:** keep timestamped `world` tarballs of the pulled state; pushing
  one back is the same push procedure.

## Guardrails

- Never copy region files while Minecraft is running.
- Always `--exclude session.lock`; a stale lock makes the next boot think another
  instance owns the world.
- Always snapshot before a push.
- Only sync `world/`. Do not sync `/data/mods`, `/data/libraries`, or
  `/data/versions` -- the server image manages those from `MODRINTH_PROJECTS`.
- Keep the live seed/version and your local dev environment in sync; mismatched
  versions rewrite or reject world data.
