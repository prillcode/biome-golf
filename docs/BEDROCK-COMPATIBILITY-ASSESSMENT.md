# Bedrock / Client-Light Compatibility Assessment

**Status:** Assessment complete. No code changed. This document answers the ten
questions in `docs/SERVER-AUTHORITATIVE-CLIENT-LIGHT.md` against the current
implementation (M0–M8.10, v0.6.0).

**Verdict: Practical with tradeoffs.**

The golf engine is already server-authoritative and, more importantly, already
Minecraft-independent at its core. Cross-play is blocked not by the simulation but by
four **input/presentation seams** that currently live in the client mod. Exactly one seam —
the shot input — fundamentally requires a Java client mod *as currently designed*, and it
is redesignable. The other three are replaceable with vanilla channels or can become
optional Java enhancements.

**Prerequisite for committing to any of this:** an actual Geyser + Bedrock prototype
(§"Prototype gate" below). Several claims here — especially whether unmodified clients can
connect at all given custom registry entries — cannot be settled by reading code.

---

## Executive summary

| Question | Answer |
|---|---|
| Is the core loop already server-authoritative? | **Yes.** Physics, scoring, rules, rounds, and course state are server-owned. |
| Is the core loop already client-independent? | **Yes at the domain layer, no at the seam.** `ball/`, `club/`, `golf/`, `round/`, `hole/`, `course/`, `config/`, `surface/` import zero Minecraft classes. |
| Can a server-only mod + unmodified client play today? | **No.** Minimum client requirement today is the whole mod (custom registry entries + shot input). |
| How many features fundamentally require the Java client mod? | **One:** the shot input protocol. |
| Is Bedrock compatibility practical? | **Practical with tradeoffs** — see §10. |

---

## 1. Current client dependencies

Everything the client mod owns today, with evidence and a Bedrock-impact classification.
Classifications use the proposal's scale: **(1)** replaceable with a vanilla-compatible
implementation, **(2)** optional Java enhancement with a fallback, **(3)** fundamentally
requires the Java client mod.

| # | Seam | Evidence | Why it is client-side | Class |
|---|---|---|---|---|
| 1 | Custom golf-ball entity | `entity/GolfBallEntities.java` (`minecraft_golf:golf_ball`), `client/render/GolfBallEntityRenderer.java` | Custom `EntityType` must be rendered by a matching client | 1 (alias to vanilla entity) |
| 2 | Custom blocks | `block/GolfBlocks.java` — `golf_cup`, `golf_flag`, `golf_flag_top` | Custom block states need client registration/models | 1 (vanilla blocks + server markers) |
| 3 | Custom club items | `item/GolfItems.java` — 7 `club_*` items | Custom items need client models/textures | 1 (vanilla item + custom model data) |
| 4 | Swing input | `client/swing/SwingController.java` → `net/ShotRequestPayload.java` → `net/ShotNetworking.java` → `ShotService.attempt` | Client times the three-click meter and mails final intent; **no server-side or vanilla-interaction path exists** | **3** (must be redesigned) |
| 5 | Follow camera | `client/camera/PostShotCamera.java`, registered on `ClientTickEvents` | Camera manipulation is inherently client-side | 2 (optional enhancement) |
| 6 | HUD | `client/hole/HoleHud.java`, `client/swing/SwingHud.java` | Custom HUD elements | 2 (action bar/boss bar fallback) |
| 7 | GUI screens | `GolfMenuScreen`, `CourseBrowserScreen`, `RoundScorecardScreen`, `NextHolePromptScreen` | Custom `Screen` subclasses | 2 (commands/chat already exist) |
| 8 | Keybindings | `client/input/PracticeKeybindings.java` — 5 bindings (`B` ball, `R` menu, `L` leave, `C` shot cycle, `Y` units) | Custom key mappings need the client mod | 2 (commands exist for all) |
| 9 | Custom networking | 4 clientbound + 6 serverbound payloads (below) | Semantic snapshots for the modded HUD | 2 (must add vanilla fallbacks) |
| 10 | Client assets | `src/client/resources/assets/minecraft_golf/**` — item models, textures, lang | Java resource pack | 1 (server resource pack / Bedrock pack) |

**Confirmed clean:** `src/main/java` contains **zero** `net.minecraft.client` imports, so
no client code leaks into common/server code. There are **no** custom sounds, particles,
or shaders — only a client `RenderType` selection and a vanilla `SoundEvents` cue.

**Clientbound payloads** (server → client, guarded by `ServerPlayNetworking.canSend`):
`HoleStatePayload` (`hole_state_v6`), `RoundScorecardPayload`, `CourseListPayload`,
`LobbyStatePayload`.
**Serverbound payloads** (client → server): `ShotRequestPayload` (`shot_request_v2`),
`GolfMenuActionPayload`, `RoundActionPayload`, `RoundLobbyActionPayload`,
`CourseListRequestPayload`, `NextHoleRequestPayload`.

Because the server guards every clientbound send with `canSend`, a non-mod client simply
receives nothing — it will not crash, but it also gets no HUD, scorecard, or course
browser. `RoundScorecardNetworking.send` logs a warning and drops the scorecard with **no
vanilla fallback**.

## 2. Server-only feasibility

**Server mod + unmodified Java client: not possible today.** The minimum client-side
requirement is currently the entire mod, because a player cannot take a shot without
`SwingController` and cannot see the ball without the custom renderer.

Split by layer:

- **Already server-only and client-independent:** `BallPhysics`, `BallState`,
  `ShotResolver`/`ShotService`, `ReadyGolfRound`/registry, scoring, penalties, hole
  lifecycle, authored-course store/JSON, protection guard, practice range, and all
  `/golf` commands.
- **Server-only but mapping-dependent:** the cup/flag blocks (detection is by block
  position on the server, so *function* survives even if the visual is wrong) and the
  ball entity (state and physics are server-side; only its *appearance* is client-side).
- **Client-only and required:** the swing input path, and (for a usable experience) the
  HUD, screens, camera, and ball renderer.

**Minimum client-side requirement to complete a round, if the architecture is unchanged:**
the mod, driven entirely by the shot input. If a vanilla-compatible shot input is added,
the remaining requirement drops to *presentation only*, which is optional by definition.

The M8.6 work already proved that the **match lifecycle** is command-accessible:
`/golf round create|list|join|start|leave|restart|status`, `/golf hole start|restart|status`,
`/golf clubs equip`, `/golf tapin`, `/golf nexthole`, `/golf practice ball`, `/golf pickup`.
So lobby, joining, scoring, and progression already have a vanilla channel; only the
**shot** and the **presentation** remain mod-only.

## 3. Ball representation

The ball is already the ideal shape for this migration:

- `GolfBallEntity.tick()` returns immediately on `level().isClientSide()`; all physics runs
  on the server via the pure-Java `BallPhysics`.
- Position is **tracker-driven** (the entity's synced position *is* the ball center); the
  client only renders it. There is no client-side physics to reconcile.
- `EntityType` is deliberately small (`0.5 × 0.5`, `MobCategory.MISC`) and tuned with
  `clientTrackingRange(16)` / `updateInterval(1)` for the follow camera.

**Options**

| Option | Tradeoff |
|---|---|
| **A. Alias to a vanilla entity type** (e.g. `item_display`/`interaction`/`armor_stand`/`snowball`) | Vanilla Java and (with care) Bedrock can see it. Loses the custom renderer; appearance moves to a resource pack. Must re-validate tracking range and update rate on the proxy type. |
| **B. Dual representation** — custom entity for modded clients, a vanilla proxy entity spawned for everyone | Preserves the best Java experience; adds spawn/despawn synchronization complexity and a "two entities, one ball" invariant to keep correct. |
| **C. Keep the custom entity** | Best Java visuals; blocks Bedrock and forces a required client mod. |

**Recommendation:** Option A for the client-light baseline, with Option C retained only
*if* a differential representation proves cheap later. The physics never depends on the
entity type, so this is a presentation swap, not an engine change. Do not attempt to make
the custom entity work through Geyser — that is the Hydraulic problem, not ours.

## 4. Shot input

This is the single hard blocker. The only path to `ShotService.attempt` is
`ShotRequestPayload`, produced exclusively by the client-side three-click meter. There is
no command and no vanilla-interaction fallback, so a Bedrock or vanilla client literally
cannot hit the ball.

| Option | Description | Cross-play |
|---|---|---|
| **A. Held-use power model** | Player stands near their own stationary ball and **holds** right-click; the server measures hold duration for power and reads look direction for aim; **release** executes. | Server sees ordinary `use` start/stop → Geyser-translatable. |
| **B. Command fallback** | `/golf swing <power>` (or `/golf hit`), optionally with an accuracy argument. | Always works through chat, but has no live meter and is clunky. |
| **C. Both** | A as the primary mechanic, B as a guaranteed floor for any client. | Best: playable everywhere, better with the mod. |

**Recommendation: C.** The proposal explicitly authorizes changing the input model if
Minecraft cannot provide reliable press/release timing through normal interactions. The
open technical question is whether Geyser translates the *duration* of a held use action
faithfully; that must be tested in the prototype. Note this is a **gameplay change** — the
three-click meter is a core feel (M3/M8.7) — so it needs its own design and playtest, and
the modded three-click meter should survive as an optional enhancement.

## 5. Camera

`PostShotCamera` runs on `ClientTickEvents` and manipulates the client camera; there is no
server-side equivalent. The proposal itself offers three options; the assessment's advice:

- **Option A (no custom camera):** accept normal first/third-person. Cheapest, fully
  compatible.
- **Option B (server-controlled spectator targeting):** risky — multiplayer isolation and
  reliable restoration of player state are hard to guarantee, and Geyser behavior is
  unverified. **Do not attempt for MVP.**
- **Option C (optional Java enhancement):** keep `PostShotCamera` behind the mod.

**Recommendation: Option C**, matching the proposal. The server may add a benign hint
(action bar `Ball is moving…`) as the vanilla fallback, but the follow camera is
Java-only by nature.

## 6. HUD

Every HUD element has a vanilla channel; the server already computes all of it.

| Information | Vanilla channel | Notes |
|---|---|---|
| Hole / par / strokes / score-to-par | Action bar or boss bar | `HoleStatePayload` already carries `holeNumber`, `par`, `strokes`, `strokeLimit`, score term |
| Distance to cup | Action bar | `distanceToCupBlocks` already present (M8.8 already converts units) |
| Current club | Held item name / action bar | Club is the held item |
| Shot power / accuracy | Boss bar or action bar | Requires the input redesign (adds a timed bar — a natural fit for a boss bar) |
| Lobby / round state | Chat (commands already) | `LobbyStatePayload` mirrors the command output |
| Final scorecard | Chat / scoreboard sidebar | `RoundScorecardPayload` needs a chat fallback (currently none) |
| Turn/hole ordering | Action bar / chat | Ready Golf has no forced turns |

**Recommendation:** add a thin `VanillaPresentation` emitter on the server that mirrors the
existing payloads into action bar / boss bar / chat / scoreboard, and keep `HoleHud` and
`SwingHud` as optional enhancements. No gameplay logic may move into the HUD.

## 7. Custom content inventory

| Kind | Count | Ids | Vanilla Java impact | Geyser/Bedrock impact | Class |
|---|---|---|---|---|---|
| Entities | 1 | `golf_ball` | Unknown entity renderer | Not renderable | 1 |
| Blocks | 3 | `golf_cup`, `golf_flag`, `golf_flag_top` | Unknown model (missing texture) | Fallback block or invisible | 1 |
| Items | 7 | `club_driver`, `club_fairway_wood`, `club_long_iron`, `club_mid_iron`, `club_putter`, `club_short_iron`, `club_wedge` | Unknown model | Fallback item | 1 |
| Clientbound payloads | 4 | hole state, scorecard, course list, lobby state | Ignored without mod | Ignored | 2 |
| Serverbound payloads | 6 | shot request, menu, round action, lobby action, course list request, next-hole | Cannot be sent without mod | Cannot be sent | 2 (shot = 3) |
| Screens | 4 | menu, course browser, scorecard, next-hole prompt | — | — | 2 |
| Keybindings | 5 | drop ball, menu, leave, shot cycle, units | — | — | 2 |
| Sounds / particles / shaders | 0 | — | — | — | clean |

## 8. Geyser risk assessment

Blockers to a Bedrock client joining and completing a full round, most severe first:

1. **Shot input (hard blocker, no fallback).** A Bedrock player cannot hit the ball. Fixes
   §4.
2. **Custom ball entity (hard blocker for aiming).** Even if the physics run server-side,
   the player cannot see the ball to aim at it. Fixes §3.
3. **Custom blocks/items (cosmetic, plus touch risk).** Cup/flag and clubs would appear as
   fallback content; cup *detection* still works server-side, but a Bedrock player cannot
   tell which item is a driver. Fixes §1 rows 2–3.
4. **Custom payloads (missing HUD/scorecard).** Bedrock receives no hole state or
   scorecard; the scorecard is currently dropped without a chat fallback. Fixes §6.
5. **Connection / registry sync (unverified).** The project currently *requires* a Fabric
   client (`ARCHITECTURE.md` §28.3). Whether an unmodified Java or Geyser client can
   connect to a server carrying extra entity/block/item registry entries is **not
   established by this assessment** and must be tested. If it fails, registry aliasing
   (§3, §9) becomes mandatory rather than optional.
6. **Console platforms.** Bedrock consoles restrict arbitrary server entry; explicitly out
   of scope (proposal agrees).

## 9. Architectural changes

The smallest set that preserves the *option* of Bedrock without complicating the MVP.
Each is additive and keeps the current modded client working.

1. **Platform-neutral shot-input adapter.** Introduce a server-side seam that accepts shot
   intent from (a) the existing payload and (b) vanilla interactions and/or a command, all
   funnelling into the unchanged `ShotService`. This is the gate — nothing else matters
   until it exists.
2. **Presentation-independent ball/visual layer.** Decouple the *logical* ball (already
   clean) from `GolfBallEntity` so the visual/entity type is a swappable adapter.
3. **Vanilla presentation emitter.** Action bar / boss bar / chat / scoreboard fallbacks
   alongside the existing payloads; keep the payloads for the optional enhancement.
4. **Interaction-adapter for items/blocks.** Treat clubs and cup/flag markers as logical
   concepts mapped onto vanilla items/blocks, with custom models as an optional pack.
5. **Document + test the dependency rule instead of splitting Gradle modules.** The
   proposal sketches `birdiebiome-core/server/client/compat` modules, but the current
   package layout already isolates the domain (`ball/club/golf/round/hole/course/config/
   surface` import zero Minecraft classes). Recommend an architecture test that *enforces*
   "no `net.minecraft` in the core packages" rather than a multi-module build split
   (cheaper, no build churn, same guarantee).

Do **not** use Mixins or depend on Hydraulic to force compatibility.

## 10. Recommendation

**Practical with tradeoffs.**

**Why not "highly practical":** the shot input is a genuine required-client seam with no
current fallback, and the ball/block/item registry entries carry an unverified connection
risk. Those are not cosmetic.

**Why not "costly/not practical":** the expensive parts are already done. The simulation is
server-authoritative, the domain layer is Minecraft-free, physics never touches the client,
and the match lifecycle already runs through commands. What remains is presentation and one
input redesign — bounded, testable work that leaves the good Java experience intact as an
optional enhancement.

**Drivers of the verdict:** the shot input, the ball entity, and the absence of vanilla
presentation fallbacks. **Enablers:** the pure core, server-authoritative physics, and the
command-first lifecycle delivered in M8.6.

### Proposed bounded slices (only after the prototype gate passes)

- **M10.0 — Geyser prototype spike.** Stand up Geyser (+ Floodgate) against the dev server
  and connect a real Bedrock client. Record exactly where it fails. Evidence, not features.
- **M10.1 — Vanilla interaction shot input + command fallback.** Reach `ShotService`
  without the client mod; keep the three-click meter as an optional enhancement.
- **M10.2 — Ball representation without a required custom entity.**
- **M10.3 — Vanilla presentation fallbacks** (HUD, scorecard, lobby).
- **M10.4 — Optional-enhancement boundary cleanup + cross-client verification matrix.**

Each slice must remain server-authoritative and must not regress the modded Java client.

### Prototype gate (do this before writing any of the above)

Per the proposal's Phase 4, the prototype should test: join → join match → select club →
aim → hit → observe ball → complete hole → view score → next hole. Every failure localizes
a remaining client dependency. Minimum two clients (modded Java + Bedrock) completing a
round is the acceptance bar.

---

## Constraints and non-goals

- Priority order (from the proposal) is preserved: good golf > maintainable server > Java
  experience > client-light/vanilla Java > future Bedrock.
- No course marketplace, matchmaking service, or external backend.
- No console (Xbox/PlayStation/Switch) Bedrock deployment work.
- No Mixin-based protocol tricks and no Hydraulic dependency.
- Do not sacrifice the three-click swing feel to win compatibility; make it optional.
- Vanilla Java and Geyser acceptance are **not** claimed anywhere until the prototype
  gate produces evidence.

## Evidence appendix

- Proposal: `docs/SERVER-AUTHORITATIVE-CLIENT-LIGHT.md`
- Client seams: `src/client/java/com/prillcode/minecraftgolf/client/**`
- Ball: `entity/GolfBallEntities.java`, `entity/GolfBallEntity.java`, `ball/BallPhysics.java`
- Shot path: `client/swing/SwingController.java`, `net/ShotRequestPayload.java`,
  `net/ShotNetworking.java`, `net/ShotService.java`
- Content registries: `item/GolfItems.java`, `block/GolfBlocks.java`
- Payloads: `net/**`; guards in `net/HoleStateNetworking.java`,
  `net/RoundScorecardNetworking.java`, `server/ActiveHoleService.java`
- Commands (vanilla-channel fallback): `command/GolfHoleCommands.java`,
  `command/GolfCourseCommands.java`
- Current client requirement policy: `docs/ARCHITECTURE.md` §8.1–8.2, §20, §21, §23, §28.3
- Prior boundary records: `docs/M8.6-PLAN.md` ("Client-light boundary record"),
  `docs/M8.7-CLOSEOUT.md` (Deferred)
