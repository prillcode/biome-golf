# Bedrock Parity Track

**Status:** Shelved (2026-09) — not planned. Kept as a reference for the Geyser visual phases.
**Prerequisite:** Complete M10.3 Tier 1 first
**Target:** Geyser 2.11.3-b1245 / Floodgate 2.2.6 / Minecraft 26.2

> **Amended 2026-09 by `docs/BEDROCK-GUEST-MODE-PLAN.md`.** The product decision is now
> "Java flagship + Bedrock guest", and a playtest showed the blockers are input feel and ball
> flight, not visuals. Feel work (G1/G2) now precedes the visual phases below; B4 (ball entity)
> is gated on a positive G2 smoothness signal. The phase documents here remain the source for
> the B0–B3/B5 visual work. A native Bedrock Add-On — a separate product with no shared Java
> rounds — was also evaluated and deferred; see `docs/BEDROCK-NATIVE-ADDON-RESEARCH.md`.

## Objective

Provide as much visual parity as practical for Bedrock mobile, Windows, and console players
without regressing the supported modded Java experience or moving gameplay authority to
clients.

This track is intentionally incremental. Cup/flag visuals and club visuals deliver value even
if the golf ball remains represented by the existing vanilla mirror. The experimental custom
ball entity is a later optional phase, not a prerequisite for the track.

## Non-negotiable constraints

- M10.3 Tier 1 is completed first.
- Java remains the supported experience.
- Server authority remains unchanged for physics, scoring, hole detection, and shot results.
- Bedrock-only server representation/presentation remains gated on
  `!ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE)`.
- The Bedrock asset tree is separate from `src/main/resources` and
  `src/client/resources`.
- No Java client regression is accepted to improve Bedrock visuals.
- Geyser and Bedrock versions are pinned and tested explicitly.
- The ball parity experiment must not remove the vanilla mirror fallback until proven stable.
- Console availability is tested where hardware/access permits, but console-specific
  deployment limitations remain an explicit risk.

## Work breakdown

| Phase | Name | Outcome | Dependency |
|---|---|---|---|
| B0/B1 | Pack foundation and delivery | Separate pack tree, manifest, validation, packaging, Docker delivery | M10.3 |
| B2 | Custom cup and flags | Bedrock sees the three Golf blocks correctly | B0/B1 |
| B3a | Club mapping spike | Choose extension mapping or supported fallback discriminator | B0/B1 |
| B3b | Club implementation | All seven clubs have distinct Bedrock visuals | B3a |
| B4 | Ball parity investigation | Decide whether experimental custom entity work is justified | B2, B3b |
| B5 | Integrated parity validation | Verify mixed Java/Bedrock play across target platforms | B2, B3b, B4 decision |

## Recommended execution order

1. Finish and verify M10.3 Tier 1.
2. Execute B0/B1 before authoring production assets.
3. Implement B2 as the first user-visible parity milestone.
4. Run B3a with one club using both viable approaches if practical.
5. Implement the selected B3b approach for all seven clubs.
6. Run B4 as a bounded investigation; retain the vanilla ball mirror if the cost or risk is
   disproportionate.
7. Execute B5 against the final supported representation.

## Overall exit criteria

- Bedrock clients receive a versioned Golf resource pack reliably.
- Bedrock clients see correct cup/flag visuals and distinct club visuals.
- Server scoring and hole detection remain identical across Java and Bedrock clients.
- Modded Java visuals, HUD, camera, blocks, items, and ball remain unchanged.
- Pack validation and deployment are reproducible without changing the Java JAR layout.
- The ball has either a verified custom-entity implementation or an explicit decision to keep
  the vanilla mirror as the supported Bedrock representation.
- Mixed-client verification is documented for mobile/Windows and, where available, console.

## Phase documents

- `docs/BEDROCK-PARITY-B0-B1-PLAN.md`
- `docs/BEDROCK-PARITY-B2-PLAN.md`
- `docs/BEDROCK-PARITY-B3A-PLAN.md`
- `docs/BEDROCK-PARITY-B3B-PLAN.md`
- `docs/BEDROCK-PARITY-B4-PLAN.md`
- `docs/BEDROCK-PARITY-B5-PLAN.md`

## Out of scope

- Replacing the Java three-click swing with a Bedrock-specific mechanic.
- Marketplace publishing or console matchmaking infrastructure.
- Automatic conversion of Java resource packs at runtime.
- A Gradle multi-module split solely for Bedrock assets.
- Removing Tier 1 fallbacks before parity alternatives are verified.
