# M1 — Golf Ball Physics: Closeout Record

Status of milestone **M1** (`M001` slices S01–S04). All four slices are implemented
and merged to `main`. This document records the verification evidence actually
executed and the small set of items that still need a human eye before M1 can be
called *fully* complete per `MILESTONES.md`.

## Deliverables present in code (all merged to `main`)

| Slice | Purpose | Key sources (under `src/main/java/...`) |
|---|---|---|
| S01 | Pure-Java server-free physics core | `ball/BallPhysics`, `ball/BallState`, `ball/PhysicsConfig`, `ball/BallCollisionWorld`, `ball/CollisionResult`, `golf/Vec3`, `surface/SurfaceDefinition` |
| S02 | Server-authoritative entity + real-world collision | `entity/GolfBallEntities`, `entity/GolfBallEntity`, `world/MinecraftBallCollisionWorld`, `world/GolfBlockSurfaceResolver`, `MinecraftGolf` |
| S03 | Dev launch/test controls (server-safe) | `command/GolfDevCommands` |
| S04 | Visible client renderer | `src/client/.../client/MinecraftGolfClient`, `client/render/GolfBallEntityRenderer`, `client/render/GolfBallRenderState` |

Server/client separation holds: zero `net.minecraft.client` or
`com.prillcode.minecraftgolf.client` imports anywhere in `src/main` (verified by
scan; the only client mention is the entrypoint class name in
`src/main/resources/fabric.mod.json`).

## Automated verification — PASSED (run 2026-09-07)

- `./gradlew cleanTest test` → BUILD SUCCESSFUL, **61 tests, 0 failures / 0 errors**
  (`BallPhysicsTest` 26 incl. the explicit no-tunnel sub-step case, `Vec3Test` 15,
  `SurfaceDefinitionTest` 9, `PhysicsConfigTest` 9, `ModIdTest` 2).
- `./gradlew build` → BUILD SUCCESSFUL.
- `./gradlew compileJava` / `compileClientJava` → green.

## Dedicated-server verification — PASSED (live Docker, 2026-09-07)

Boot log (`dev-server`, image `itzg/minecraft-server:latest`, Fabric 26.2) contains:

```
Minecraft Golf initialized
Registered entity minecraft_golf:golf_ball as golf_ball
Registered /golf dev command group (spawn, launch, inspect, clear)
Done (0.699s)!
```

Log error/exception/client-class scan: **0** matches.

Live rcon transcript over real terrain in force-loaded spawn chunks
(`forceload add 0 0`):

- `golf spawn 1 84 1` → `golf: spawned ball #3 ...` then a later `golf inspect`
  showed it had **fallen y84 → y46.84 airborne at 15.75 blocks/s** under server gravity.
- After settling: `golf: ball #1 RESTING | pos (0.50, 41.25, 0.50) | vel (0,0,0) |
  grounded=true resting=true` — drop, bounce→roll, and stable stop all observed.
- `golf launch 1.0 0.6` → `MOVING (airborne) ... pos (0.50, 43.62, 6.31) ... speed 19.32 blocks/s`
  — real flight over terrain.
- `golf clear` → removed balls cleanly.

Evidence log: `build/m1-devserver-evidence.log` (gitignored, disposable).

## Remains for human sign-off (per MILESTONES.md exit criteria)

The milestone's defining exit check is *"a ball launched through Minecraft terrain
is satisfying enough to build the golf game around it."* The following require a
human/visual judgment and were **not** closed by automated/Docker evidence:

1. **runClient visual render** — confirm the white ball is visible, correctly
   sized/centered, and behaves believably in a live client (S04 renderer ships; a
   dedicated server has no renderer so nothing draws there by design).
2. **Surface-feel distinctions** — normal ground vs. ice/sand/slime/honey produce
   meaningfully different behavior when *watched*. Automated `BallPhysicsTest`
   covers the coefficient math, but the perceived feel is a gameplay call.
3. **"Good enough to keep building"** decision on ball feel (/golf spawn/launch
   controls in a cheats-enabled world: `/golf launch <forward> <up>`).

If any feel defect surfaces, tune only existing constants in
`src/main/java/com/prillcode/minecraftgolf/ball/PhysicsConfig.java` or
`src/main/java/com/prillcode/minecraftgolf/surface/SurfaceDefinition.java` and
rerun `./gradlew test`.

## Handoff / next milestone

M1 code is merged to `main` (S04 renderer commit `5b6fa51` merged as `811114f`).
After human visual sign-off above, the next milestone is **M2 — Clubs and Shot
Execution**: club item definitions + registration, club property model, camera
aim, server-authoritative shot request, ball ownership, and club melee behavior.
