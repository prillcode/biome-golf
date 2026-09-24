# Changelog

All notable changes to Minecraft Golf are documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- **Bedrock/vanilla visitor mode.** Client-light connections (`!canSend(HoleStatePayload)` —
  Bedrock via Geyser or unmodified Java) are repurposed as promotional visitors instead of
  degraded players. On join a visitor is put into survival/peaceful, optionally teleported to a
  configured viewpoint, and sent a one-time welcome; `/golf spectator` watches a live round (and
  shows the "join on Java" promo) while `/golf spectator leave` returns to survival. All golf
  commands are blocked for visitors and `ShotService` rejects them as defence in depth. Operators
  configure the promo with `/golf visitor address|link|spawn|status`. The modded Java client is
  unaffected; see `docs/BEDROCK-VISITOR-MODE-PLAN.md`.
- **M10.3 Tier 1 Java-safe cross-play** (`9e0f7b5`): the vanilla ball mirror appears only while a
  client-light player is tracking the ball, the server-side ball camera was removed, and
  `ClientLightPresentation` / `HeldShotService` add vanilla action-bar, boss-bar, and chat
  fallbacks. No payload, `HoleStateNetworking`, or modded-HUD change; see `docs/M10.3-CLOSEOUT.md`.
- **G1 client-light tap meter** (`c2c376d`): the hold-based input was replaced after the playtest
  showed Geyser never delivered a release. It is dormant under visitor mode.
- M10 client-light/Bedrock **play** is **shelved**: the modded Java client remains the only
  supported target. The connection fix, dual clubs, ball mirror, held-use input, and server
  ball camera stay in the tree but are gated to clients that cannot receive
  `HoleStatePayload`, are not a supported path, and get no further parity work. The one
  Java-facing change (replacing the custom cup/flag blocks with a cauldron/banner) was reverted,
  so the modded client keeps its custom cup/flag models. See `docs/M10.3-CLOSEOUT.md`.
- M10.1 (experimental, unsupported) held-use shot input for client-light players: holding
  right-click near your own resting ball charges a shot (power from hold duration, capped at
  1 second) and release strikes along the look direction. Input funnels through the
  unchanged server-authoritative `ShotService`; the modded three-click meter and
  `/golf swing` both remain.
- M10.2 (experimental, unsupported) server-side ball-follow camera for client-light players:
  after a shot the server attaches the shooter's camera to the ball's vanilla-visible mirror
  through Java's `SetCamera` packet and restores it when the ball rests or is removed.
  Modded clients keep their own `PostShotCamera`.
- M10.1 vanilla-compatible shot entry point: `/golf swing [power] [accuracy] [shotType]`
  strikes the player's nearest resting ball along their look direction, so a client without
  the mod (including Bedrock via Geyser) can take a shot. Validation is unchanged — it all
  funnels through the server-authoritative shot service, and the modded three-click meter
  remains the optional Java experience.
- M10.0 Geyser/Floodgate Bedrock spike harness: a dev-server Compose overlay that adds the
  UDP 19132 listener and pins Floodgate auth (`scripts/dev-server-geyser-up.sh`).
- M10.0 connection fix: the block/item/entity registries the mod adds to are marked
  `RegistryAttribute.OPTIONAL`, so Fabric registry sync no longer rejects clients without
  the mod. Vanilla Java and Bedrock/Geyser clients can now connect.
- M10.2 club representation (dual): modded clients keep the custom club items; clients that
  cannot resolve them (Bedrock/Geyser, vanilla Java) receive distinct vanilla items with a
  club display name and an identifying `custom_data` tag. Both resolve to the same logical
  club, so shot validation is unchanged.
- M10.2 ball visual probe: the authoritative ball mirrors itself with a vanilla item entity
  (a dropped snowball) so clients without the mod can see it. Experimental; the custom
  renderer remains for modded clients.

## [0.6.0] - 2026-09-19

### Added

- Operator-authored whole-course landscape protection. `/golf course landscape bounds`
  captures a two-corner XZ perimeter (Y expanded to build height) on the player's current
  draft or the active selected course; `/golf course landscape lock|unlock` toggles the
  operator exemption, `/golf course landscape status` reports the perimeter, and
  `/golf course landscape clear` removes it. Landscapes work on both drafts and finalized
  courses without weakening hole-metadata immutability.
- Inside a perimeter, non-operators cannot break or place blocks, and TNT cannot be
  placed, ignited, or detonate. `landscape lock` extends that to every player, operators
  included; `unlock` restores operator repair.
- TNT is guarded without any Mixin through public Fabric events: `BlockEvents.USE_ITEM_ON`
  denies TNT placement and direct ignition inside a blast-margin-expanded perimeter, and
  `ServerEntityEvents.ALLOW_LOAD` cancels newly spawned primed TNT (covering redstone,
  dispenser, fire, and flaming-arrow ignition). TNT still works normally outside
  perimeters.

### Changed

- Authoring commands `clear`, `lock`, `unlock`, and `status` accept an optional explicit
  course id so console/RCON can always release a locked course.

## [0.5.1] - 2026-09-19

### Changed

- Widened the tap-in eligibility boundary from one block to two blocks (horizontal
  center-to-cup radius) after playtest feedback that one block felt too tight.

## [0.5.0] - 2026-09-19

### Added

- Operator-authored practice range: `/golf practice tee set` and `/golf practice tee`
  save and return to a persisted practice tee, and
  `/golf practice target set|clear|list <1-8>` place, remove, and list up to eight
  cup-and-flag targets.
- Practice-range configuration persists per world at
  `data/minecraft_golf_practice_range.json`; target numbers are bounded to 1–8 and
  cross-dimension teleport/marker operations are rejected.

## [0.4.0] - 2026-09-18

### Added

- Golf distance displays now default to yards using a 1.75 yards-per-block display
  conversion.
- Configurable `Y` keybind toggles Swing and Hole HUD distances between yards and blocks.
- Putter HUD distance is labeled as roll distance.
- The Hole HUD now shows cumulative horizontal distance traveled by the current shot while
  the ball is moving and after it comes to rest.

### Changed

- Gameplay, physics, course geometry, and authoritative network distances remain in blocks.

## [0.3.3] - 2026-09-18

### Fixed

- Reduced Stinger rolling retention so driver Stingers stop sooner after landing
  without changing their launch trajectory.

## [0.3.2] - 2026-09-18

### Added

- Server-authoritative Chip, Stinger, and Flop shot types with contextual `C` selection.
- Near-cup Tap-In action through `B`, `/golf tapin`, and the server prompt; Tap-In counts
  exactly one stroke.

### Changed

- Stinger shots retain their lower trajectory while using standard rolling friction for
  less post-landing rollout.
- Shot and hole-state payloads use versioned identifiers for the new contracts.

## [0.3.1] - 2026-09-17

### Added

- Multiple independent Ready Golf lobbies and rounds can coexist, including on
  the same course and hole, while unrelated golfers play solo rounds.
- `/golf round list` and the optional browser expose every open lobby with
  server-authoritative, UUID-targeted join actions.
- Active-hole scoring now tracks accepted shots independently from penalties and
  Pick Up totals.

### Changed

- Scorecards support variable authored hole numbers and course lengths.
- Finalized-course block protection now covers all authored courses and dimensions
  instead of relying on one globally selected course.
- Round, reconnect, transition, and result handling is scoped to each golfer's
  actual solo or shared round.

### Fixed

- Completed-solo reconnect no longer depends on a globally selected course.
- Current-hole HUD and action-bar output no longer present provisional score to par
  as a completed result.
- Disconnect, final-hole cleanup, and startup failures no longer leak or overwrite
  unrelated round state.

## [0.3.0] - 2026-09-15

### Changed

- `/golf round leave` is now the single player-facing exit action for lobbies,
  solo play, active Ready Golf, and completed rounds.
- Removed `/golf hole abandon` and `/golf round done` to eliminate overlapping
  lifecycle commands.

### Fixed

- Completed rounds can no longer be replaced while another golfer still owns
  them, preventing stale scorecards and trapped remaining players.
- Leaving and final-hole completion now clear stale client lobby state, and the
  last departing golfer retires the completed round.
- Disconnecting from a completed round no longer blocks remaining golfers from
  replaying.
- Solo round replay and current-hole replay now replace the authoritative hole
  session correctly and preserve completed earlier holes.
- Hole replay now uses the round-owned course and prepares replacement state
  before committing the restart.

## [0.2.9] - 2026-09-13

### Added

- Practice Mode course browser for finalized authored courses.
- Server-authoritative Ready Golf lobby create, join, start, and leave UX.
- Lobby participant status and coordinator actions in the HUD.

## [0.2.8] - 2026-09-13

### Fixed

- The server no longer selects the retired M5 development course at startup.
- Players now receive a safe no-course-selected state until an operator selects
  an authored course.

## [0.2.7] - 2026-09-13

### Fixed

- Authored courses with more than three holes now finalize scorecards without
  disconnecting players or crashing the server.
- Solo hole starts no longer create hidden Ready Golf round state.
- Round and hole cleanup now handles stale, abandoned, and one-player state
  consistently.

## [0.2.6] - 2026-09-12

### Added

- `/golf round restart` to restart a completed shared round at Hole 1 for all
  remaining participants.
- `/golf round done` to leave a completed round and return the player to world spawn.
- Sand-specific lofted landing retention, shot-power reduction, and driver rejection.
- Authored holes may now be finalized without explicit playable bounds; those holes
  use an unbounded playable region.

### Changed

- `/golf hole restart` and `/golf hole abandon` remain scoped to active hole attempts.

## [0.2.4] - 2026-09-12

### Fixed

- Removed the legacy low flag cloth from the cup base so only the top block of
  the three-block marker displays a flag.

## [0.2.3] - 2026-09-12

### Added

- `/golf course clone <source-id> <new-id> [display name]` to create an independent
  draft from an existing draft or finalized course.
- Separate non-colliding middle and top flag blocks for a three-block cup marker
  with one flag cloth.

### Fixed

- Existing golf cups receive the complete stacked flag marker when cup metadata is
  recaptured or a hole starts.

## [0.2.2] - 2026-09-12

### Fixed

- Replaced the clipped out-of-bounds flag model with a real non-colliding upper
  flag block so cup markers render two blocks high.
- Ensured authored cups, hole transitions, and M5 preparation place the upper
  flag block consistently.
- Re-running cup authoring on an existing golf cup now also installs its upper
  flag block.

## [0.2.1] - 2026-09-11

### Fixed

- Extended golf-ball entity tracking and Docker server view/simulation distance
  so long shots remain visible while the player stays at the shot origin.
- Updated the Docker sync and runtime configuration documentation for the expanded
  distance settings.

## [0.2.0] - 2026-09-11

### Added

- Operator-authored courses with draft creation, editing, validation, finalization,
  deletion, listing, status inspection, and runtime selection.
- Per-world persistence for authored course and hole metadata.
- Position-based tee, cup, par, and axis-aligned bounds authoring commands.
- `/golf clubs equip` to reset the complete seven-club set into hotbar slots 1-7.

### Changed

- Authored holes support 1..N holes per course while preserving the fixed M5
  three-hole regression course.
- Cup visuals now include a two-block-tall flag stick and raised flag.
- Course selection and deletion are blocked during active play.

### Fixed

- Authored definitions fail safely when persisted data is malformed or when required
  hole metadata is missing or outside its playable boundary.
- Club reset preserves displaced non-club hotbar items when inventory space allows.

### Deferred

- `/golf clubs add` for future club types.
- Multiple tee boxes and per-player tee selection.
- Additional game modes and other M9 scope.

[Unreleased]: https://github.com/prillcode/minecraft-golf/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/prillcode/minecraft-golf/compare/v0.5.1...v0.6.0
[0.5.1]: https://github.com/prillcode/minecraft-golf/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/prillcode/minecraft-golf/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/prillcode/minecraft-golf/compare/v0.3.3...v0.4.0
[0.3.3]: https://github.com/prillcode/minecraft-golf/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/prillcode/minecraft-golf/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/prillcode/minecraft-golf/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/prillcode/minecraft-golf/compare/v0.2.9...v0.3.0
[0.2.4]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.4
[0.2.3]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.3
[0.2.2]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.2
[0.2.1]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.1
[0.2.0]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.0
