# Bedrock Parity B0/B1 — Pack Foundation and Delivery

**Status:** Future phase
**Depends on:** M10.3 Tier 1

## Goal

Establish an independently versioned Bedrock resource-pack pipeline before creating Golf
content.

## Planned structure

```text
bedrock-pack/
  manifest.json
  pack_icon.png
  textures/
  models/
  attachables/
  render_controllers/
  entity/
  blocks/
  items/

geyser/
  custom_mappings/
```

The exact subdirectories may follow the final Blockbench/Geyser output, but the tree must not
duplicate or replace Java assets.

## Tasks

1. Define pack name, UUID ownership, semantic pack version, and supported Bedrock range.
2. Create a valid conservative Bedrock manifest with distinct header and module UUIDs.
3. Define archive layout with `manifest.json` at the root.
4. Add validation for JSON syntax, UUID uniqueness, required paths, and archive layout.
5. Produce a versioned `.zip` artifact and checksum.
6. Document copying/mounting into Geyser `packs/` and `custom_mappings/` directories.
7. Add an optional Docker deployment step that restarts/reloads Geyser safely.
8. Test pack delivery without modifying the Java resource layout or JAR contents.

## Acceptance criteria

- A test pack is offered to a Bedrock client by the exact Docker Geyser deployment.
- Version changes cause the client to accept the updated pack.
- Invalid manifests, duplicate UUIDs, and malformed mappings fail validation.
- Java build outputs remain unchanged.
- Deployment can be repeated from a clean checkout using documented commands.

## Risks

- Pack priority conflicts with GeyserIntegratedPack.
- Bedrock manifest format/version compatibility varies by client platform.
- Geyser config paths differ between development and production deployments.
