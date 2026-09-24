# Bedrock Native Add-On — Research & Decision Record

**Status:** Researched (2026-09). **Not recommended; deferred.** Documentation only — no pack,
script, or Java change was made.
**Target reference:** Minecraft Bedrock Edition 1.26.x, Script API v2 (Beta APIs), BDS/Realms.
**Related:** `docs/BEDROCK-GUEST-MODE-PLAN.md` (the chosen direction),
`docs/BEDROCK-RESOURCE-PACK-RESEARCH.md` (Geyser pack route),
`docs/BEDROCK-PARITY-TRACK-PLAN.md` (Geyser visual phases).

## The question

Should BirdieBiome ship a **native Bedrock Add-On** (resource pack + behavior pack + TypeScript
Script API) as its own experience, instead of supporting Bedrock through Geyser, so that
Xbox/PlayStation/Switch/mobile/Windows players get a first-class feel without a translation
layer?

This was prompted by an external write-up arguing that a native Add-On is "considerably more
viable" than the Geyser route. This document records the assessment and the decision so the
option is not silently re-litigated.

## What a native Add-On is

- **Resource pack** — textures, models, animations, sounds, UI assets.
- **Behavior pack** — entities, items, blocks, recipes, loot, spawn rules, and scripts.
- **Script API** — JavaScript/TypeScript (`@minecraft/server`, `@minecraft/server-ui`, ...)
  running events, systems, custom components, and custom commands.

Content travels with the world/Realm, so players do not install anything client-side. This is a
real UX advantage over Fabric for non-technical players.

## Architecture comparison

| Dimension | Java Fabric (current) | Geyser guest (chosen) | Native Bedrock Add-On |
|---|---|---|---|
| Shares a world with Java players | — | **Yes** | **No** |
| Bedrock ball feel | N/A | Poor (mirrored Java physics) | Likely good (engine physics) |
| Bedrock visuals | N/A | Needs a Geyser pack | Native pack |
| HUD richness | Custom HUD | Action bar / boss bar / chat | Action bar / title / forms (no custom HUD) |
| Implementation cost | baseline | low (implemented) | **very high (full rewrite)** |
| Ongoing maintenance | one codebase | one codebase | **two codebases** |
| Console/kid access | none | Workaround (BedrockConnect/Realms) | Realm/server; packs auto-delivered |
| Authority model | Server-authoritative Java | Server-authoritative Java | Script-driven Bedrock |

## Why it is not recommended now

1. **It is a second implementation, not a port.** None of the Java domain transfers:
   `BallPhysics`, `ShotResolver`, `HeldShotRules`, `HoleLifecycle`, `ActiveHoleService`, the
   scorecard, and the course services would all be rewritten in TypeScript. That means two code
   bases, two test suites, and two release cycles for a small project.
2. **No shared rounds.** A native Bedrock experience plays in a Bedrock world. Java and Bedrock
   players cannot be in the same round (without Geyser, which is the other track). This directly
   contradicts the recorded "Java flagship + Bedrock guest" product decision and the goal of
   side-by-side play.
3. **The Script API has hard performance budgets.** The script watchdog defaults to roughly
   **2 ms of slow-script time per tick**, a **100 ms single-tick spike**, a **3 s hang**, and a
   **250 MB memory cap** that shuts the world down. Porting custom 20 Hz rigid-body ball
   physics with world collision is precisely the workload that fights these limits.
4. **Beta APIs and version churn.** Scripting v2 requires the "Beta APIs" experiment, and the
   entity physics components (`minecraft:bounciness`, `air_drag_modifier`, `friction_modifier`,
   `uses_uniform_air_drag`) only appeared in Bedrock 1.26.20–1.26.30. A second asset/code
   pipeline would need continuous version migration.
5. **Platform/module limits.** Scripts run on local worlds, BDS, and Realms, but
   `server-admin`, `server-net`, `diagnostics`, and `graphics` are **not available on Realms**,
   and some require manual `permissions.json` on BDS. Consoles cannot easily import behavior
   packs locally, so console play implies a Realm or dedicated server.
6. **No custom HUD.** Bedrock scripting offers action bar/title/chat and form menus, not a
   custom-drawn HUD — roughly the same presentation ceiling as Tier 1, so native Bedrock is a
   delivery upgrade, not a HUD upgrade.
7. **It competes with the priority order.** `good golf > maintainable server > Java experience >
   client-light > Bedrock`: investing in a second product for the lowest-priority client, while
   the Java experience still has polishing headroom, is the worst-value option.

## The one genuine upside (kept on the record)

Bedrock simulates and networks entities natively. A ball modelled as a "throwable" entity,
launched with `applyImpulse` and tuned with the engine physics components, would be simulated
and interpolated by the Bedrock engine rather than mirrored through Geyser. That is the one
approach that could make Bedrock ball flight **smoother** than the Geyser mirror, which is the
root of the 2026-09 choppiness finding. The tradeoff is that it uses Bedrock physics, not the
S01 model, so shots would not be identical to Java.

## Decision

- **Do not build a native Bedrock Add-On now.** Keep Java as the flagship and Geyser as the
  guest path (`docs/BEDROCK-GUEST-MODE-PLAN.md`).
- **Continue the Geyser feel work** (G1 input fixed; G2 flight investigation), because it
  preserves shared rounds and is bounded.
- Revisit this document only if a trigger below fires. Treat any future attempt as a **new,
  separate track** with its own plan — never as a port of the Fabric mod.

## Triggers to revisit

1. Bedrock becomes the *primary* audience (for example, the people who actually play are mostly
   on consoles/mobile and shared rounds with Java players matter less).
2. Geyser feel cannot be made acceptable after G1/G2 (input and flight) are exhausted.
3. A proof of concept shows engine-physics ball feel is clearly better *and* the intended
   players would actually use it.
4. The project is willing to maintain two codebases indefinitely.

## If ever pursued — bounded proof of concept

Time-box a spike before any plan:

1. One throwable ball entity with `minecraft:physics` + the 1.26.x bounciness/drag/friction
   components; launch with `applyImpulse`; confirm smooth flight on a real device.
2. One custom command (power selection) and one action-bar HUD; confirm no watchdog warnings.
3. One block-based cup and a stroke counter; confirm server-authoritative-enough scoring.
4. Confirm pack delivery on the actual family devices (Realm vs BDS) before building content.

Only a clean POC justifies a plan; otherwise stop.

## Sources

- Script watchdog: <https://wiki.bedrock.dev/scripting/script-watchdog>
- Scripting v2 overview (Beta APIs): <https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/v2-overview>
- Script API module/platform availability: <https://jaylydev.github.io/scriptapi-docs/latest/index.html>
- Entity `applyImpulse` / movement: <https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/entity>
- Engine physics components (1.26.20/1.26.30): <https://github.com/MicrosoftDocs/minecraft-creator/blob/main/creator/Casual/TossLab.md>
- Dedicated-server scripting and permissions: <https://learn.microsoft.com/en-us/minecraft/creator/documents/bedrockserver/scripting>
- Custom commands: <https://wiki.bedrock.dev/scripting/custom-commands>
