# M4 — Holes, Cups, Boundaries, and Scoring: Closeout Record

Status: **Complete** on branch `m4-holes-and-scoring`.

M4 turns individual server-authoritative shots into one complete playable hole with configured tee/cup metadata, physical completion, boundaries, hazards, recovery, scoring, a stroke cap, Pick Up Ball, and clear feedback.

## Delivered

| Area | Result |
|---|---|
| Pure domain | Immutable, Minecraft-free hole definition, AABB boundary, player hole state/session, penalties, status, completion reason, and golf terminology. |
| Configuration | Human-readable `config/minecraft_golf/hole.json` with id, number, dimension, tee, cup, par, and inclusive axis-aligned boundary. Missing config generates a validated default; malformed config fails startup with context. |
| Lifecycle | `/golf hole start` creates and assigns one authoritative tee ball; `/golf hole status` reports progress; `/golf pickup` completes at the cap. |
| Cup/flag | Registered `minecraft_golf:golf_cup` ring block and visual flag; authoritative segment-based cup detection captures and stops valid slow entries. |
| Cup speed | Entries above 0.25 blocks/tick are rejected until the ball exits the capture region, preventing rim slowdown from turning a hard hit into a hole-out. |
| Boundaries | Inclusive axis-aligned 3D boundary with server-side OOB detection. |
| Hazards | Segment-sampled water detection and OOB each add one explicit penalty stroke and recover to the previous legal shot origin. |
| Scoring | Valid launches count exactly one stroke; rejected/cancelled attempts do not. Score-to-par and golf terms are server-owned. |
| Stroke limit | Human-directed default is **Double Par + 2**: Par 3 = 8, Par 4 = 10, Par 5 = 12. Reaching the limit or Pick Up completes the hole at that score. |
| Completion | Holed-out, stroke-limit, and picked-up states are terminal; further shots are rejected. |
| Practice compatibility | Existing shots remain available without scoring when no configured hole session is active. |
| Server validation | Ownership, resting state, active assigned ball, hole completion, held club, legal aim, and six-block strike proximity are revalidated server-side. |
| Feedback | Server messages cover stroke totals, score-to-par, penalties/recovery, Pick Up, rejected shots, and final hole result; hole-out also plays a sound. |
| Putter tuning | Full-power flat NORMAL-ground target increased from 16 to 22 blocks after course playtesting; short-putt control remains power-meter driven. |
| Test environment | Docker startup fixes time at day, pauses time/weather advancement, and clears weather for repeatable testing. |

## Automated verification

Final command:

```text
./gradlew clean test build
```

Result: **passed** with all 10 Gradle tasks executed.

JUnit result:

```text
tests=121 failures=0 errors=0 skipped=0 suites=15
```

Coverage includes:

- inclusive AABB behavior and malformed coordinates,
- hole definition identity/par/position validation,
- Double Par + 2 limits and overflow rejection,
- accepted-shot counting and terminal transitions,
- water/OOB penalty history,
- Pick Up scoring,
- score-to-par and golf terminology,
- immutable last-safe recovery positions,
- strict JSON types and invalid configuration,
- slow, fast, near-miss, and wrong-height cup trajectories,
- all prior ball physics, clubs, swing, networking-domain, surface, and terrain tests.

## Runtime verification

### Loom dedicated server

- Reached `Done` with Minecraft 26.2/Fabric.
- Registered `minecraft_golf:golf_cup`.
- Loaded `family_test:1` as Par 4 with `Double Par + 2 limit 10`.
- Registered `/golf hole start|status`, `/golf pickup`, and existing developer commands.
- Registered M3 shot networking.

### Loom client

- Common and client entrypoints initialized.
- Cup block model loaded without missing-model or missing-texture-reference warnings after adding its particle texture.
- Known environment-only warnings remain: offline development authentication and unavailable optional `flite` narrator library.

### Docker server

- Built JAR and deployed JAR SHA-256 matched:
  `160147d7440568b71f536cf59576a64741b92d0f5cab7b0454855e00c4cc3ff2`.
- Container became healthy.
- M4 block, config, networking, and command registrations appeared before server `Done`.
- Startup RCON commands confirmed:
  - time set to day,
  - `minecraft:advance_time = false`,
  - `minecraft:advance_weather = false`,
  - weather cleared.
- No `net.minecraft.client` imports exist under `src/main`.

## Manual gameplay acceptance

Accepted in the prepared Loom test world:

- dry grass tee-to-cup test lane,
- configured tee ball and visible cup/flag,
- two-putt hole completion,
- server log showed two accepted launches followed by capture at the configured cup center and finalization in two strokes,
- correct post-shot ball resting behavior and camera restoration,
- water penalty adds one stroke and returns the ball to the prior shot origin,
- OOB uses stroke-and-distance semantics: first shot + one penalty means the next shot is “three off the tee,”
- hard cup crossings no longer hole out after the stricter speed gate/latch,
- slow putts still capture successfully,
- increased 22-block Putter range feels appropriate,
- overall M4 gameplay accepted by the user.

Double Par + 2 and Pick Up terminal behavior are additionally covered by deterministic unit tests after the final requested scoring adjustment.

## Deviations and decisions

- Original PRD/architecture default of Double Par was changed by explicit user direction to Double Par + 2 (GSD decision D013).
- The cup uses a low collision ring and visual flag in one registered block; authoritative detection remains independent of visuals.
- M4 remains a single configured-hole lifecycle. No Ready Golf, turn ordering, round manager, or course-authoring system was added.
- Walking to the resting ball remains the MVP behavior. M4 feedback that this may become tedious on full-size holes is recorded for optional on-demand teleport evaluation during M6.

## Known deferred items

- Club item art remains missing-texture placeholders.
- Course-authoring commands/UI remain deferred.
- Multi-client Ready Golf belongs to M5.
- Optional teleport-to-ball evaluation belongs to M6 course playtesting.
- More realistic water/OOB drop rules remain post-MVP.

## Exit criteria

**Met.** One player can start at a configured tee, play server-counted shots, incur deterministic water/OOB penalties with recovery, physically hole out, Pick Up or reach the configured cap, receive correct scoring feedback, and enter a terminal hole-complete state.
