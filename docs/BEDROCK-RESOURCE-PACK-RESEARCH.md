# Bedrock Resource Pack Research

**Date:** 2026-09-23
**Scope:** Research only; no pack, mapping, server, or Java-client changes were made.
**Target:** Geyser 2.11.3-b1245 / Floodgate 2.2.6, Minecraft 26.2.

## Conclusion

Tier 2 is technically feasible, but it is not a resource-pack-only change and is not
recommended for the current milestone. A Bedrock pack can supply the cup, flag, and club
visuals when Geyser is given explicit custom-content mappings. The Java resource pack is not
passed through or converted automatically. The ball is the difficult exception: Geyser's
2.11.x experimental custom-entity API provides a possible extension-based route, but it is
not a JSON/mapping-only feature and would require a Geyser extension that translates the
existing Java ball spawn to a Bedrock custom entity.

The practical recommendation is **do not plan Tier 2 yet**. Reconsider it only as a separate,
Geyser-version-pinned integration project if visual parity becomes more valuable than the
maintenance cost of a second asset pipeline and a Geyser extension.

## Findings

### 1. Pack serving, UUIDs, and Java-pack passthrough

- Geyser serves Bedrock resource packs from its `packs` directory. On Fabric this is under
  `config/Geyser-Fabric/packs/`; the current Docker overlay should therefore mount or copy
  the generated pack into the container's Geyser config data, not into
  `src/client/resources`.
- A `.zip` or `.mcpack` is accepted. A pack must contain a valid Bedrock `manifest.json`.
  The manifest has a pack UUID and a different module UUID; increment the pack/module
  version for updates. UUIDs are pack identity, not a Minecraft Golf namespace or Java
  resource-pack hash.
- Packs in the directory are sent to connecting Bedrock sessions after restart/reload.
  The Geyser API also supports local/remote and per-session pack selection, but that would
  require a Geyser extension. Remote packs must be a direct download with exact
  `Content-Length` and `Content-Type: application/zip`.
- Geyser does **not** convert, interpret, or pass through a Java Edition resource pack as a
  Bedrock pack. `src/client/resources/assets/minecraft_golf` is therefore an input/reference
  for a manually authored or converted Bedrock pack only.
- “RPACK” is not a separate Geyser deployment format identified by the current docs. The
  deliverable is a Bedrock resource-pack archive (`.zip`/`.mcpack`) plus Geyser
  `custom_mappings/*.json`. Rainbow's output names the archive `pack.zip`.

Geyser's built-in integrated pack remains separate. A Golf pack should not overwrite it;
resource-pack priority and any overlapping entity definitions would need testing.

### 2. Custom blocks

Geyser 2.11.x supports custom blocks when `gameplay.enable-custom-content: true` (the
default needed for custom mappings). JSON mapping files go in Geyser's
`custom_mappings` directory. A mapping supplies a Bedrock block name, geometry identifier,
material instances/textures, collision/selection boxes, and optional state overrides. The
mapping key identifies the Java block/state being overridden; Geyser supports modded Java
blocks as well as vanilla block-state overrides.

The three Golf blocks are therefore a good Tier 2 candidate:

| Java block | Bedrock mapping work | Assessment |
|---|---|---|
| `minecraft_golf:golf_cup` | map to a custom Bedrock block and cup geometry/material | feasible |
| `minecraft_golf:golf_flag` | map to a custom Bedrock block and pole geometry/material | feasible |
| `minecraft_golf:golf_flag_top` | map to a custom Bedrock block and pole/flag geometry/material | feasible, likely requires careful state/placement mapping |

The existing Java models are not Bedrock geometry. They can guide the dimensions and colors,
but the Bedrock pack needs Bedrock geometry/material JSON and textures. Block components are
explicitly less stable across Bedrock updates than item mappings, so this part would need
versioned compatibility testing.

There is an important distinction: custom-block mappings affect the Bedrock representation
of the Java block sent by Geyser; they do not change the server's authoritative block or cup
detection. This is compatible with the existing `!ServerPlayNetworking.canSend(player,
HoleStatePayload.TYPE)` policy because Java clients continue to receive the existing blocks
and Java models. Any future server-side alternate placement/visual path must remain behind
that non-modded-client gate.

### 3. Custom items / seven clubs

Geyser custom item mappings v2 can map a Java item/model combination to a Bedrock custom item.
They require a Bedrock resource pack, a unique non-`minecraft` `bedrock_identifier`, and
either the Java `item_model` definition or a legacy `custom_model_data` selector.

The current custom Java items are non-vanilla IDs (`minecraft_golf:club_*`). Geyser's JSON
mapping path is intended primarily for vanilla Java items with custom models. Geyser's
non-vanilla custom-item definition path is API/extension-based and currently permits one
definition per non-vanilla Java item, without the normal JSON predicate support.

The existing client-light fallback is seven different vanilla base items plus
`minecraft:custom_data` (`minecraft_golf_club=driver`, etc.). The documented Geyser v2
predicate set covers item model/custom-model-data, selected components, damage/count, and
similar properties; it does not document arbitrary `minecraft:custom_data` NBT matching.
Consequently, mapping the current fallback tags directly is not a supported JSON solution.

Two viable designs exist if Tier 2 is ever approved:

1. Add a Geyser extension/API registration for the seven non-vanilla Java club IDs, while
   retaining the existing Java-safe fallback for clients that do not use the extension.
2. Make the fallback stacks carry a Geyser-supported discriminator (most naturally a
   vanilla base item plus `item_model` or legacy custom-model-data), then provide seven
   v2 mappings and Bedrock attachable/item assets.

Option 1 is closer to the current modded Java content; option 2 needs a deliberately designed
server representation change. Neither should be implemented as part of this research.

Rainbow can inspect Java `item_model`/`custom_model_data`, generate Geyser item mappings, and
convert simple 2D or 3D item assets. It is useful as an authoring aid, not a substitute for
committing to a stable generated output and validating it against the pinned Geyser build.

### 4. Golf ball entity

The earlier conclusion remains correct for mapping-only approaches: the custom Java entity
cannot be made visible merely by adding a Bedrock pack. Geyser's normal entity translator
needs a known Java entity type, and the existing `minecraft_golf:golf_ball` is not a vanilla
entity that Bedrock can render.

Geyser API 2.11.0 introduced an **experimental** custom-entity API, which is present in the
2.11.x line and is the one possible new route for the pinned target:

- register a Bedrock custom entity definition from a Geyser extension;
- intercept the Java ball spawn for Geyser sessions and select that definition;
- provide Bedrock client entity JSON, Blockbench geometry, texture, and render-controller
  assets in the served pack.

This is not available through `custom_mappings` JSON. It also introduces a separate Java
Geyser extension artifact and an API marked experimental, rather than solving the problem in
the mod's resource pack. The ball's authoritative server movement can remain unchanged, but
spawn, position updates, despawn, tracking, scale, and camera interactions would all require
an integration test. The existing vanilla mirror remains the lower-risk supported path.

### 5. Geometry and authoring tools

Blockbench is a suitable authoring tool. It can export Bedrock geometry JSON and textures for
both block-style geometry and a small ball entity. The pack needs, at minimum:

- `manifest.json` with unique header/module UUIDs and an incremented version;
- `textures/` plus `item_texture.json` for item icons/textures;
- block geometry/material definitions used by Geyser mappings;
- entity geometry/client-entity/render-controller files only if the experimental ball route
  is selected;
- `pack_icon.png` (recommended for diagnostics and client UX).

Bedrock manifest v2 remains the conservative production choice for Geyser compatibility;
manifest v3 is documented in current Bedrock tooling but is newer/preview-oriented in the
tooling references and should not be adopted without testing the actual client fleet.

### 6. CI and packaging assessment

The clean boundary is a separate tree, for example `bedrock-pack/`, never a second copy of
`src/client/resources/assets/minecraft_golf`. A future packaging task could:

1. validate JSON, manifest UUID uniqueness, required texture/geometry paths, and archive
   layout;
2. copy or generate the Bedrock assets into a staging directory;
3. zip the directory with `manifest.json` at the archive root;
4. emit `minecraft-golf-bedrock-<pack-version>.zip` and a checksum;
5. publish/copy that archive and its Geyser mappings to the Geyser deployment.

This can be a small script/CI job and need not be part of Gradle or the Java JAR. The Java
build must continue to package only `src/main/resources` and `src/client/resources`; Bedrock
pack validation should be an independent optional job. Docker deployment would need an
explicit copy/mount into `/data/config/Geyser-Fabric/packs` and
`/data/config/Geyser-Fabric/custom_mappings`, followed by a Geyser/server restart.

No CI or pack files were added in this research session.

## Decision

**Tier 2 does not merit a plan now.** Blocks are feasible, clubs are feasible with a
representation or extension decision, and the ball is feasible only through a new experimental
Geyser extension. Together those are a second, version-sensitive content pipeline with no
benefit to the supported Java experience. Keep Tier 1's vanilla mirror and Java-safe gating.

If the decision is revisited, make it a new plan with these gates, in order:

1. prove a minimal cup mapping on the exact `2.11.3-b1245` Docker server;
2. prove one club mapping using the chosen discriminator;
3. decide whether the experimental entity extension is acceptable, otherwise retain the
   vanilla ball mirror;
4. add pack validation and deployment packaging only after those probes succeed.

## Sources consulted

- [Geyser: Using Resource Packs](https://geysermc.org/wiki/geyser/packs)
- [Geyser: Custom Blocks](https://geysermc.org/wiki/geyser/custom-blocks)
- [Geyser: Custom Items](https://geysermc.org/wiki/geyser/custom-items)
- [Geyser: Custom Entity API](https://geysermc.org/wiki/geyser/custom-entities)
- [Geyser: Rainbow converter](https://geysermc.org/wiki/other/rainbow)
- [Bedrock manifest reference](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/addonsreference/packmanifest?view=minecraft-bedrock-stable)
- [Bedrock Wiki: Project setup and RP manifest](https://wiki.bedrock.dev/guide/project-setup.html#rp-manifest)
- [Bedrock Wiki: Custom entity authoring](https://wiki.bedrock.dev/guide/custom-entity)
