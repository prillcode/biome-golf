Minecraft Golf M6 -- Multiplayer Ready Golf implementation

THE PROBLEM
M5 is complete and M6 must add 1–4 player Ready Golf to the proven three-hole course without weakening server authority or regressing the accepted single-player flow. The highest-risk seam is that ActiveHoleService currently keeps independent PlayerCourseState entries that can advance onto different holes; M6 needs one shared server-owned round and all-active-participants-terminal advancement barrier around the existing per-player sessions.

WHAT WE TRIED
- Completed and manually accepted M5 at implementation commit c60b028 -- 172 tests passed, Loom and Docker servers passed, Docker JAR identity matched, and course generation stabilized at zero changes.
- Added the bounded risk-first M6 plan at docs/M6-PLAN.md and committed all existing milestone docs to main at a43d3ff.
- Created feature branch feature/m6-multiplayer-ready-golf from main -- no gameplay implementation has started.
- Preserved the existing architecture: HoleLifecycle and balls are already keyed/owned by player UUID, HoleStatePayload is player-targeted, and Minecraft entity synchronization remains authoritative for ball motion.

WHAT NEEDS TO HAPPEN NEXT
- Read AGENTS.md, docs/PRD.md, docs/ARCHITECTURE.md, docs/MILESTONES.md, docs/M5-CLOSEOUT.md, and docs/M6-PLAN.md before changing code.
- Verify the branch is feature/m6-multiplayer-ready-golf, main baseline a43d3ff is an ancestor, and the tree is clean except for the pre-existing untracked .claude/ directory.
- Execute docs/M6-PLAN.md in order. Start only with S1: implement and test the Minecraft-free Ready Golf round/barrier domain before Fabric or ActiveHoleService integration.
- Prove two-player state isolation, terminal-only atomic advancement, final-hole completion, deterministic participant order, disconnect/reconnect policy, and invalid/duplicate transition safety with focused unit tests.
- Run ./gradlew test after S1 and stop on failure. Do not begin S2 until S1 contracts and tests pass clearly.
- Continue slice by slice only after each verification gate passes; use the full verification ladder and require user participation for subjective/two-client/four-player manual acceptance where needed.

CURRENT STATE
- Repository: /home/prill/dev/minecraft-golf
- Branch: feature/m6-multiplayer-ready-golf; base main commit: a43d3ff; M5 implementation merge: c60b028.
- Local main is ahead of origin/main and nothing has been pushed. Do not push, merge, create a PR, or take other GitHub-facing actions without explicit user approval.
- .claude/ is pre-existing untracked content -- do not modify, stage, or commit it.
- M6 contracts are locked in docs/M6-PLAN.md: one configured-course round, explicit /golf round lobby commands, solo /golf hole start compatibility, owner-scoped balls/HUD/scores/travel, no turns, barrier-gated Holes 1–2, automatic shared Hole 3 finalization, and bounded suspension/withdrawal reconnect semantics.
- M5 course remains minecraft_golf:m5_ocean_campus v11 on seed -1928790872702396508. Do not change its layout, physics, club tuning, or generation footprint in M6.
- Known deferred issues remain out of scope unless blocking acceptance: short Hole 3, woods rollout tuning, M8 chip shots, and the magenta/black first-person club rendering defect.
