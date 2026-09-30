# Biome Golf — Product Requirements Document

**Status:** Draft v0.2 — MVP scope-reviewed  
**Platform direction:** Minecraft Java Edition + Fabric  
**Primary initial audience:** Families and small friend groups, especially 1–4 players  

---

## 1. Overview

Biome Golf is a multiplayer Fabric mod that adds an arcade-realistic golf experience to ordinary Minecraft worlds.

The goal is not to recreate professional golf simulation inside Minecraft. Instead, the mod should make it fun for small groups of players to build unusual golf courses directly into Minecraft terrain and then play them together.

The game should embrace Minecraft as part of the golf experience. Players should be able to hit shots across cliffs, through caves, over water, off walls, onto ice, through redstone contraptions, and across intentionally strange terrain.

The initial target audience is families and small friend groups playing on private Minecraft servers, with an emphasis on 1–4 players. The mod should not intentionally restrict larger servers or future public-server use.

---

## 2. Product Vision

Create a golf experience that feels like:

> **Arcade-style multiplayer golf played according to Minecraft's world rules.**

The experience should combine:

- Simple but skill-based golf mechanics
- Minecraft-native course construction
- Multiplayer rounds that move quickly
- Physical interaction between the golf ball and Minecraft terrain
- Real golf scoring and terminology
- Enough realism to make club selection and shot planning meaningful
- Enough absurdity to make impossible Minecraft courses fun

The product should favor fun, experimentation, and replayability over simulation accuracy.

---

## 3. Core Product Principles

### 3.1 Minecraft Is the Course

A golf course should not require an entirely custom terrain system.

The Minecraft world itself forms the course.

Vanilla blocks, terrain, caves, elevation, water, structures, redstone, and other world features should meaningfully affect golf-ball behavior.

Examples:

- Grass can behave like fairway.
- Tall grass or similar terrain can behave like rough.
- Sand can behave like a bunker.
- Water can act as a hazard.
- Ice can dramatically reduce rolling friction.
- Slime can cause exaggerated bounce.
- Honey can dramatically slow a ball.
- Stone walls can be used intentionally for bank shots.
- Caves, cliffs, waterfalls, bridges, towers, and player-built structures can become parts of holes.

A small number of custom golf-specific blocks and items are acceptable where they improve gameplay.

### 3.2 Fun Before Simulation

Golf should feel believable without attempting to model professional golf physics precisely.

Players should recognize concepts such as:

- club distance
- loft
- elevation
- power
- accuracy
- hazards
- par

More detailed lie, wind, and terrain penalties may be added after the MVP.

### 3.3 Player Skill Matters

Shots should not be determined only by choosing a club and clicking.

Players should have meaningful control over:

- aim
- club
- power
- accuracy

Better execution should generally result in better shots. Randomness should be minimized.

### 3.4 Multiplayer Should Keep Moving

The default multiplayer experience is **Ready Golf**.

Players may prepare and hit their own shots without waiting for strict turns. Each player's ball is tracked independently.

Traditional turn-based golf is intentionally deferred until after MVP.

### 3.5 Build, Play, Rebuild

Course creation should eventually be a major part of the experience.

Players should be able to:

1. Build terrain using normal Minecraft tools.
2. Define tees and cups.
3. Set hole metadata such as par.
4. Play the course.
5. Modify the build.
6. Play it again.

For MVP, course definitions may be configured manually rather than authored through polished in-game tools.

---

## 4. Target Experience

A typical MVP session should look like this:

1. Players join a Fabric-enabled Minecraft server.
2. Players join or start a golf round.
3. A preconfigured three-hole course is selected.
4. Players begin at Hole 1.
5. Each golfer equips a golf ball and clubs.
6. Players select clubs and aim using their Minecraft camera.
7. Players use a three-click swing mechanic.
8. Golf balls physically travel through the Minecraft world.
9. Players walk to their stopped balls.
10. Players continue independently under Ready Golf rules.
11. Players hole out or reach the stroke limit.
12. The game displays hole scores.
13. Players proceed to the next tee.
14. At the end of the round, a final scorecard is shown.

The experience should retain normal Minecraft movement and exploration rather than constantly placing players into a separate minigame interface.

---

## 5. Core Shot Mechanic

The primary shot mechanic should use a three-click swing system.

### Click 1 — Start Swing

The shot meter begins moving.

### Click 2 — Set Power

The location of the meter determines shot power.

Players may intentionally choose less than maximum power.

### Click 3 — Set Accuracy

The meter returns toward an accuracy zone.

The player's timing determines directional accuracy.

A near-perfect click should produce a shot close to the intended line. Missing the accuracy zone should cause the ball to deviate left or right.

The mechanic should be deterministic enough that players feel responsible for good and bad shots.

---

## 6. Aiming System

Players aim primarily using their Minecraft camera direction.

For MVP, the HUD should show only what is necessary to make a shot:

- selected club
- nominal club distance
- shot power meter
- accuracy meter
- basic aim direction feedback

An approximate landing-area indicator is **not required for MVP** and may be added later after the basic shot system is fun and predictable.

Elevation should naturally affect shots. Uphill shots should require more effective distance, while downhill shots should generally carry or roll farther.

---

## 7. Camera

### MVP

After impact, provide a simple ball-follow camera or view that makes it easy to watch the shot through landing and roll.

The implementation should prioritize reliability over cinematic presentation.

### Post-MVP / V1

Add a cinematic shot camera that:

1. follows the initial flight,
2. frames the landing and roll,
3. returns control to the golfer after the ball stops.

---

## 8. Clubs

Clubs should exist as physical Minecraft inventory items for the initial implementation.

The initial club set should include approximately:

- Driver
- Fairway Wood
- Long Iron
- Mid Iron
- Short Iron
- Wedge
- Putter

Exact iron numbers may be chosen during implementation rather than modeling every standard club immediately.

Each club should define a small set of gameplay properties, such as:

- nominal distance
- launch angle
- power multiplier
- accuracy tolerance

The system should be data-driven enough that additional clubs can be introduced later.

---

## 9. Clubs as Weapons

Golf clubs may also function as simple Minecraft melee weapons.

This is a secondary feature and must not complicate MVP golf mechanics.

Possible characteristics:

- Driver — relatively high damage, slower attack
- Irons — moderate damage
- Wedge — moderate or light damage
- Putter — low damage

No combat progression system is planned.

---

## 10. Golf Ball

Each golfer owns an independently tracked golf ball during a round.

Players should not be able to accidentally strike another golfer's ball.

Golf balls should be visually distinguishable where reasonably possible through simple means such as:

- player-specific color accents
- colored bands
- subtle identifiers

Do not make the ball unnaturally large purely for identification.

---

## 11. Ball Physics

The ball should physically move through the Minecraft world.

Core MVP behaviors:

- launch
- gravity
- collision
- bounce
- rolling
- friction
- stopping

Balls should interact with Minecraft blocks.

Players should be able to intentionally use the environment, including:

- bank shots off walls
- bouncing from cliffs
- launching from slime
- rolling across ice
- striking trees or structures
- navigating caves
- interacting with constructed obstacles

These interactions are central to the product identity.

---

## 12. Terrain and Lies

### MVP

Keep terrain behavior simple.

Surfaces should affect:

- bounce
- rolling friction
- stopping behavior

Important initial terrain types include:

- normal grass-like terrain
- sand
- water
- ice
- slime
- honey

Detailed fairway, rough, bunker, and green shot penalties are **not required for MVP**.

### Post-MVP / V1

Add richer lie behavior where useful, such as:

- fairway — normal club performance
- rough — moderately reduced distance and/or accuracy
- bunker — significantly reduced effective distance
- green — optimized putting behavior

The system should remain easy to understand and tune.

---

## 13. Custom Golf Blocks and Items

The mod may add a limited number of golf-specific blocks, items, or entities where vanilla Minecraft lacks a good equivalent.

Expected examples include:

- Golf Cup
- Flag / Pin
- Tee Marker
- Golf Ball
- Golf Clubs

A custom green surface is optional and should not be required for MVP unless putting quality demands it.

Most course terrain should remain ordinary Minecraft blocks.

---

## 14. Course Definition

A course consists primarily of metadata applied to locations within a Minecraft world.

One Minecraft world may contain multiple independent golf courses.

Each hole minimally requires:

- course identifier
- hole number
- tee position
- cup position
- par
- playable boundary

Everything else should be inferred from the Minecraft world wherever practical.

---

## 15. Course Boundaries

Each hole must define a playable boundary so golfers cannot continue indefinitely through the entire Minecraft world.

For MVP, the boundary system should use the **simplest implementation that works reliably**, preferably a rectangular or box-like region.

Leaving the playable area triggers out-of-bounds handling.

More flexible polygonal or multi-zone course boundaries are explicitly deferred.

---

## 16. Course Creation

Polished course-authoring tools are not required for MVP.

MVP courses may be defined through configuration files, development commands, or other simple administrative mechanisms.

M8 should introduce a bounded, operator-facing version of convenient in-game
commands, such as:

```text
/golf course create pine-hills
/golf hole create 1
/golf hole tee
/golf hole cup
/golf hole par 4
```

Commands should generally operate using the administrator's current position.

The initial authoring system should define metadata on existing Minecraft terrain;
it should not attempt to provide a visual editor, arbitrary terrain generation, a
course marketplace, or external persistence infrastructure.

---

## 17. Distances and Scale

Golf distances should be compressed relative to real-world golf.

A literal real-world distance scale would make Minecraft courses unnecessarily large.

The mod should establish a gameplay-specific scale where:

- par 3 holes feel meaningfully shorter,
- par 4 holes allow multiple-shot strategy,
- par 5 holes feel long without requiring excessive travel.

Displayed distances may use golf-style yardage even when the internal block scale is compressed.

The exact conversion should be determined through playtesting rather than fixed prematurely in the PRD.

---

## 18. Multiplayer

The primary multiplayer target is 1–4 players.

This is a design target rather than a hard server limit.

The architecture should avoid unnecessary assumptions that make larger groups impossible later.

---

## 19. Ready Golf

Ready Golf is the only required multiplayer play order for MVP.

Players may hit whenever they are ready.

Each player's golf state is independent.

Example:

```text
Aaron      Shot 2
Max        Shot 1
Seb        Shot 3
Dietrich   Shot 2
```

Players should be able to see other golfers and their shots occurring live where technically reasonable.

The hole advances after all participating players have either:

- holed out, or
- reached/picked up at the stroke limit.

Traditional turn-based golf is deferred until V1 or later.

---

## 20. Moving Between Shots

For MVP, golfers physically walk through the Minecraft world to their stopped ball.

Teleport-to-ball is **not required for MVP**.

It may be added after playtesting if walking between shots proves tedious.

---

## 21. Moving Between Holes

For MVP, players may either travel to the next tee normally or use a simple automatic/administrative transition if necessary.

A polished **Teleport to Next Tee** interaction is desirable for V1 but should not block MVP completion.

---

## 22. Hole Completion

Players must physically sink the golf ball into the cup.

Simply approaching the hole should not count.

The cup interaction should be visually and audibly satisfying because sinking a putt is one of the most important feedback moments in the game.

---

## 23. Scoring

The mod should use real golf scoring terminology.

Supported concepts include:

- Eagle
- Birdie
- Par
- Bogey
- Double Bogey
- Additional over-par scoring

Players should see:

- current-hole strokes
- score relative to par
- hole result
- round total

A final scorecard should appear after the round.

---

## 24. Stroke Limit

Courses or servers may configure a maximum number of strokes per hole.

The default is **Double Par + 2**.

Examples:

- Par 3 → maximum 8
- Par 4 → maximum 10
- Par 5 → maximum 12

Players should also have a simple **Pick Up Ball** action.

When the limit is reached, the golfer completes the hole at the configured maximum score.

---

## 25. Hazards and Penalties

Hazard rules should be intentionally simple and deterministic.

### Water

Current behavior:

- add one penalty stroke,
- drop the ball on the nearest safe land near the point where it first touched water,
  walking back along the incoming shot line first, then widening into a cone, then a
  bounded search in any direction around the entry point,
- skip golf hazard surfaces (bunker sand, honey) while any non-hazard land is available,
  so a bunker at the water's edge never becomes the drop,
- if no non-hazard land exists, accept a hazard surface rather than give the whole shot
  distance back, and only if that also fails return the ball to the previous-shot
  position.

The drop anchors on the first hazard-fluid contact (water or lava) and never lands in
fluid, and is server-authoritative like the rest of the round state. A ball that skips
across water without settling still incurs the penalty; distinguishing a skipped shot
from one that lands in the hazard is a separate physics improvement.

### Out of Bounds

Default MVP behavior:

- add one penalty stroke,
- return the ball to the previous-shot position.

More realistic entry-point/drop-zone logic for out-of-bounds play is deferred.

Because the server tracks every golf ball, a traditional lost-ball mechanic is unnecessary.

---

## 26. Game Modes

### MVP

#### Stroke Play

Players compete based on total strokes.

### V1 Roadmap

#### Scramble

All members of a team hit, the team chooses one resulting position, and the team continues from there.

Scramble should be the first major alternate mode after core stroke play is proven fun.

### Future

Potential modes include:

- Traditional Turn-Based Stroke Play
- Match Play
- Best Ball
- Closest to the Pin
- Longest Drive
- Speed Golf

---

## 27. Minecraft Survival Interaction

MVP should avoid building a custom survival-rules subsystem.

Golf should initially coexist with the server/world's normal Minecraft rules.

Server operators can use ordinary Minecraft settings such as difficulty, gamerules, or peaceful mode when they want a distraction-free golf experience.

Golf clubs may interact with mobs as ordinary melee weapons.

A future V1 configuration system may optionally control golf-specific behavior such as:

- hostile mob interference
- hunger
- PvP during rounds
- environmental damage

This configuration system is **not required for MVP**.

Golf balls should not allow other golfers to intentionally disrupt a shot simply by standing in the ball's path.

### Player Modes (V1, M8.15)

The supported Java client has three server-owned player modes over the same
Minecraft world. They are a deliberate product decision, not server survival rules:

- **Golf** — the default for a supported newcomer. Survival game type plus
  Creative-style flight and damage protection, sharing the normal Survival
  inventory. This is deliberately **flight-only, not literal Creative**: it does not
  grant `instabuild` or a Creative item catalogue, so it cannot leak free items into
  Survival play.
- **World** — ordinary Survival/Peaceful world play, including mining and building
  outside protected course landscapes.
- **Builder** — an operator-scoped construction mode for one finalized course with an
  authored landscape perimeter: flight plus an isolated, replenishable palette loadout
  drawn from `config/minecraft_golf/builder_palette.json`. Placement is
  server-enforced against the palette and the perimeter; the normal World inventory is
  preserved and restored on exit.

Visitors remain the existing Survival/Spectator policy and never receive Java
Golf/Builder abilities. The server-wide default stays Survival with
`force-gamemode=false`. Peaceful remains a server-wide difficulty, not a per-player
mode. Cup and flag assembly stays command-managed (`/golf hole cup <n>`), and the
palette is decorative construction material only.

---

## 28. Wind

Wind is not required for MVP.

Wind is planned for V1.

It should influence ball flight without dominating the game and should clearly communicate:

- wind direction
- wind strength

Wind should be predictable enough for players to learn compensation.

---

## 29. Mini Golf

The core engine should avoid assumptions that prevent putting-only or mini-golf-style courses.

Dedicated mini-golf functionality is not an initial priority.

---

## 30. Progression

MVP and initial V1 should contain no gameplay-affecting progression.

Do not initially implement:

- player levels
- statistically superior clubs
- equipment rarity
- unlockable power advantages
- currency
- pay-to-progress systems

All golfers should have mechanically equivalent equipment.

Player success should come from:

- shot execution
- course knowledge
- aiming
- power judgment
- club selection

Progression or cosmetics may be considered later if the project proves popular.

---

## 31. MVP Definition

The MVP is complete when:

> **Four players can join a Fabric Minecraft server, equip golf clubs, play a manually configured three-hole course using Ready Golf, independently hit physically simulated golf balls through the Minecraft world, hole out, and receive a completed scorecard.**

### Required for MVP

- Fabric-based Minecraft multiplayer
- 1–4 golfers
- physical golf-ball entity
- player-owned balls
- visually distinguishable balls where practical
- Driver
- Fairway Wood
- several representative irons
- Wedge
- Putter
- physical club inventory items
- camera-based aiming
- three-click shot mechanic
- power control
- accuracy control
- simple shot HUD
- simple shot-follow camera
- elevation-sensitive ball flight
- world/block collision
- bouncing
- rolling
- friction
- stopping
- basic surface differentiation
- cup / hole interaction
- tee location
- simple course metadata
- simple rectangular/box hole boundaries
- Ready Golf
- simultaneous independent player state
- walking between shots
- stroke counting
- par
- Double Par + 2 default stroke limit
- Pick Up Ball
- simplified water penalty
- simplified out-of-bounds penalty
- standard golf scoring terminology
- hole results
- final round scorecard
- three-hole playable test course

### MVP Quality Bar

The MVP is not complete merely because the features technically function.

At minimum:

1. Hitting the ball must feel responsive.
2. Ball flight must be readable and reasonably predictable.
3. The ball must interact consistently with Minecraft terrain.
4. Putting into the cup must feel satisfying.
5. Four-player Ready Golf must remain understandable rather than chaotic.
6. A complete three-hole round must be playable without developer intervention once started.

---

## 32. Explicitly Out of MVP Scope

The following should **not** block MVP completion:

- cinematic shot camera
- approximate landing-area preview
- full projected trajectory
- wind
- advanced lie penalties
- detailed rough behavior
- detailed bunker behavior
- custom green terrain system
- full 14-club golf bag
- traditional turn mode
- teleport-to-ball
- polished teleport-to-next-tee UI
- polished in-game course-authoring commands
- visual course editor
- polygonal or complex course boundaries
- realistic water drop locations (delivered post-MVP; see section 25)
- realistic out-of-bounds drop rules
- custom golf-specific mob/hunger/PvP controls
- player progression
- unlockable equipment
- cosmetics
- matchmaking
- public server infrastructure
- persistent leaderboards
- career statistics
- achievements
- tournaments
- scramble
- match play
- mini-golf-specific systems
- AI golfers
- downloadable course marketplace
- custom world generation
- golf carts

---

## 33. Post-MVP / V1 Priorities

After family playtesting proves the core loop is fun, V1 should prioritize improvements based on observed friction.

Likely priorities include:

1. Cinematic shot camera
2. Wind
3. Better terrain and lie behavior
4. Approximate landing-area preview
5. Teleport-to-ball if walking becomes tedious
6. Teleport-to-next-tee UX
7. Scramble mode
8. Traditional turn mode
9. Improved course boundary tools
10. Expanded golf-specific configuration
11. More polished HUD, sound, particles, and visual feedback

The exact order should be revised after real family playtesting.

---

## 34. Success Criteria

The project should initially be evaluated through qualitative playtesting rather than adoption or revenue metrics.

Primary questions:

1. Is hitting the golf ball satisfying?
2. Does the three-click swing feel learnable but skill-based?
3. Can younger players understand how to play without extensive instruction?
4. Does Ready Golf keep multiplayer rounds moving?
5. Is walking through the Minecraft course enjoyable?
6. Are Minecraft terrain interactions fun and predictable?
7. Do players naturally want to build new holes?
8. Do players want to replay courses?
9. Do unusual Minecraft course designs produce memorable moments?
10. After completing a round, do players want to play another?

The strongest success signal is:

> **The family voluntarily wants to launch the mod and play again.**

---

## 35. Product Identity

Biome Golf should not position itself primarily as a realistic golf simulator.

Its identity is:

> **A multiplayer arcade-golf system where Minecraft itself becomes the golf course.**

The game should encourage courses that would be impossible in real life, including:

- mountain-top tee shots
- cave holes
- massive drops
- castle courses
- shots across ravines
- water features
- ice fairways
- slime launch zones
- moving redstone obstacles
- underground greens
- Nether-themed courses
- End-themed courses

The most memorable rounds should come from the intersection between golf mechanics and Minecraft creativity.

---

## 36. Long-Term Possibilities

These are intentionally not commitments.

If the core game becomes successful, future exploration could include:

- larger public servers
- persistent player statistics
- handicaps
- tournaments
- leagues
- achievements
- cosmetic golf balls
- cosmetic clubs
- golf outfits
- downloadable courses
- course ratings
- course sharing
- server leaderboards
- personal records
- replay systems
- shot statistics
- driving ranges
- challenges
- golf carts
- additional game modes
- player progression
- optional equipment systems

All long-term development should remain subordinate to the core principle:

> **Golf must remain simple to understand, satisfying to play, and uniquely Minecraft.**

---

## 37. Scope Review Rationale

The MVP was intentionally narrowed to answer one product question as quickly as possible:

> **Is playing multiplayer golf inside Minecraft fun enough that the family wants another round?**

Several originally discussed features were moved out of MVP because they add implementation and testing cost without being necessary to answer that question.

### Deferred: Approximate landing indicator

Useful for polish and shot planning, but accurate prediction requires duplicating or approximating ball simulation on the client. Basic aiming plus club distance is enough for initial playtesting.

### Deferred: Traditional turn mode

Ready Golf is the defining default multiplayer experience. Supporting two play-order state machines before testing the first one creates unnecessary complexity.

### Deferred: Teleport-to-ball

Walking to the ball reinforces Minecraft exploration. Teleportation should only be added if real playtesting proves traversal tedious.

### Deferred: Golf-specific mob/hunger/PvP controls

Minecraft already provides difficulty and gamerules. A custom rules subsystem does not improve the core golf mechanic enough to justify MVP complexity.

### Simplified: Hazard and out-of-bounds recovery

Water recovery now drops near the hazard entry point (section 25), which keeps the lost
distance from a shot that rolled into water believable. Out-of-bounds recovery is still
simplified: returning the ball to its previous-shot location is easy to understand and
deterministic, and requires much less geometric/state logic than calculating legal
drop areas.

### Simplified: Course boundaries

A simple rectangular or box-like boundary proves the gameplay requirement. Sophisticated course shapes can come later.

### Simplified: Terrain lies

MVP terrain differences primarily affect ball motion. Shot penalties from rough, bunkers, and other lies can be layered in once basic physics feel good.

### Kept in MVP: Multiple clubs

Although a three-club prototype would be smaller, club selection is central to the intended golf experience. A compact but recognizable bag helps test whether distance and loft choices are fun.

### Kept in MVP: Three-click swing

This is the primary player-skill mechanic and should be validated from the beginning rather than added after physics are already tuned around simpler controls.

### Kept in MVP: Four-player Ready Golf

Multiplayer is not a later enhancement; it is part of the product thesis. The first meaningful MVP should validate the actual family use case rather than only a single-player physics sandbox.
