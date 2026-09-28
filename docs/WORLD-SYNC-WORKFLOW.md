# Develop Locally, Publish to the Live Server (World Sync)

This is the workflow for adding courses or making landscape edits without
authoring on the live server. You keep the live server open for players, do the
work in a local world, then promote that world back in a short maintenance step.

## Why this is needed

There is no course import/export command. Everything a course needs lives in the
saved world:

| Data                                                       | Location in the world save                        |
| ---------------------------------------------------------- | ------------------------------------------------- |
| Course definitions (tee/cup/par/bounds/landscape)          | `world/data/minecraft_golf_authored_courses.json`                              |
| Course terrain (fairways, greens, hazards, builds)         | `world/dimensions/<dim>/region/*.mca` (+ `entities/`, `poi/`)                  |

`<dim>` is the dimension path, e.g. `minecraft/overworld`. The course store is one
world-level file shared by all dimensions.

The in-game authoring commands (see
[COURSE-CREATION-STEPS.md](COURSE-CREATION-STEPS.md)) mutate the live store, so
without a world-sync step, authoring means editing the running server.

The live server keeps its world on a persistent DigitalOcean Volume that is only
attached while a Droplet is running, and a Volume attaches to only one Droplet at
a time. Promotion therefore goes through the running Droplet.

## The loop

```text
PULL  ->  DEVELOP LOCALLY  ->  PUSH  ->  verify
(live)     (your machine)      (live)
```

- The live server runs normally *between* pull and push.
- Pull and push each briefly stop Minecraft; connected players are disconnected.
  The graceful stop saves first, so nothing is lost, but schedule both when the
  server is idle. Keep the stopped window short: the session guard's idle timeout
  can auto-stop the Droplet mid-copy (the world is small and syncs in seconds).
- You cannot copy region files while Minecraft is running: chunks may be
  mid-write. Always stop the container before copying, in both directions.

## Tooling

The transport helper lives with the production control plane (apcode-dev), not in
this repo:

```text
apcode-dev/apps/birdie-biome-server/scripts/world-sync.sh
```

It drives the Droplet lifecycle through the apcode API (the same control plane as
`mc-ctl.sh`) and uses `doctl`, `ssh`, `rsync`, and `python3`. This repo supplies
the authoring model and the course formats.

Prerequisites:

- An SSH key authorized on the Droplet, and `doctl` authenticated for the account.
- The controls: `mc-ctl.sh` (apcode-dev), `rsync`, and `python3`.
- A local development environment matching the live Minecraft/mod version
  (currently Minecraft `26.2`, Fabric, the pinned Fabric API; seed
  `-1928790872702396508`).

## Commands

Every command takes a **world name**, and there is no default. A world must be
registered in the worlds registry (below); an unregistered name is refused.

```bash
# List registered worlds.
world-sync.sh worlds

# Show server state and the local copy.
world-sync.sh status birdie-biome-world

# Pull the live world into that world's local directory.
world-sync.sh pull birdie-biome-world

# Preview exactly which files a course push would send.
world-sync.sh course-files birdie-biome-world

# Push the local course work back (course-scoped delta; default).
world-sync.sh push birdie-biome-world

# Replace the entire live world instead.
world-sync.sh push birdie-biome-world --full

# Capture the current local world as a baseline (usually done by 'pull').
world-sync.sh baseline birdie-biome-world
```

Useful flags:

| Flag            | Meaning                                                        |
| --------------- | -------------------------------------------------------------- |
| `--conf FILE`   | Worlds registry (default `../worlds.conf` beside the script)    |
| `--full`        | Whole-world push instead of the course-scoped delta            |
| `--no-snapshot` | Skip the pre-push DigitalOcean Volume snapshot                 |
| `--stop-after`  | Stop the server when the command finishes                      |
| `--dry-run`     | Preview without stopping the server, snapshotting, or writing  |

## The worlds registry

World names resolve through `worlds.conf` in the apcode-dev repo
(`apps/birdie-biome-server/worlds.conf`), which maps a name to its local
directory and, for a live world, to its server:

```ini
[birdie-biome-world]
server = birdie-biome
container = birdie-biome-server
volume = birdie-biome-data
remote_data = /mnt/birdie-biome-data
```

- `dir` and `baselines` are optional; they default to `$WORLDS_ROOT/<world>` and
  `<dir>-baselines` (`WORLDS_ROOT` defaults to `$HOME`).
- A section with **no `server`** is a **local-only world**: `baseline` and
  `status` work, while `pull` and `push` refuse. That is how a new course project
  on a fresh seed stays independent of the live server.
- Adding a second live world means adding a section; nothing else is hardcoded.
| `--full`            | Replace the whole world (`rsync --delete`) instead of delta   |
| `--no-snapshot`     | Skip the pre-push DigitalOcean Volume snapshot                |
| `--stop-after`      | Stop the server when the command finishes                     |
| `--dry-run`         | Preview without stopping the server, snapshotting, or writing |

A `--dry-run` needs the server already running (it reads remote checksums); it
will not start a billable Droplet for you.

## Course-scoped delta (default)

The default push honours "course areas are authoritative; everything else is a
playground":

1. The script reads the local `world/data/minecraft_golf_authored_courses.json`.
2. It maps every course's landscape perimeter and hole boundaries to the region
   files that cover them, per dimension (holes without an explicit boundary fall
   back to a box around the tee/cup). Since the landscape perimeter is the
   grief-proof area, this scope matches the protected course area. Region files
   span 512x512 blocks, so a course area can pull in adjacent chunks at the
   edges.
3. It force-pushes only those region files plus the course store JSON.

Everything outside a course's bounds is left exactly as the live server has it,
so player builds in the playground survive. If someone built where you made a
course, the local course files win for that region.

The push uses `rsync --checksum`, not mtimes. This matters: `docker stop` makes
the live server save loaded chunks, which rewrites region files with a fresh
mtime. A plain `rsync --update` would then treat the live files as newer and
silently skip your edits. Checksums compare content, so the selected course files
always win.

## Baselines and reset

Each local world has a **baseline**: a tarball of its known-good origin. Reset
restores that tarball — it never deletes or regenerates the world.

- `pull` captures a baseline automatically, after the copy and while the
  container is still stopped, so it is pristine. Retention defaults to the
  newest five.
- `world-sync.sh baseline <world>` captures the current local copy on demand. Use
  it only to bootstrap a world pulled before baselines existed; the result is only
  as good as the local copy at that moment.
- `scripts/dev-server-reset.sh` stops the dev server and restores the newest
  baseline, discarding local work done since the pull. It is local-only: it
  never contacts the live server. A world with no baseline is refused.

Baselines live *beside* the world directory, never inside it, because everything
inside the local world directory is bind-mounted into the dev server as `/data`:

```text
~/birdie-biome-world/              # MINECRAFT_DATA_DIR; bind-mounted as /data
~/birdie-biome-world-baselines/    # baseline-<server>-<utc-stamp>.tar.gz (+ .meta)
```

Both paths follow the world's registry entry, so a differently named local
world (`bogey-biome-world`) gets its own directory and baseline directory just by
adding a section. `WORLD_SYNC_BASELINE_DIR` and `WORLD_SYNC_BASELINE_KEEP`
override the location and retention for one invocation. The reset script reads
`MINECRAFT_DATA_DIR` from `dev-server/.env`.

Reset is the recovery path for a drifted development world. It supersedes the
legacy M5-era `/golf dev preparecourse` rebuild, which is slated for removal.

Reset is *not* how you start a new world. A new world (a new seed, or a separate
course project) is a new directory with its own name and its own baseline.

## Full replace

`--full` replaces the entire `world/` directory with the local copy
(`rsync --checksum --delete`). Use it for changes outside any course bounds, or
when you want the local copy to become the single source of truth. It overwrites
everything players changed on the live server since the pull.

## Safety

- Always stop the container before copying (the script does this).
- Always exclude `session.lock` (the script does this); a stale lock makes the
  next boot think another instance owns the world.
- The script snapshots the Volume before a push unless `--no-snapshot` is given.
- Baselines and reset are local-only; they never touch the live server or the
  DigitalOcean Volume. Only `pull`/`push` do.
- Only `world/` is synced. Do not sync `/data/mods`, `/data/libraries`, or
  `/data/versions`; the server image manages those from `MODRINTH_PROJECTS`.
- Keep local and live versions/seeds in sync; mismatched versions rewrite or
  reject world data.
- Course areas take precedence. Tell players to stay out of the course area while
  you develop if you want zero surprises in that region.

## Rollback

- **Reset to baseline (fastest):** `world-sync.sh pull <world>` banked the
  pre-development state; `scripts/dev-server-reset.sh` restores it. This local
  rollback needs no network and no live downtime.
- **DigitalOcean snapshot:** create a new Volume from the pre-push snapshot,
  point `BIRDIE_BIOME_VOLUME_ID` at it, and restart. The old Volume is untouched
  until you delete it.
- **Local backup:** keep timestamped `world` tarballs of the pulled state; pushing
  one back is the same push procedure. Baselines are exactly this, captured
  automatically.

See [COURSE-CREATION-STEPS.md](COURSE-CREATION-STEPS.md) for the authoring
commands and `apcode-dev/docs/minecraft-operations.md` for server operations.
