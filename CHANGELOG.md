# Changelog

All notable changes to Minecraft Golf are documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

Changes made after the latest release will be recorded here.

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

[Unreleased]: https://github.com/prillcode/minecraft-golf/compare/v0.4.0...HEAD
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
