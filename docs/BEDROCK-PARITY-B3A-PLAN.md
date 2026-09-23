# Bedrock Parity B3a — Club Mapping Spike

**Status:** Future phase
**Depends on:** B0/B1

## Goal

Choose the lowest-maintenance way to render the seven clubs while preserving the current
server-authoritative fallback behavior.

## Candidate approaches

### Option A — Geyser extension registration

Register the existing `minecraft_golf:club_*` Java items through Geyser's custom-item API and
serve matching Bedrock item/attachable assets.

### Option B — Supported fallback discriminator

Give the existing Bedrock-facing vanilla fallback stacks a Geyser-supported discriminator,
such as `item_model` or legacy `custom_model_data`, then map those definitions in JSON.

## Tasks

1. Select one representative club, preferably the putter or driver.
2. Build the minimum Bedrock model/icon/texture assets.
3. Prototype Option A if the pinned API can be isolated cleanly.
4. Prototype Option B without changing modded Java stacks.
5. Test inventory, held-item rendering, first-person rendering, and use input.
6. Confirm `GolfItems.clubOf` still recognizes every representation.
7. Compare maintenance cost, artifact count, compatibility, and Geyser upgrade risk.
8. Record the decision before mapping all seven clubs.

## Decision criteria

- Works on exact Geyser `2.11.3-b1245`.
- Does not require arbitrary `minecraft:custom_data` matching.
- Preserves Java custom items.
- Preserves Tier 1 vanilla fallback behavior.
- Minimizes long-term Geyser-specific code.
- Can be packaged and tested independently of the Java JAR.

## Acceptance criteria

One club renders distinctly and remains usable on Bedrock, while modded Java and fallback
Java behavior remain unchanged. A short decision record selects Option A or B for B3b.
