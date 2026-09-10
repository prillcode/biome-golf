Minecraft Golf M6 S4 -- Concurrent presentation isolation and club sprite repair

THE PROBLEM
S1--S3 now provide the shared server-owned Ready Golf round, two-player ownership integration, coordinated barriers/transitions, and deterministic results. S4 must prove simultaneous shots, owner-only travel/HUD/camera behavior, and shared ball visibility while repairing the Minecraft 26.2 club item resource path and replacing the broken shared placeholder with seven distinct 2D sprites.

WHAT WE TRIED
- Committed S1 as fa4c99c, S2 as a858836, and S3 as 717e934 on feature/m6-multiplayer-ready-golf; S3 passed 186 tests and ./gradlew clean test build.
- Deployed S3 to the authenticated Fabric client and Docker server; all JARs matched SHA-256 396810858f485dca266667cc9d24a8edfafba1998615c4d40693368e21262532 and Docker reached healthy/Done.
- The user confirmed the S2 one-player course flow and is manually testing S3 while this handoff is prepared; collect their findings before treating manual S3 behavior as accepted.
- Confirmed the built JAR contains assets/minecraft_golf/models/item/club_*.json and textures/item/club.png but no modern assets/minecraft_golf/items/club_*.json definitions; clubs still render as magenta/black artifacts.
- Chose seven distinct Minecraft-style 2D club sprites for MVP. Vanilla tool models may be used only as a short resource-resolution diagnostic; custom 3D Blockbench models are deferred.

WHAT NEEDS TO HAPPEN NEXT
- Read AGENTS.md, docs/PRD.md, docs/ARCHITECTURE.md, docs/MILESTONES.md, docs/M5-CLOSEOUT.md, and docs/M6-PLAN.md; verify branch feature/m6-multiplayer-ready-golf contains S3 commit 717e934 and is clean except .claude/.
- Execute only S4 from docs/M6-PLAN.md. Do not implement S5 disconnect/reconnect hooks, alter the M5 course, retune physics/clubs, or add cosmetic ball systems.
- Inspect Minecraft 26.2's actual item-definition schema from the pinned game resources, add correct assets/minecraft_golf/items/club_*.json definitions, and first prove one vanilla/custom model resolves without checkerboards.
- Create seven readable 2D sprites for Driver, Fairway Wood, Long Iron, Mid Iron, Short Iron, Wedge, and Putter; preserve dedicated-server-safe resource separation and verify inventory, first-person, third-person, and dropped-item views.
- Prove adjacent-tick independent shots, owner-only automatic travel, player-targeted HUD/camera state, and normal entity visibility with two clients; run ./gradlew test and ./gradlew build, then stop before S5.

CURRENT STATE
- Repository: /home/prill/dev/minecraft-golf; implementation HEAD: 717e934; local main: a43d3ff; nothing has been pushed.
- Only .claude/ is pre-existing untracked content -- do not modify, stage, or commit it.
- Client profile is fabric-loader-26.2 with Loader 0.19.5 and Fabric API 0.159.0+26.2; client mod path is ~/.minecraft/mods/minecraft-golf-0.1.0.jar.
- Docker container minecraft-golf-dev is healthy on localhost:25565; staged mod is dev-server/mods/minecraft-golf.jar; PrLLager207 is operator level 4.
- Do not redeploy or restart Docker while the user is actively testing S3 without coordinating first; club checkerboards are expected in the deployed S3 artifact.
