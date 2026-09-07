# M3 — Three-Click Swing and HUD: Closeout Record

Status: **Complete** on branch `m3-three-click-swing`.

M3 delivers the complete skill-based shot interaction: client-local three-click
meter timing, server-authoritative shot resolution, a readable HUD, and a simple
post-shot follow camera. Manual playtesting accepted the controls, tuned club
ranges and trajectories, camera behavior, ball-ground alignment, and stepped
terrain behavior.

## Delivered

| Area | Result |
|---|---|
| Swing input | Three right-clicks start the meter, lock power, and lock accuracy/strike. |
| Power and accuracy | Deterministic, clamped values feed the pure `ShotResolver`; poor accuracy creates lateral deviation. |
| Server authority | One typed `ShotRequestPayload` carries intent; `ShotService` re-validates ball, ownership, rest state, club, aim, power, and accuracy before launch. |
| HUD | Lower-right panel above the hotbar shows club, display loft, target distance, power, accuracy zone, and shot state. |
| Club tuning | Display loft is metadata; horizontal/upward launch components independently pin flat-ground range and apex. |
| Follow camera | Client-only third-person ball follow with heading tracking and restoration on rest, rejection, removal, disconnect, timeout, or sneak cancel. |
| Ball presentation | Resting billboard is centered on the physics position and visually touches the ground. |
| Terrain behavior | Near-ground shots traverse supported one-block terrain steps while genuine airborne and two-block sheer-wall impacts remain collisions. |
| Practice tooling | Configurable op-only `B` key submits the existing server-authorized `golf spawn` command. |

## Accepted full-power tuning

Flat `NORMAL`-surface target travel until rest:

| Club | Distance | Display loft | Horizontal velocity | Upward velocity | Approximate apex |
|---|---:|---:|---:|---:|---:|
| Driver | 150 blocks | 10° | 2.6370 | 0.8960 | 6 blocks |
| Fairway Wood | 125 blocks | 16° | 2.1790 | 0.9719 | 7 blocks |
| Long Iron | 100 blocks | 22° | 1.6752 | 1.0440 | 8 blocks |
| Mid Iron | 85 blocks | 36° | 1.4120 | 1.1114 | 9 blocks |
| Short Iron | 60 blocks | 42° | 0.9730 | 1.1776 | 10 blocks |
| Wedge | 42 blocks | 50° | 0.6801 | 1.2096 | 10.5 blocks |
| Putter | 16 blocks | 2° | 0.6655 | 0.0120 | Ground roll |

These are accepted M3 values, not permanent balance promises; later family and
course playtests may tune them without changing the shot architecture.

## Verification evidence

### Automated

- `./gradlew clean test build` — passed from a clean build.
- 97 tests passed with zero failures/errors/skips.
- Flat-ground carry and apex contracts cover all seven clubs.
- Deterministic power/accuracy tests cover partial power, perfect timing,
  lateral miss direction, clamping, and NaN defense.
- Terrain-step tests cover full and half steps, supported rising/descending
  motion, unsupported flight, and taller sheer faces.
- `rg -l 'net\.minecraft\.client' src/main` — zero matches.

### Runtime

- Loom client reached Minecraft Golf common and client initialization.
- Loom dedicated server loaded common code and M3 networking.
- Docker JAR hash matched `build/libs/minecraft-golf-0.1.0.jar`.
- Docker container `minecraft-golf-dev` reported healthy.
- Docker log recorded entity registration, M3 shot-request registration, and
  `Done (1.215s)` with no Minecraft Golf errors.

### Manual acceptance

The user confirmed:

- three-click controls work consistently and feel appropriately paced;
- partial/full power and good/poor accuracy visibly affect shots;
- all clubs execute through the finalized shot flow;
- tuned distance and raised trajectory values are satisfactory for now;
- the HUD no longer overlaps the hotbar;
- the follow camera works well and returns control correctly;
- the corrected resting ball sits on the ground;
- one-block terrain assistance resolves the sharp hill rebound satisfactorily;
- the configurable practice-ball hotkey works.

## Decisions

- D009: power and accuracy thread into the existing resolver/launch path.
- D010: three consecutive right-clicks drive the swing interaction.
- D011: meter timing is client-local and only one finalized payload is sent.
- D012: display loft is metadata; horizontal/upward physics components are tuned independently.

## Known limitations and follow-up

- Club item art remains a placeholder/missing texture and is outside M3 gameplay scope.
- Concurrent multi-client session behavior was not separately playtested; M5 owns
  multiplayer-ready turn flow and broader multi-player validation.
- Club range/trajectory values may be revisited after course and family playtests.
- The practice hotkey intentionally retains the `/golf spawn` gamemaster permission requirement.
- Cinematic cameras, prediction, wind, scoring, and advanced lies remain deferred per M3 non-goals.

## Commit sequence

- `fd42d32` — deterministic partial-power and accuracy shot resolution
- `240737d` — typed shot networking and authoritative `ShotService`
- `a0c92fe` — three-click controller, HUD, synchronization, and tuned trajectories
- `46cc4f0` — post-shot follow camera
- `e7ce2b1` — stepped-terrain traversal
- `a7adcb7` — practice ball hotkey

## Exit criterion

A player can understand and intentionally influence aim, power, and accuracy
through the complete shot interaction. **Met.**
