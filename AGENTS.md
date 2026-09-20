# Minecraft Golf — Agent Instructions

You are working on the Minecraft Golf mod. This file tells you how to work safely in this repository.

## Before doing anything

1. Read `docs/PRD.md` — what the product is.
2. Read `docs/ARCHITECTURE.md` — how the technical pieces fit.
3. Read `docs/MILESTONES.md` — what to build now, what to defer.
4. Determine the **current milestone** (see the status table in `docs/MILESTONES.md` and the root `README.md`).
5. Work only within that milestone. Do not start the next milestone early.

## Non-negotiables

- **Client/server separation.** Client-only code lives only in `src/client/java/`. Common code in `src/main/java/` must never import client classes.
- **Server authority.** Gameplay state (score, ball position, shot results) is owned by the server. Clients send intent, not outcomes.
- **Verification is proof.** Compilation alone is not completion. Run the verification ladder:

```text
unit tests        ./gradlew test
      ↓
Gradle build      ./gradlew build
      ↓
Loom client       ./gradlew runClient
      ↓
Loom server       ./gradlew runServer
      ↓
Docker server     ../scripts/dev-server-sync.sh && ../scripts/dev-server-up.sh
      ↓
manual gameplay   (where applicable, per milestone)
```

### Local Client Deployment

When testing with the authenticated launcher client rather than `./gradlew runClient`,
building is not enough. Use `scripts/deploy-client.sh`, which builds and copies the JAR
into a client's `mods/` folder as `minecraft-golf.jar`. It defaults to the vanilla
`~/.minecraft` location and accepts a game directory (or a `mods/` directory) override:

```bash
./scripts/deploy-client.sh              # -> ~/.minecraft/mods/minecraft-golf.jar
./scripts/deploy-client.sh <GAME_DIR>   # custom launcher instance
```

The user plays through the custom `bhmc-launcher`; its golf instance is named
`BirdieBiome - Golf` (Fabric, Minecraft 26.2). Resolve its game directory from the
launcher metadata and deploy there:

```bash
GAME_DIR=$(python3 -c "import json,os;i=json.load(open(os.path.expanduser('~/.local/share/bhmc-launcher/instances.json')))['instances'];print(next(v['gameDirectory'] for v in i.values() if v['name']=='BirdieBiome - Golf'))")
./scripts/deploy-client.sh "$GAME_DIR"
```

Fully exit and relaunch the game after deploying; Fabric loads mod JARs only at startup.
The launcher's `mods.json` registry (electron-store) records a sha1 for display only; if
you update it, close the launcher first or the running process will overwrite your edit.
The Loom development client uses the compiled classes directly and does not test the
installed JAR.

For Docker testing, also run `./scripts/dev-server-sync.sh` followed by
`./scripts/dev-server-restart.sh`, then verify the container hash and health. Do not
claim client gameplay verification until the local client JAR has been updated and
the client has been restarted.

- **Stop on failure.** If any verification step fails, stop and fix it (or escalate to the user). Do not mark work complete with failing checks.
- **Stop before deviating.** If implementation would require deviating from `docs/ARCHITECTURE.md`, stop and document the deviation rather than silently choosing differently.
- **Document ideas, don't implement them.** Found something interesting outside the current milestone? Add it to a future milestone's notes in `docs/MILESTONES.md` (or a backlog file) — do not build it now.
- **No scope creep.** Do not add speculative abstractions, libraries, or infrastructure without architectural justification.

## Useful facts

- Minecraft 26.2 / Fabric Loader 0.19.5 / Fabric API 0.159.0+26.2 / Loom 1.17.20 / Java 25 / Gradle 9.5.1 — pinned in `gradle.properties`.
- Uses Loom's `splitEnvironmentSourceSets()` — `src/main` (common/server-safe) and `src/client` (client-only).
- Unobfuscated development workflow (no Yarn mappings — Loom handles it).
- Mod id: `minecraft_golf`; base package: `com.prillcode.minecraftgolf`.
- Gradle wrapper is the only supported build entry point — always `./gradlew`, never a system Gradle.
- Dev server: see `dev-server/README.md`. Docker host may be this Linux machine or the Windows 11 mini-PC; keep compose portable.
- Do not commit build output, run/, dev-server data, or secrets (see `.gitignore`).
