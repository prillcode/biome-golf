# M5 — Single-Player Course Experience: Closeout Record

Status: **Complete** on branch `m5-single-player-course`.

M5 proves the complete single-player course loop before multiplayer concurrency:
a separate practice range, three ordered natural-terrain holes, server-authoritative
sequencing and scoring, safe shot-to-shot travel, a final scorecard, replay, and
full-sized club tuning.

## Delivered

| Area | Result |
|---|---|
| Course domain | Minecraft-free `CourseDefinition`, ordered par 4/3/5 holes, `PlayerCourseState`, cumulative scorecard, terminal-only advancement, current-hole restart, and full replay reset. |
| Runtime course flow | `/golf hole start` begins Hole 1; Holes 1 and 2 expose a clickable `/golf nexthole` transition; Hole 3 automatically finalizes the course and prints the authoritative three-hole scorecard. `/golf hole restart` restarts the current hole during play; completed rounds use `/golf round restart` or `/golf round done`. |
| HUD guidance | Server snapshots drive hole/par, strokes/cap, penalties, score, cumulative course score, cup direction, and authoritative ball-to-cup distance. Swing guidance remains upper-right and scoring remains upper-left. |
| Practice range | Fixed manicured range with long-club markers, wedge target, putting area, bunker, water, ice, and slime lanes. Practice remains outside active course attempts. |
| Natural course | Seed-gated coastal/forest campus with selective vegetation clearing, natural slopes and hazards, blue tee markers, wool tee boxes, irregular greens, supported cups, and distinct par 4, par 3, and par 5 routes. |
| Hole 3 | Completed par 5 with first-drive sightline, two wooded approach corridors, retained route separation, a greenside bunker, irregular green, and supported cup. |
| Generation safety | `minecraft_golf:m5_ocean_campus` v11 is explicit, two-phase, deterministic, bounded to reviewed authored subregions, seed-gated to `-1928790872702396508`, and rejects stateful blocks before writes. |
| Travel flow | When an active owned ball naturally rests, the server searches for a supported, unobstructed nearby standing position and moves the player automatically. Penalties, completion, practice balls, missing balls, and invalid state do not trigger travel. |
| Club tuning | Non-Putters use raised 14–24 block apex targets at 150/125/100/85/60/42 block carry targets and a persisted lofted-shot landing profile with sharply limited rollout. Putter retains standard surface-driven physics, including on green wool resolving as `NORMAL`. |

## Verification

### Automated

- `./gradlew clean test build` — **PASS**
- 172 tests, 0 failures, 0 errors, 0 skipped
- `net.minecraft.client` import guard under `src/main/java` — **PASS**
- `git diff --check` — **PASS**

### Loom dedicated server

Verified on alternate port 25566 while Docker owned 25565:

- dedicated server reached `Done`
- M5 course v11 loaded as three holes, total par 12
- shot and HUD networking registered
- player lifecycle, Pick Up, `/golf nexthole`, and development commands registered

### Docker integration server

Verified:

- container healthy on port 25565
- staged and container JAR SHA-256 identities match
- final JAR loads course `minecraft_golf:m5_development`
- repeated `/golf dev preparecourse` runs settle at zero changed blocks
- layout reports v11 and the documented campus envelope
- `minecraft:advance_time=false`
- `minecraft:advance_weather=false`
- startup sets clear weather

### Manual gameplay acceptance

The user completed the Loom `runClient` checklist and confirmed:

- practice-range use and improved feel across the complete club set
- readable higher non-Putter flights and controlled rollout
- accepted Putter behavior on greens
- automatic safe travel after natural shot rest
- short- and long-shot camera restoration
- completion of Hole 1 par 4, Hole 2 par 3, and Hole 3 par 5
- cup direction and distance HUD guidance
- normal `/golf hole start` sequencing without operator `testhole`
- clickable transitions from Holes 1 and 2
- cumulative scoring and final three-hole scorecard
- automatic finalization after Hole 3
- replay reset returning to Hole 1 with zero strokes and cleared cumulative score

## Accepted findings and known limitations

- Hole 3 currently plays short for a par 5; its route remains playable and accepted for M5. Re-evaluate length during M7 course tuning.
- Driver and Fairway Wood may benefit from slightly more rollout than irons during M7 playtesting; current behavior is accepted.
- A dedicated Wedge/Short Iron chip-shot mode is deferred to M8.
- The large magenta/black first-person club rendering defect remains unresolved and should be prioritized during M6/M7 client testing if still reproducible.
- Course attempts remain server-memory state; persistent global statistics remain out of scope.
- `/golf dev testhole` intentionally selects an isolated operator test hole and does not exercise course sequencing.

## M6 handoff

M6 should add Ready Golf concurrency without changing the proven course or allowing
clients to own outcomes. Preserve:

- server-owned course, score, ball, shot, penalty, and completion state
- independent player-owned balls and targeted cleanup
- per-player course/HUD snapshots and cumulative scorecards
- simultaneous legal shots without turn serialization
- automatic next-shot travel scoped to each player's naturally resting owned ball
- advancement only after every participating golfer is terminal on the current hole
- strict client/common source-set separation and dedicated-server verification

Start with two-player state isolation and the all-players-terminal advancement barrier,
then verify disconnect/reconnect behavior before the four-player LAN acceptance run.
