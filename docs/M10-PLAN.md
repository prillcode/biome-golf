# M10 - Client-Light / Bedrock Compatibility

**Status:** In progress, started 2026-09. S1 (shot input) is underway; the ball
representation slice is gated on the Geyser prototype evidence below. Do not
claim vanilla-Java or Bedrock acceptance until the prototype gate passes
(`docs/BEDROCK-COMPATIBILITY-ASSESSMENT.md`).

## Why

The mod currently requires a matching Fabric client. M10 preserves the *option* of
unmodified-Java and Geyser/Bedrock cross-play without sacrificing the modded Java
experience. The assessment found the core is already server-authoritative and
Minecraft-free; only the input/presentation seams need work.

## Invariants (must hold throughout)

- **Server authority.** Gameplay state is server-owned; clients send intent only.
- **No client leakage.** `src/main/java` must not import `net.minecraft.client`.
- **Core domain stays Minecraft-free.** `ball/`, `club/`, `golf/`, `round/`, `hole/`,
  `course/`, `config/`, `surface/` keep zero `net.minecraft` imports.
- **Optional enhancement, never required.** The modded client (three-click meter, follow
  camera, custom HUD) keeps working; a fallback path must exist without it.
- **No Mixins, no Hydraulic dependency, graceful under the Modrinth/Geyser lifecycle.**

## Slices

### M10.0 - Geyser prototype evidence gate (DONE: harness)

`dev-server/docker-compose.geyser.yml` + `scripts/dev-server-geyser-up.sh` run Geyser +
Floodgate next to the golf server on UDP 19132, auth pinned to Floodgate. Verified: Geyser
2.11.3-b1245 + Floodgate 2.2.6 boot alongside `minecraft_golf 0.6.0` on MC 26.2 and Geyser
listens on 19132.

**Result (CONFIRMED BLOCKER).** A Bedrock client via Geyser was disconnected during
configuration: *"This server requires Fabric Loader and Fabric API installed on your
client! The following registry entry namespaces may be related: minecraft_golf"*.
Verified source: Fabric API's `fabric-registry-sync-v0` 7.1.1 `RegistrySyncManager`
disconnects clients missing entries in a non-optional (vanilla) registry; there is no
server-side disable switch. **So no unmodded client can connect while the mod adds blocks,
items, or entity types.** Each slice below must therefore *remove* vanilla-registry content,
not merely re-skin it. S1 (shot input) does not depend on this and is already underway.

### M10.1 - Vanilla-compatible shot input

- **S1a (started).** Server-only shot path: `ShotService.attemptNearest(...)` +
  `/golf swing [power] [accuracy] [shotType]`, aiming along the player's look direction and
  striking the nearest resting ball; all validation still funnels through
  `ShotService.attempt`. This is the guaranteed floor for any client.
- **S1b.** Held-use power model over vanilla `use` start/stop (power from hold ticks), so
  Bedrock players get a live shot without a command. Keep the three-click meter as the
  optional enhancement. This is a **gameplay change** and needs design + playtest.
- Command fallback stays for accessibility and as the test seam.

### M10.2 - Remove all custom vanilla-registry content (expanded by M10.0)

**Mandatory connection gate.** Fabric registry sync means any custom `minecraft:block`,
`minecraft:item`, or `minecraft:entity_type` entry blocks every unmodded client. This slice
must therefore alias **all** of it to vanilla:

- **Ball entity** (1): the logical ball (physics/owner/resting, already Minecraft-free)
  stays server state; the in-world entity becomes a swappable visual adapter renderable by
  vanilla/Bedrock (e.g. a vanilla display/item entity). `GolfBallEntity` is referenced
  widely (`ActiveHoleService`, `ShotService`, commands, client swing/camera/renderer), so
  do this behind tests.
- **Club items** (7): vanilla item + custom model data / resource pack instead of custom
  `Item` entries; the held stack still maps to a logical club server-side.
- **Cup/flag blocks** (3): vanilla blocks plus server-side authored markers instead of
  custom `Block` entries (cup detection is already position-based).

Acceptance for this slice: an unmodded client reaches the world without a registry-sync
disconnect — testable with the Geyser harness immediately, no full round required.

### M10.3 - Vanilla presentation fallbacks

Mirror the existing payloads into action bar / boss bar / chat / scoreboard so a client
without the mod sees hole/par/strokes/distance and the final scorecard (which currently
drops with no fallback). Custom HUD screens stay optional.

### M10.4 - Optional-enhancement boundary + verification

Add an architecture test enforcing the no-`net.minecraft` rule for the core packages, then
run the cross-client matrix (modded Java + Bedrock completing the same round).

## Non-goals

Console Bedrock deployment; Mixins; Hydraulic; marketplace/matchmaking; splitting Gradle
modules.

## Verification ladder

`./gradlew test` -> `./gradlew build` -> Loom client/server -> Docker (`dev-server-sync.sh`
+ restart) -> Geyser/Bedrock round. Vanilla-Java/Geyser acceptance requires the M10.0
prototype evidence; server-only slices are proven with unit tests plus Docker registration
logs.
