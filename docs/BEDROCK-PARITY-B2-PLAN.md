# Bedrock Parity B2 — Custom Cup and Flags

**Status:** Future phase
**Depends on:** B0/B1

## Goal

Make `minecraft_golf:golf_cup`, `golf_flag`, and `golf_flag_top` visibly correct to Bedrock
clients through Geyser custom block mappings.

## Tasks

1. Create Bedrock geometry/material assets based on the existing Java dimensions and colors.
2. Create Geyser mappings for all three Java block IDs.
3. Set block geometry, textures, collision, selection, and render properties.
4. Verify flag-top orientation/state behavior and placement relationships.
5. Confirm `gameplay.enable-custom-content: true` in the pinned deployment.
6. Test blocks in a real hole through the Docker Geyser server.
7. Verify server cup detection and scoring are unchanged.
8. Confirm the Java path remains untouched and gated behavior is preserved.

## Acceptance criteria

- Bedrock sees the correct cup and complete flag silhouette.
- Java sees the existing custom cup/flag models with no visual change.
- Block state changes and orientation do not produce invisible or malformed parts.
- Hole completion and scoring match modded Java exactly.
- The result works on at least mobile/Windows Bedrock clients before console testing.

## Exit gate

Do not begin all-seven-club implementation until a cup mapping has succeeded on exact
Geyser `2.11.3-b1245`.
