# Bedrock Parity B3b — Club Implementation

**Status:** Future phase
**Depends on:** B3a decision

## Goal

Implement the selected mapping strategy for all seven clubs:

- driver
- fairway wood
- long iron
- mid iron
- short iron
- wedge
- putter

## Tasks

1. Author or convert all seven Bedrock item models, textures, icons, and attachables.
2. Generate or write stable Geyser mappings.
3. Implement the selected extension integration only if Option A was chosen.
4. Preserve the `!canSend(HoleStatePayload.TYPE)` client distinction.
5. Verify club selection, equip, held-use input, and shot validation.
6. Test first-person, third-person, inventory, and dropped-item presentation where applicable.
7. Add pack/mapping regression fixtures for all seven club identifiers.

## Acceptance criteria

- Each club is visually distinct on Bedrock.
- Each club still produces the correct server-side club definition and shot rules.
- No modded Java client receives Bedrock-specific item substitutions.
- Vanilla fallback clients still receive a usable, recognizable club representation.
- All seven mappings load without warnings or identifier collisions.
