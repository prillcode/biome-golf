# Bedrock Parity B4 — Ball Parity Investigation

**Status:** Future phase
**Depends on:** B2 and B3b

## Goal

Determine whether the experimental Geyser custom-entity API provides enough value and
stability to replace the Bedrock vanilla ball mirror with a proper golf-ball visual.

## Baseline

Retain the vanilla mirror as the supported Bedrock representation throughout this phase. Do
not remove it merely because a custom entity can spawn once.

## Investigation tasks

1. Confirm the custom-entity API against exact Geyser `2.11.3-b1245`.
2. Register a minimal Bedrock golf-ball entity through a Geyser extension.
3. Intercept only the Golf ball spawn for Geyser sessions.
4. Provide Blockbench geometry, texture, client entity, and render-controller assets.
5. Test spawn, movement interpolation, tracking range, chunk changes, and despawn.
6. Test shot flights, rest state, pickup/removal, reconnects, and multiple players.
7. Test interactions with `SetCamera`/spectate behavior if camera fallback remains enabled.
8. Compare the result against the vanilla mirror for smoothness and failure recovery.

## Decision gate

Choose one:

- **Adopt:** custom entity becomes an optional Bedrock path with vanilla mirror fallback.
- **Defer:** retain the vanilla mirror and document the limitation.
- **Reject:** remove the experiment if it creates unacceptable Geyser maintenance or client
  isolation risk.

## Acceptance criteria for adoption

- Bedrock sees one ball with stable position updates during full shots.
- Java clients retain their current custom ball and renderer.
- Ball lifecycle remains server-authoritative.
- Failure to load the extension or pack falls back safely to the vanilla mirror.
- No camera or multiplayer isolation regression is introduced.
