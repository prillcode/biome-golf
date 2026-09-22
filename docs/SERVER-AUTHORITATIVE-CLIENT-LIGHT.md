# Server-Authoritative, Client-Light Architecture Proposal

> **Assessed.** See `docs/BEDROCK-COMPATIBILITY-ASSESSMENT.md` for the answers to this
> proposal's ten evaluation questions. Verdict: **practical with tradeoffs**. Tracked as
> proposed milestone M10 in `docs/MILESTONES.md`; not started.

## Context

BirdieBiome is being developed as a Java Edition Minecraft golf mod, currently targeting a Fabric-based Java server.

The original project direction assumed Java clients and therefore allowed for normal Fabric client/server mod behavior. However, there is a strong reason to explore whether BirdieBiome can be designed so that most gameplay logic lives on the server while the client remains as lightweight and vanilla-compatible as practical.

The primary motivation is future **Bedrock Edition compatibility**.

A Java Minecraft server can accept Bedrock clients through projects such as **GeyserMC**, with **Floodgate** optionally handling Bedrock authentication. Geyser translates Bedrock protocol traffic into Java protocol traffic, but Bedrock clients cannot run a Java Fabric client mod. Therefore, any BirdieBiome feature that depends on a required Java client mod may become a blocker for Bedrock cross-play.

The goal is **not** to require Bedrock support for the MVP. The goal is to avoid unnecessary architectural decisions now that would make Bedrock support impractical later.

This document proposes a server-authoritative, client-light direction for the project and asks the implementation agent to evaluate how practical it would be within the current architecture.

---

## Primary Goal

Design BirdieBiome so that the **server owns the authoritative golf simulation and game state**, while clients consume the experience primarily through vanilla-compatible Minecraft mechanics whenever feasible.

The ideal outcome would be:

- Java players can connect with little or no required client-side mod functionality.
- Core gameplay does not depend on custom client-only rendering or networking.
- Geyser/Floodgate compatibility remains technically plausible.
- Bedrock support can be added later without rewriting the core golf engine.
- Java clients may still receive optional enhancements from a client mod when those enhancements are worthwhile.

This should be treated as an architectural preference, not an absolute rule. If an important gameplay feature cannot reasonably be implemented without client-side support, the agent should identify that clearly rather than forcing an inferior implementation.

---

## Proposed Architectural Principle

Use the following hierarchy when deciding where a feature should live:

1. **Server-only implementation using vanilla-compatible mechanics** — preferred.
2. **Server-authoritative implementation with optional Java client enhancement** — acceptable and likely ideal for some features.
3. **Required Java client mod functionality** — use only when the gameplay benefit clearly outweighs the loss of Bedrock compatibility.

A feature should not be moved client-side merely because it is easier to implement there if a practical server-side implementation exists.

---

## Server Responsibilities

The server should own as much of the actual golf game as possible.

### Golf Ball State and Physics

The server should be authoritative for:

- Ball position.
- Ball velocity.
- Shot direction.
- Shot power.
- Club modifiers.
- Gravity and trajectory.
- Ground collision.
- Bounces.
- Rolling.
- Friction.
- Terrain interaction.
- Water hazards.
- Sand or bunker behavior.
- Out-of-bounds handling.
- Ball-to-ball collision, if retained.
- Ball rest detection.

Clients should display the resulting state rather than independently deciding the outcome of the shot.

This avoids different clients calculating different golf outcomes and also keeps the physics engine usable by Bedrock clients.

### Course Definitions

Course data should be server-owned.

Examples include:

- Course boundaries.
- Hole definitions.
- Tee locations.
- Pin/cup locations.
- Par values.
- Hazard definitions.
- Out-of-bounds regions.
- Maximum stroke rules.
- Hole ordering.
- Course metadata.

The world itself should remain the visual source of truth. BirdieBiome should ideally describe how an existing portion of the Minecraft world functions as a golf course rather than requiring clients to download a custom representation of the course.

### Match and Session State

The server should manage:

- Player registration.
- Ready state.
- Turn order.
- Current player.
- Current hole.
- Player ball locations.
- Stroke counts.
- Scores.
- Penalties.
- Match status.
- Winner/final standings.
- Join-in-progress behavior, if supported.
- Disconnect/reconnect behavior.

### Rule Enforcement

Rules should be enforced server-side, including:

- Whether a player is allowed to hit.
- Which ball belongs to which player.
- Whether a ball is considered stationary.
- Whether a player can move while taking a shot.
- Whether a club is valid for the current action.
- Whether the shot exceeds legal power limits.
- Whether a ball is in a hazard.
- Hole completion.
- Maximum stroke/double-par rules.

The client may suggest an action, but the server should validate and execute it.

---

## Client-Light Interaction Model

Where possible, BirdieBiome should communicate gameplay using mechanics that normal Minecraft clients already understand.

Possible techniques include:

### Vanilla Entities

Represent golf balls with a vanilla-compatible entity or an entity that can be translated into a vanilla representation.

The project should evaluate options such as:

- Small armor stands.
- Item displays.
- Interaction entities paired with displays.
- Snowballs or other projectile-like entities when appropriate.
- Other vanilla entities with customized appearance through server/resource-pack mechanisms.

The key question is whether the selected representation works through Geyser and remains controllable enough for accurate golf physics.

A custom Java entity should be avoided if its existence requires a Java client mod to understand or render it.

### Vanilla Items for Clubs

Clubs could potentially be represented using existing Minecraft items with:

- Custom names.
- Lore.
- Custom model data.
- Resource-pack models.
- Server-side metadata identifying the logical club type.

For example, the Java server could treat several visually customized vanilla items as:

- Driver.
- Fairway wood.
- Iron.
- Wedge.
- Putter.

The agent should evaluate what subset of custom item appearance can be made compatible with both Java clients and Geyser-connected Bedrock clients.

### Existing Minecraft Interaction Events

Prefer normal client actions such as:

- Right-click/use item.
- Left-click/attack.
- Sneak.
- Jump.
- Hotbar selection.
- Movement/look direction.

These actions are naturally communicated to the server and are more likely to translate cleanly through Geyser.

Avoid requiring custom Fabric keybind packets for core gameplay unless no viable vanilla interaction exists.

---

## Shot Input Proposal

The shot mechanic is one of the most important areas to design carefully.

A desirable cross-platform shot system would use information the vanilla server already receives.

Potential model:

1. Player selects a club using a normal inventory/hotbar item.
2. Player stands near their ball.
3. Player aims using normal camera/look direction.
4. Player begins shot preparation using a normal interaction such as holding use/right-click.
5. Shot power increases over time on the server.
6. Player releases the interaction to execute the shot.
7. Server calculates direction, power, club characteristics, terrain interaction, and resulting physics.

Alternative input patterns should be evaluated if Minecraft does not provide reliable press/release timing through normal interactions.

The goal is to avoid a required custom Java UI for swing power if a usable server-driven mechanic can provide the same gameplay.

---

## HUD and User Interface

This is likely one of the largest areas where a fully vanilla-compatible implementation differs from a traditional client mod.

### Preferred Vanilla-Compatible UI Channels

Evaluate use of:

- Action bar.
- Boss bar.
- Titles/subtitles.
- Chat messages.
- Scoreboard sidebar.
- Tab list.
- Item names/lore.
- Inventory/container interfaces.

These can communicate information such as:

- Current hole.
- Par.
- Stroke count.
- Current club.
- Shot power.
- Distance to pin.
- Current turn.
- Leaderboard.

### Optional Java Client Enhancement

A Java client mod could optionally replace or augment those interfaces with a cleaner BirdieBiome HUD.

For example:

**Without the client mod:**

> Hole 4 | Par 3 | Stroke 2 | 84 blocks to pin

appears through the action bar and scoreboard.

**With the client mod:**

The same server state could be presented as a polished golf HUD.

The important architectural principle is that the optional client UI should consume server state rather than becoming the authoritative source of gameplay logic.

---

## Camera System

The planned simple follow camera is probably the feature most likely to conflict with the client-light goal.

A custom Java client can provide much richer camera behavior than a vanilla/Bedrock-compatible approach.

The agent should evaluate at least three possibilities:

### Option A — No Required Custom Camera

Use the player's normal first-person or third-person camera.

After the shot, the player observes the ball naturally or follows it by moving/looking.

This provides maximum compatibility but may be less satisfying.

### Option B — Server-Controlled Spectator Technique

Explore whether temporary spectator-style viewing or server-controlled entity targeting can provide a ball-follow experience using vanilla client functionality.

This must be tested carefully for:

- Java behavior.
- Geyser behavior.
- Multiplayer isolation.
- Movement restrictions.
- Reliable restoration of the player's previous state.

### Option C — Optional Enhanced Java Camera

Make ball-follow camera behavior an optional Java-client enhancement.

Java users with the BirdieBiome client component receive the smooth follow camera.

Vanilla Java and Bedrock users receive a simpler fallback experience.

This may represent the best compromise if custom camera behavior proves inherently client-specific.

---

## Networking

Core game networking should prefer standard Minecraft synchronization when practical.

Avoid making custom Fabric networking packets mandatory for basic gameplay.

Custom server-to-client packets may still be useful for optional Java enhancements, but the server should ideally maintain a fallback representation for clients that do not understand them.

Conceptually:

```text
                    BirdieBiome Server
                           |
          +----------------+----------------+
          |                                 |
   Vanilla-compatible                 Optional custom
   Minecraft state                    BirdieBiome packets
          |                                 |
    +-----+------+                          |
    |            |                          |
Java vanilla   Geyser                    Java client
client         translation               enhancement mod
                 |                         installed
              Bedrock
               client
```

The left side should remain capable of completing an entire round of golf.

---

## Resource Packs

Resource packs may be an important tool for making a server-authoritative design visually appealing without requiring a traditional client mod.

Potential uses include:

- Golf club models.
- Golf ball appearance.
- Tee markers.
- Pin/flag visuals.
- Course signage.
- UI textures where supported.

The agent should investigate how Java server resource packs interact with Geyser and whether Geyser can translate or provide corresponding Bedrock resource-pack assets.

Do not assume Java resource-pack behavior automatically works for Bedrock.

If asset translation requires a separate Bedrock pack, that may still be significantly easier to support than recreating the Java Fabric client mod.

---

## Data Model Recommendation

Core game state should be independent of client representation.

For example, avoid designing the domain model around a particular Minecraft entity implementation.

Prefer logical concepts such as:

```text
GolfBall
  playerId
  worldId
  position
  velocity
  state
  lie
  strokes

GolfHole
  courseId
  holeNumber
  teePosition
  pinPosition
  par
  boundaries
  hazards

GolfPlayer
  playerId
  ballId
  activeClub
  score
  turnState
```

Then separately map those concepts to Minecraft entities and visual representations.

This separation makes it easier to support:

- Different Java client experiences.
- Geyser translation.
- Future Bedrock-specific visual adapters.
- Automated testing.
- Server simulation without a live client.

---

## Suggested Module Boundaries

The implementation should consider separating BirdieBiome into logical layers such as:

```text
birdiebiome-core
  Golf rules
  Course model
  Physics model
  Scoring
  Match state

birdiebiome-server
  Fabric hooks
  Minecraft entity management
  Player interactions
  Persistence
  Server commands

birdiebiome-client (optional)
  Enhanced HUD
  Follow camera
  Visual effects
  Client-only polish

birdiebiome-compat (optional/future)
  Geyser/Floodgate handling
  Resource-pack translation support
  Platform-specific fallbacks
```

The exact Gradle/project structure does not need to follow these names, but the separation of concerns would be valuable.

In particular, the physics engine and golf rules should not depend directly on client-only Minecraft classes.

---

## Features That Should Be Server-Authoritative

Unless there is a strong technical reason otherwise, the following should live on the server:

- Course creation and editing.
- Hole definitions.
- Tee placement.
- Cup/pin placement.
- Course boundaries.
- Hazards.
- Golf ball state.
- Ball physics.
- Swing validation.
- Club selection validation.
- Stroke counting.
- Hole completion.
- Player turn state.
- Match state.
- Scoreboard state.
- Win conditions.
- Penalties.
- Teleportation or player repositioning.
- Ball reset/drop rules.
- Multiplayer synchronization.

---

## Features That Could Be Optional Client Enhancements

These should be evaluated as optional rather than required:

- Smooth ball-follow camera.
- Custom HUD.
- Swing meter graphics.
- Ball trajectory preview.
- Club-selection GUI.
- Advanced particle effects.
- Custom sounds beyond what a vanilla/resource-pack client can support.
- Client-side course editor visualization.
- Spectator camera improvements.

The fallback experience should still allow a player to complete a round.

---

## Features Most Likely to Threaten Bedrock Compatibility

The agent should specifically identify any current or planned functionality that relies on:

- Custom Fabric entities requiring client registration.
- Custom Fabric blocks requiring client registration.
- Custom Java GUI screens.
- Custom client keybindings.
- Client-only physics calculations.
- Client-only world state.
- Mandatory Fabric networking packets.
- Custom shaders/rendering.
- Client-side camera manipulation.
- Java-only resource-pack techniques that cannot be translated by Geyser.

For each such feature, classify it as:

1. Can be replaced with a vanilla-compatible implementation.
2. Can become an optional Java enhancement with a fallback.
3. Fundamentally requires the Java client mod.

---

## Geyser and Floodgate Target

Future compatibility would likely use:

- Fabric Java server.
- BirdieBiome server mod.
- Geyser for Bedrock-to-Java protocol translation.
- Floodgate if Bedrock-only authentication is desired.

Conceptually:

```text
Java Edition Client
        |
        | Java protocol
        v
+-----------------------------+
| Fabric Java Server          |
|                             |
| BirdieBiome Server Logic    |
| Geyser                      |
| Floodgate (optional)        |
+-----------------------------+
        ^
        |
        | Bedrock protocol
        |
Bedrock Clients
Windows / Xbox / PlayStation
Switch / iOS / Android
```

Console connectivity may require additional practical setup because consoles generally do not expose arbitrary custom-server entry as freely as mobile/Windows Bedrock clients. That deployment issue is separate from BirdieBiome's architecture and does not need to be solved now.

---

## Hydraulic

Geyser also has an experimental project called **Hydraulic** intended to improve compatibility between Bedrock clients and modded Java servers.

Hydraulic should not currently be treated as a required dependency or architectural foundation.

Instead:

- Architect BirdieBiome so normal Geyser compatibility is as achievable as practical.
- Consider Hydraulic a possible future compatibility layer.
- Do not rely on Hydraulic to rescue core features that unnecessarily require Java client mods.

---

## Development Strategy

This architecture should not significantly slow down the MVP.

A reasonable approach is:

### Phase 1 — Keep the Core Server-Authoritative

Ensure the following are server-owned from the beginning:

- Golf physics.
- Course state.
- Ball state.
- Match state.
- Scoring.
- Rules.

### Phase 2 — Use Vanilla-Compatible Interactions Where Reasonable

Prefer normal Minecraft actions for:

- Club selection.
- Shot initiation.
- Aiming.
- Power selection.

Do not compromise gameplay severely merely to avoid a client feature.

### Phase 3 — Introduce Optional Java Enhancements

Add client-side polish where it provides clear value:

- Camera.
- HUD.
- Swing meter.
- Visual trajectory information.

Maintain a functional fallback experience.

### Phase 4 — Prototype Geyser Compatibility

Once the basic golf loop works, test an actual Bedrock client through Geyser.

The purpose of this prototype should be architectural validation, not production-quality Bedrock support.

Test at minimum:

1. Join server.
2. Join a golf match.
3. Select a club.
4. Aim.
5. Hit the ball.
6. Observe ball movement.
7. Complete a hole.
8. View score.
9. Advance to next hole.

Any failure should reveal where the Java client dependency remains.

---

## Evaluation Requested From the Project Agent

Please inspect the current BirdieBiome codebase, architecture documents, PRD, milestone plan, and existing implementation and evaluate the practicality of adopting this design.

Do **not** immediately refactor the project.

First produce an architectural assessment answering the following.

### 1. Current Client Dependencies

Identify every current or planned feature that requires a Fabric client mod.

For each one, explain why.

### 2. Server-Only Feasibility

Determine whether the core gameplay loop could function with:

- Fabric server mod only.
- Unmodified Java client.

If not, identify the minimum client-side requirements.

### 3. Ball Representation

Evaluate the current golf-ball design and determine whether it can be represented using a vanilla-compatible entity while maintaining acceptable physics and visuals.

### 4. Shot Input

Evaluate whether aiming, club selection, and shot power can use vanilla Minecraft interactions without custom client networking.

Recommend the most practical approach.

### 5. Camera

Evaluate whether the planned follow camera can:

- Work server-side with vanilla mechanics.
- Be approximated for vanilla clients.
- Become an optional Java enhancement.

Recommend the best tradeoff.

### 6. HUD

Determine which planned HUD elements could use:

- Action bar.
- Scoreboard.
- Boss bar.
- Titles.
- Inventory UI.

Identify which features would still benefit from a custom Java HUD.

### 7. Custom Content

Inventory any planned custom:

- Entities.
- Items.
- Blocks.
- Models.
- Screens.
- Keybindings.
- Packets.

Classify each according to its impact on vanilla Java and Geyser compatibility.

### 8. Geyser Risk Assessment

Identify the likely blockers to Bedrock clients joining and completing an entire BirdieBiome round through Geyser.

### 9. Architectural Changes

Recommend the smallest architectural changes that would preserve future Bedrock compatibility without significantly complicating the MVP.

### 10. Recommendation

Provide one of the following conclusions:

- **Highly practical** — architecture can become server-authoritative/client-light with modest changes.
- **Practical with tradeoffs** — achievable, but certain Java-only enhancements will require fallbacks.
- **Technically possible but costly** — would significantly complicate the project.
- **Not practical with the current gameplay design** — required features fundamentally depend on a Java client mod.

Explain the reasoning and explicitly identify which features drive the conclusion.

---

## Important Constraint

Do not sacrifice the core golf experience solely for Bedrock compatibility.

The intended priority order is:

1. Good multiplayer golf gameplay.
2. Maintainable server architecture.
3. Java Edition experience.
4. Ability to support client-light/vanilla Java where practical.
5. Future Bedrock compatibility through Geyser.

The objective is to **preserve the option** of Bedrock compatibility, not to let cross-platform constraints dominate the MVP.

---

## Desired Outcome

Ideally, BirdieBiome evolves toward this model:

```text
                         BirdieBiome Core
                    Rules / Physics / Courses
                              |
                              v
                     Fabric Java Server
                              |
             +----------------+----------------+
             |                |                |
        Java vanilla     Java enhanced     Geyser
          client           client              |
                              |                v
                    optional BirdieBiome    Bedrock
                        client mod           client
```

All three client types should eventually be capable of participating in the same server-authoritative round.

The enhanced Java client may provide the best presentation, but it should not own the game.

