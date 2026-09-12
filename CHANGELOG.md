# Changelog

All notable changes to Minecraft Golf are documented in this file.

This project follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

Changes made after the latest release will be recorded here.

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

[Unreleased]: https://github.com/prillcode/minecraft-golf/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/prillcode/minecraft-golf/releases/tag/v0.2.0
