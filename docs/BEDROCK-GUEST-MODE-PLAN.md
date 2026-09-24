# Bedrock Guest Mode — Plan

**Status:** Proposed (2026-09). Documentation only; no code started.
**Supersedes the priority ordering of:** `docs/BEDROCK-PARITY-TRACK-PLAN.md` (which stays the
source for the B0–B5 visual phases).
**Prerequisite:** M10.3 Tier 1 (implemented, commit `9e0f7b5`).
**Target:** Geyser 2.11.3-b1245 / Floodgate 2.2.6 / Minecraft 26.2.

## Evidence from the 2026-09-22 playtest logs

From the Docker dev-server log for the Floodgate player `.PFamGamez` (Windows mini-PC) and the
modded Java player (three-click meter, varied power):

1. **Held-use never observes release.** All 12 Bedrock held-use shots logged
   `power=1.0 after 200 ticks` — the `MAX_HOLD_TICKS` auto-fire cap. Geyser did not deliver a
   use/release pair for a club (an item with no use duration), so `isUsingItem()` stayed true
   and every shot maxed out after 10s. This is a fixable integration bug, not the accepted
   input asymmetry. `/golf swing <power>` remains the controllable floor and was not used.
2. **Server tick debt.** Earlier Bedrock sessions logged repeated
   `Can't keep up! Running 2.5–4.3s or 50–86 ticks behind`, which alone makes a 20 Hz mirror
   choppy for every client. G2 must check server health/TPS, not only mirror interpolation.
3. **Geyser translation errors.** `IndexOutOfBoundsException` on the Geyser player thread,
   `Downstream packet error!`, and `Could not translate packet ClientboundContainerSetContentPacket`
   recurred. Verify Geyser 2.11.3-b1245 against MC 26.2 before investing in packs.
4. **Custom-block chunk errors.** Chunk sections containing `minecraft_golf:golf_cup/flag/flag_top`
   logged `Unknown registry key ... using default` during the Bedrock session, near the same
   palette errors. Possible Geyser chunk-translation instability from unmapped custom blocks;
   independent of cosmetics and worth its own investigation.

## Product decision

Java is the **flagship** experience. Bedrock (mobile, Windows, consoles via Geyser) is a
**guest** mode: it should join, see the course, hit the ball with a controller/touch-friendly
input, and be scored correctly — but it is not promised parity with the modded Java client.

Rationale: a Windows-mini-PC Bedrock playtest showed the two blockers are **feel**, not
visuals. The charge always saturated at 100% with no usable control, and snowball flight was
choppy. A Bedrock resource pack fixes cup/flag/club/ball *looks*; it fixes neither blocker.
So feel must come first and visuals second. (Market context: Bedrock ≈ 1:3.7 the players of
Java and reaches consoles/mobile; a guest mode is worth bounded investment, but full parity is
not justified against the project priority order.)

## Guest quality bar

A Bedrock guest session is acceptable when, on mobile/Windows and (with the usual console
server-join workarounds) console:

- the player can join and be scored with the Java players in the same round;
- power is controllable with a controller/touch input, with discoverable feedback;
- ball flight is at least stable, even if not silky;
- the cup/flag are visible enough to aim at;
- the modded Java client is unchanged.

## Non-negotiables

- Java remains the supported experience; no Java regression is accepted.
- Server authority (physics, scoring, hole detection, shot results) is unchanged.
- Every Bedrock-only path stays gated on `!ServerPlayNetworking.canSend(player,
  HoleStatePayload.TYPE)`.
- The Bedrock asset tree is separate from `src/main/resources` and `src/client/resources`.
- Geyser/Bedrock versions are pinned and tested explicitly.
- The vanilla ball mirror stays the supported fallback until a custom entity is proven stable.

## Increments (feel first, then visuals, ball last)

| # | Name | Outcome | Cost |
|---|---|---|---|
| G1 | Input feel | A controller/touch-friendly power input that no longer saturates at 100% | S |
| G2 | Flight smoothness | Root-cause the choppiness; adopt, defer, or reject a smoother mirror | S–M |
| G3 | Visual pack (B0/B1, B2, B3a/B3b) | Geyser-pushed pack: correct cup/flag/club visuals on all Bedrock platforms | M–L |
| G4 | Ball visual (B4) | Only if G2 shows the experimental custom entity improves smoothness | L, risky |
| G5 | Integrated validation (B5) | Mixed Java/Bedrock round verified on mobile/Windows/console | M |

### G1 — Input feel (do first)

**Status: implemented (2026-09), pending Bedrock playtest.** The hold-based model was replaced
by a tap meter (`HeldShotRules` + `HeldShotService`): each use tap advances a power step shown
on the boss bar, and the shot fires after a 15-tick tap window or at the 4th tap. It no longer
depends on a release event, so the "always 100% after 200 ticks" failure cannot recur. Counted
taps and the auto-fire path are logged for the next playtest. Hardened against Geyser holding:
repeated use actions within 3 ticks are de-bounced, and a 20-tick-plus gap starts a fresh
meter.

The original hold-based design mapped a hold to power with a 1-second analog ramp and a
200-tick auto-fire cap; the playtest logs showed the auto-fire always won. The remaining tasks
above are kept for context.

1. **Diagnose** what Geyser actually sends for press/release/hold on the pinned build
   (instrument `HeldShotService`; compare against a vanilla Java client). Determine whether
   Bedrock produces a continuous `isUsingItem` hold or only discrete use events.
2. **Redesign power** for discrete input: either
   - a **tap/cycle meter** reuse of the three-click idea (each use tap advances power a step
     and auto-fires), which is controller- and touch-friendly and unifies the mental model; or
   - a **stepped hold** with a longer ramp and notched feedback (`BossBarOverlay.NOTCHED_10`)
     snapping to 10% steps.
   Keep `/golf swing <power>` as the precise floor.
3. **Discoverability**: a one-time chat hint on first charge, and an explicit power label.
4. Do **not** change the Java three-click meter.

### G2 — Flight smoothness

1. **Isolate the cause.** With the same server, compare flight on a **vanilla Java client**
   (same snowball mirror) against Bedrock. Smooth Java + choppy Bedrock ⇒ Geyser/network.
   Choppy on both ⇒ the 20 Hz mirror or server health.
2. **Check server health** during a shot (`Can't keep up` ticks, TPS) — the dev host is a
   Wi-Fi laptop and already logged a tick-behind spike.
3. If it is mirror interpolation, trial a vanilla entity that interpolates better (for example
   a real thrown `Snowball` projectile rather than a dropped `ItemEntity`).
4. Record a decision: adopt / defer / reject. Do not start G4 without a positive signal here.

### G3 — Visual pack

Execute the existing `docs/BEDROCK-PARITY-B0-B1-PLAN.md`, `...-B2-PLAN.md`, and
`...-B3A/B-PLAN.md`. Geyser serves packs server-side to every Bedrock platform, including
consoles, so this is the reachable half of parity and needs no client install.

### G4 — Ball visual

Only if G2 justifies it, execute `docs/BEDROCK-PARITY-B4-PLAN.md`. Otherwise keep the vanilla
mirror and document the limitation; the ball is the riskiest, most Geyser-version-coupled part.

## Decision gates

1. G1 must produce a controllable input before G3 is worth starting.
2. G2 must produce an adopt decision before G4 is worth starting.
3. G3 does not block G4, and vice versa, but both feed G5.

## Open questions

- Does Geyser 2.11.3-b1245 translate a notched boss bar and an action bar reliably on console?
- Can console players reach the server without a DNS/MCXboxBroadcast workaround? If not, is
  "console" actually in scope, or is the guest audience effectively mobile + Windows?
- Is the tap/cycle input intuitive on touch, or does it need an explicit on-screen affordance?

## Out of scope

- Bedrock visual parity as a goal in itself; marketplace/console matchmaking.
- Replacing the Java three-click meter; changing payload shapes; removing Tier 1 fallbacks.
- A Gradle module split for Bedrock assets.
