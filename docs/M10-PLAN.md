# M10 - Client-Light / Bedrock Compatibility

**Status: SHELVED (re-shelved 2026-09).** The full client-light/Bedrock program is not a goal;
the modded Fabric Java client remains the only fully supported experience. The one bounded
slice, **M10.3 Tier 1**, was implemented (ball-mirror gate, vanilla presentation fallbacks,
removal of the server camera), and a follow-up G1 tap-meter input fix was built. A Bedrock
playtest then showed the blockers are input feel and ball-flight smoothness rather than
visuals, and a strategic review chose to invest in the **Java experience** instead. Client-light
connections are now a promotional **visitor mode** (watch + "join on Java" invite) rather than a
play mode; the play code stays in the tree, gated on `!canSend(HoleStatePayload)` and dormant.
Visual parity (Tier 2) and a native Bedrock Add-On remain evaluated and deferred, not planned.
See `docs/BEDROCK-VISITOR-MODE-PLAN.md` and `docs/M10.3-CLOSEOUT.md`.

## Why this was shelved

The work reached a playable-but-rough Bedrock baseline and stopped there. The remaining gap
is not a bug to fix but a quality ceiling: with no client mod, the follow camera is a
server-driven spectate of a 20 Hz mirrored entity (subjectively shaky), there is no visible
power meter unless M10.3 is built, and the interaction cannot show the three-click swing.
Closing that gap means M10.3 plus camera smoothing — investment that competes directly with
Java polish for no gameplay gain on the supported client. Per the project priority order
(`good golf > maintainable server > Java experience > client-light > Bedrock`), we stop.

## What stays in the tree (gated, unsupported)

These are kept because they cost the modded Java client nothing — every path is gated on the
player **not** being able to receive `HoleStatePayload`:

- **Connection fix.** The touched vanilla registries are marked `RegistryAttribute.OPTIONAL`
  (`MinecraftGolf.markClientRegistryContentOptional`), so unmodified clients can connect.
- **Dual clubs.** Modded clients get the custom items; other clients get vanilla items with a
  display name and `custom_data` tag. `GolfItems.clubOf` recognises both.
- **Ball mirror.** The authoritative ball mirrors itself with a vanilla item entity so
  non-mod clients can see it.
- **Held-use input (M10.1 S1b).** `server/HeldShotService` + `club/HeldShotRules` time a
  right-click hold and launch through the unchanged `ShotService`. `/golf swing` remains.
- **Server ball camera (M10.2).** `server/BallCameraService` follows the ball's vanilla
  mirror via `ServerPlayer#setCamera` and restores on rest/removal.

These are explicitly **unsupported**: no acceptance is claimed, the Geyser Compose overlay
(`dev-server/docker-compose.geyser.yml`) is an optional experiment, and bugs in the
client-light path are not a reason to change the Java client.

## What was reverted

The vanilla cup/flag swap (cauldron + red banner) was reverted: it was the one change that
made the **modded Java** visuals worse. The M4 custom cup/flag blocks and their placement
(`block/GolfBlocks`, `GolfCupBlock`, `GolfFlagBlock`) are back, so Java keeps its models.
This is the only Java-facing change from the M10 work and is now undone.

## What is not being built

- **M10.4 cross-client verification matrix** and the no-`net.minecraft` architecture test —
  not planned.
- **Tier 2 (Bedrock resource pack / visual parity)** — deferred to a separate research
  session; see the Tier 2 section of `docs/M10.3-PLAN.md`.

## Reactivated slice

**M10.3 Tier 1 (planned)** keeps the two-client side-by-side goal but drops camera parity
and Bedrock visual parity. It is scoped entirely inside the `!canSend(HoleStatePayload)`
gate so the Java experience does not change. Full details, slices, verification, and exit
criteria are in `docs/M10.3-PLAN.md`.

## Historical evidence (kept for the record)

The M10.0 Geyser prototype confirmed that Fabric API's `fabric-registry-sync-v0` rejected
any client missing entries in a non-optional vanilla registry; marking the touched
registries `OPTIONAL` fixed the connection. A real Bedrock client connected, saw the clubs
and a snowball ball, and Geyser showed `EntitySpectateHelper`-based camera translation. The
remaining feel problems above are why the effort was shelved rather than finished.

## Non-goals

Console Bedrock deployment; Mixins; Hydraulic; marketplace/matchmaking; splitting Gradle
modules.
