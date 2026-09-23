# Bedrock Parity B5 — Integrated Cross-Platform Validation

**Status:** Future phase
**Depends on:** B2, B3b, and the B4 decision

## Goal

Validate the complete Bedrock parity track in mixed-client gameplay.

## Client matrix

- Modded Java client.
- Bedrock mobile client.
- Bedrock Windows client where available.
- Console Bedrock client where practical.
- Vanilla Java client only if it remains an intended supported target.

## Test flow

1. Connect all clients through the supported server path.
2. Download and accept the Bedrock pack.
3. Join or create a round.
4. Equip each club.
5. Take held-use shots and command fallback shots.
6. Observe the ball during short and long flights.
7. Identify and complete holes using the cup/flag visuals.
8. Verify strokes, scoring, progression, and scorecard output.
9. Disconnect/reconnect and restart the server.
10. Update the pack version and verify clients receive the update.

## Regression checks

- Modded Java HUD, swing meter, camera, custom ball, cup, and flags are unchanged.
- Java clients do not see Bedrock mirrors or duplicate balls.
- Server-side physics and scores are identical for all clients.
- All Bedrock-only substitutions remain behind
  `!ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE)`.
- Pack and mapping failures do not prevent Java gameplay.

## Acceptance criteria

- Bedrock mobile/Windows clients complete a multi-hole round with recognizable clubs, cup,
  and flags.
- Console results are recorded separately, including any platform connection or pack-policy
  limitations.
- Mixed-client strokes and scores match server truth.
- Deployment, pack versioning, and rollback are documented.
- The final supported ball representation is explicitly documented: custom entity or vanilla
  mirror.
