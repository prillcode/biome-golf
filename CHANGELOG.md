# Changelog

All notable changes to Minecraft Golf are documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

Changes made after the latest release will be recorded here.

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

[Unreleased]: https://github.com/prillcode/minecraft-golf/compare/v0.2.4...HEAD
[0.2.4]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.4
[0.2.3]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.3
[0.2.2]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.2
[0.2.1]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.1
[0.2.0]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.0
