# M4 — Holes, Cups, Boundaries, and Scoring: Implementation Notes

Status: **Complete** on branch `m4-holes-and-scoring`. See `docs/M4-CLOSEOUT.md`.

## Delivered

- Minecraft-free immutable hole/scoring domain:
  - `HoleDefinition` and inclusive axis-aligned `HoleBoundary`
  - `PlayerHoleState` and `PlayerHoleSession`
  - explicit status, completion reason, penalty type, and golf score terminology
- Default Double Par + 2 limit, Pick Up Ball, score-to-par, and terminal hole state.
- Human-readable single-hole configuration at `config/minecraft_golf/hole.json`.
  - Generated on first start with id, number, dimension, tee, cup, par, and boundary.
  - Invalid or malformed configuration fails startup with the config path in the error.
- Server-owned active-hole lifecycle and assigned ball.
- `/golf hole start`, `/golf hole status`, and `/golf pickup` player commands.
- Registered `minecraft_golf:golf_cup` collision-ring block with a visual pin/flag.
- Server-side slow-speed cup-entry detection and ball capture.
- Segment-sampled water detection and axis-aligned out-of-bounds detection.
- One-stroke water/OOB penalties with recovery to the previous legal shot origin.
- `ShotService` integration:
  - only validated, launched shots count;
  - existing no-hole practice shots remain unscored and available;
  - active-hole players may strike only their assigned ball;
  - complete/capped players cannot continue.
- Server messages for strokes, penalties, score-to-par, Pick Up, and completion.
- Course-playtest tuning increased full-power flat NORMAL-ground Putter carry from 16 to 22 blocks, leaving controlled reserve for a 16-block green.
- Overspeed cup crossings are rejected until the ball exits the capture region; the stricter 0.25-block/tick limit prevents hard putts from being captured after rim slowdown.

## Default test hole

The generated default targets the repository Docker test world's flat spawn area:

- Dimension: `minecraft:overworld`
- Tee ball center: `[0.5, 63.25, 0.5]`
- Cup ball center: `[16.5, 63.25, 0.5]`
- Par: 4
- Boundary: `[-32, -64, -32]` through `[64, 384, 32]`

For another world, stop the server and edit its generated `config/minecraft_golf/hole.json` before testing.
Coordinates are ball-center coordinates. Tee and cup must both be inside the boundary.

## Verification completed

- `./gradlew clean test build` — passed.
- 121 tests passed; zero failures/errors/skips.
- Loom dedicated server reached `Done` on an alternate port while Docker occupied 25565.
- Loom client initialized the common and client entrypoints. The environment still reports the known offline-auth and missing optional narrator-library warnings.
- Docker JAR SHA-256 matched the Gradle-built JAR.
- Docker server became healthy and logged cup, configured-hole, networking, and command registration.
- No `net.minecraft.client` imports exist under `src/main`.

## Manual gameplay acceptance

1. Connect to the Docker server and run `/golf hole start`.
2. Confirm teleport to the tee, assigned resting ball, visible cup/flag, and Par 4 feedback.
3. Take valid shots and confirm exactly one stroke per launched shot.
4. Cancel/reject a shot and confirm no stroke is counted.
5. Run `/golf hole status` and confirm stroke/limit/relative-score feedback.
6. Sink a slow putt physically; confirm capture, sound, golf term, final score, and blocked further shots.
7. Replay and enter water; confirm one penalty and recovery to the prior shot origin.
8. Replay and leave the boundary; confirm one penalty and the same recovery behavior.
9. Reach ten strokes on the Par 4; confirm Double Par + 2 completion and blocked further shots.
10. Replay, run `/golf pickup`, and confirm an eight-stroke completed score and ball removal.

Do not begin M5 Ready Golf or multiplayer round orchestration during M4 closeout.

## Deferred playtest feedback

- The post-shot camera correctly restores to the stationary player at the previous shot origin. Walking to the resting ball may become tedious on full-size holes; evaluate an optional on-demand teleport-to-ball interaction during M6 course playtesting rather than changing M4 traversal semantics.
