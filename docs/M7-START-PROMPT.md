Minecraft Golf M7 -- MVP hardening and family playtest

THE PROBLEM
M6 is complete and M7 must determine whether the MVP is genuinely fun and stable enough to justify V1 development. M7 is a hardening/playtest milestone, not a feature milestone: it must resolve the two M6 S4 playtest findings (course destructibility via a block-break guard, and the advance-UX product decision), absorb the deferred M6 two-player manual matrix and optional four-player session, structure playtest capture so findings drive tuning and defect fixes, and keep every M6 Ready Golf contract intact.

WHAT WE TRIED
- Completed and merged M6 (Ready Golf) to main at merge commit 9af27ba; pushed main to origin at 4f31776. 187 tests passed; Loom, Docker, and JAR identity checks passed; M5 solo regression tests 1-3 passed after the stale-state fix 68447e7.
- Recorded M6 S4 two-player playtest findings in docs/MILESTONES.md (course destructibility, advance UX) and documented the M7 handoff in docs/M6-CLOSEOUT.md.
- Added the bounded risk-first plan at docs/M7-PLAN.md.
- Confirmed the M7 seams: no PlayerBlockBreakEvents handler exists yet; greens exist only as layout blocks (no authored green metadata); recovery is the idempotent `/golf dev preparecourse`; full auto-advance would deviate from the M6 player-initiated `/golf nexthole` contract.

WHAT NEEDS TO HAPPEN NEXT
- Read AGENTS.md, docs/PRD.md, docs/ARCHITECTURE.md, docs/MILESTONES.md, docs/M6-CLOSEOUT.md, and docs/M7-PLAN.md before changing code.
- Verify the branch is feature/m7-mvp-hardening, main baseline 4f31776 is an ancestor, and the tree is clean.
- Execute docs/M7-PLAN.md in order. Start with S1: the Minecraft-free protected-zone model plus the Fabric PlayerBlockBreakEvents guard, before any playtesting infrastructure. Prove zone membership/radius logic and the dev/operator exemption with focused unit tests.
- Run ./gradlew test after S1 and stop on failure. Do not begin S2 until S1 contracts and tests pass clearly.
- S2 requires a recorded product decision with the user (HUD prompt vs. full auto-advance vs. keep /golf nexthole) before implementation; full auto-advance is a deliberate deviation from the M6 contract and must be documented as such.
- Continue slice by slice only after each verification gate passes; use the full verification ladder. S3-S5 depend on real family/LAN sessions and require user participation for subjective acceptance. Stop and document any required deviation from docs/ARCHITECTURE.md.

CURRENT STATE
- Repository: /home/prill/dev/minecraft-golf
- Branch: feature/m7-mvp-hardening; base main commit: 4f31776; M6 merge commit: 9af27ba.
- Main and origin/main are in sync at 4f31776.
- M6 contracts are locked: server-owned round/score/ball/shot/penalty/completion state, no forced turns, independent scoring, barrier-driven Holes 1-2 advancement and automatic shared Hole 3 finalization, disconnect/reconnect suspension, owner-only travel, per-player HUD snapshots, and the preserved solo `/golf hole start` fast path.
- M5 course remains minecraft_golf:m5_ocean_campus v11 on seed -1928790872702396508. Do not change its layout, physics baseline, or generation footprint except through the M7 plan's deliberate tuning slice (S4).
- Known deferred issues remain out of scope unless blocking acceptance: wind, cinematic camera, landing-area indicator, chip shots, expanded clubs, sounds/particles polish, traditional turns, persistence/global statistics, matchmaking, and course authoring.
- .claude/ is gitignored local tool state -- do not stage or commit it.