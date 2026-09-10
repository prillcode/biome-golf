# M7 — MVP Hardening and Family Playtest: Bounded Plan

**Status:** In progress. S1/S2 implementation and automated verification complete;
manual acceptance pending. S3 capture artifact is ready at `docs/M7-PLAYTEST.md`.
Baseline: `main` at `4f31776` with 187 passing tests and M6 accepted.

M7 does not add new features. It hardens the proven M5 course + M6 Ready Golf MVP for repeated family/LAN play and produces the evidence needed to decide whether to proceed to V1. The milestone's exit is a judgment call, so the plan's job is to (a) resolve the two M6 S4 playtest findings with minimal, deliberate change, (b) absorb the deferred M6 manual-acceptance items, (c) structure playtest capture so findings drive tuning and defect fixes rather than anecdotes, and (d) keep every M6 contract intact.

## Purpose

Determine whether the MVP is genuinely fun and stable enough to justify V1 development. Repeated family/LAN sessions with 1–4 golfers, structured observation capture, deliberate resolution of the two known playtest findings, targeted tuning and defect fixing, and a documented V1 go/no-go decision.

## Existing seams and required change

- **No block-break protection exists.** The mod currently has no `PlayerBlockBreakEvents` handler, so in creative mode any golfer can break any block, including tee boxes, greens, and the cup/flag. Course metadata today is `HoleDefinition` (tee, cup, boundary, transition) plus the generated layout's `GREEN_WOOL` fills; greens exist only as blocks, not as authored metadata. Per the recorded S1 decision (see "Recorded M7 decisions"), the guard keys protection on tee/cup vicinity around the authored `HoleDefinition` positions rather than adding new green metadata; greens are protected through cup vicinity and tee boxes through tee vicinity.
- **`/golf dev preparecourse` is the recovery path.** It is idempotent and currently repairs drifted blocks. The block-break guard must not block the operator/dev repair path, or recovery breaks. It should also remain possible to clear in-the-way trees/rocks outside protected zones, preserving the fun Minecraft flavor.
- **Advance UX is a deliberate product decision.** The post-hole flow already has a clickable chat action plus `/golf nexthole`. Playtesters want either a no-text-input prompt (HUD button) or full auto-advance. Full auto-advance would deviate from the M6 player-initiated `/golf nexthole` contract, so the choice is explicitly an M7 product decision to be made with the user before implementation.
- **Deferred M6 manual acceptance carries over.** The two-player manual matrix (items 1–9 in `docs/M6-PLAN.md`) still needs a second golfer, and the true four-player LAN session was waived for M6. M7 family playtesting is the natural place to run these.
- **Tuning flags already recorded.** Hole 3 plays short for a par 5, and Driver/Fairway Wood may benefit from slightly more rollout than irons (both flagged in the M5 closeout). The club sprites were repaired in M6 S4; no first-person club-rendering defect is known to remain, but M7 playtesting should confirm.

## Locked M6 contracts (must not regress)

- The server owns round membership, connection/withdrawal status, current hole, every `PlayerCourseState`, ball assignment, strokes, penalties, completion, transitions, and scorecards.
- Ready Golf: no turn owner, no shot-order check; any non-terminal participant may shoot whenever their own assigned ball rests.
- Advancement is barrier-driven: Holes 1–2 transition only when every active participant is terminal; Hole 3 finalizes the shared round automatically when the last active golfer becomes terminal. Repeated/near-simultaneous requests stay idempotent.
- Disconnect suspends and removes from the barrier; timely reconnect resumes authoritative state; advancing without an offline golfer withdraws their stale state; all-disconnected rounds stay suspended.
- Automatic next-shot travel is owner-only and triggers only from that owner's naturally resting active ball.
- Each client receives only its own hole/HUD snapshot; each golfer's scoring is independent; all clients observe all loaded golf balls via normal entity synchronization.
- `/golf hole start` remains the backward-compatible one-player fast path with unchanged M5 start, progression, finalization, and replay.
- Common code under `src/main` contains no `net.minecraft.client` imports; dedicated-server compatibility is preserved.

## Recorded M7 decisions

Made with the user before implementation (2026-09-10). These are locked inputs to the slices below.

- **S1 protected-zone scope — tee/cup vicinity only.** The block-break guard protects a configurable-radius vicinity around each hole's authored tee and cup positions. Greens are protected via cup vicinity and tee boxes via tee vicinity; no new green metadata is added to `HoleDefinition` in M7. The cup/flag block itself is always protected regardless of radius.
- **S2 advance UX — HUD button.** Advance remains player-initiated and barrier-gated. A no-text-input HUD prompt/button appears when all active golfers are terminal and invokes the existing server-side `nextHole` barrier call. `/golf nexthole` and the clickable chat action remain as fallbacks. Full auto-advance is rejected for M7 and remains a documented future option that would require an explicit contract deviation.

## Bounded slices

Ordered so that the two known playtest findings are resolved first, followed by structured playtesting, then tuning/defect work driven by what the sessions find. Slices S1 and S2 are the only slices with significant agent-implementable scope before playtesting; S4 and S5 must be fed by real session findings.

### S1 — Course protection: block-break guard

- **Goal:** Stop golf-course destruction in creative mode without a world-guard framework and without breaking the fun of clearing in-the-way trees/rocks or the `/golf dev preparecourse` repair path.
- **Deliverables:**
  - Minecraft-free protected-zone model in the course/hole domain: per-hole tee and cup vicinity zones with a configurable radius, derived from the authored `HoleDefinition` positions (per the recorded S1 decision, no new green metadata).
  - A `PlayerBlockBreakEvents`-based server-side guard that cancels breaks of blocks inside the configured tee/cup vicinity zones (protecting tee boxes, greens, and the cup/flag vicinity) and always protects the cup/flag block itself.
  - Operator/dev exemption so `/golf dev preparecourse` (and any future repair command) can still rebuild protected areas; ordinary players remain free to clear vegetation/terrain outside protected zones.
  - Configuration for the vicinity radius (server gameplay config), with a sane default; no full world-guard framework.
  - Focused unit tests for zone membership/radius logic plus integration evidence that breaks inside zones are rejected and outside zones remain allowed.
- **Verification:** `./gradlew test`, `./gradlew build`, then Loom `runClient`/Docker creative-mode manual check (break on green/tee/cup rejected; clear a tree outside a zone succeeds).
- **Exit criteria:** Creative-mode golfers cannot damage tee boxes, greens, or the cup/flag; in-the-way clearing outside protected zones still works; `/golf dev preparecourse` still repairs the course.

### S2 — Advance UX: deliberate decision and minimal implementation

- **Goal:** Resolve the M6 S4 advance-UX playtest finding as an explicit product decision, then implement the chosen option with minimal scope.
- **Deliverables:**
  - The recorded decision (2026-09-10, see "Recorded M7 decisions" and `docs/MILESTONES.md`): **HUD button** — a no-text-input HUD prompt/button shown when all active golfers are terminal, invoking the existing server-side barrier call; `/golf nexthole` and the chat action remain as fallbacks; full auto-advance is rejected for M7 and would require a recorded contract deviation if revisited.
  - Implementation of the recorded option only; server authority and the barrier logic are preserved.
- **Verification:** `./gradlew test`, `./gradlew build`; manual two-golfer check that the chosen flow works and early advancement is still rejected.
- **Exit criteria:** The playtest finding is resolved with a documented decision and a working implementation; the M6 barrier contract remains intact.

### S3 — Structured playtesting and deferred manual acceptance

- **Goal:** Run repeated family/LAN sessions (1–4 golfers) with repeatable checklists so findings are captured, not recalled.
- **Deliverables:**
  - A short, repeatable playtest checklist per session covering the M7 observation areas: ball feel, swing difficulty, shot pacing, club balance, putting, camera usability, multiplayer synchronization, hole pacing, scoring clarity, course navigation, and Minecraft terrain interactions.
  - A findings log template tied to severity and priority, feeding S4/S5 and the V1 decision.
  - Run the deferred two-player manual matrix (items 1–9 from `docs/M6-PLAN.md`) when a second golfer is available, and an optional true four-player session during family playtesting.
  - Confirm the club-sprite repair holds in first-person, third-person, and inventory presentation across clients.
- **Verification:** at least one family/LAN session with two or more golfers, ideally repeated; the two-player matrix completed when possible; observations recorded per checklist.
- **Exit criteria:** Two or more golfers complete the three-hole Ready Golf course without state corruption or forced turns; observations and the two-player matrix results are documented; the remaining gap to four-player is explicit.

### S4 — Tuning from playtest findings

- **Goal:** Apply the documented M7 success questions (is hitting satisfying, is the swing understandable, does Ready Golf keep the round moving, are Minecraft interactions fun, do players want to replay) to targeted tuning.
- **Deliverables:**
  - Tuning changes driven by session findings, starting from the already-flagged items: Hole 3 length/route pacing for a par 5, and Driver/Fairway Wood rollout. Club balance, putting, shot pacing, and camera usability are tuned only when sessions produce clear signals.
  - Every tuning change lands with its carry/physics tests updated so accepted behavior stays pinned.
- **Verification:** `./gradlew test`; the specific manual check that motivated each tuning change; no regression to accepted M5 club behavior or M6 concurrency.
- **Exit criteria:** Tuning changes are small, deliberate, test-backed, and traceable to a session finding; the M5/M6 accepted feel remains intact.

### S5 — Defect fixing from prioritized findings

- **Goal:** Fix the MVP-critical defects family play reveals, prioritized by the findings log.
- **Deliverables:**
  - A prioritized defect list from the M7 sessions covering the milestone's fix targets: crashes, desync, stuck balls, impossible recovery states, misleading HUD behavior, scoring errors, and major usability problems.
  - Fixes for all MVP-critical items; lower-priority items are documented and deferred explicitly rather than silently ignored.
- **Verification:** `./gradlew test`, `./gradlew build`, Loom server boot, and re-run of the session scenario that exposed each fixed defect.
- **Exit criteria:** Every MVP-critical defect from the sessions is fixed or explicitly deferred with rationale; the previously failing scenarios pass.

### S6 — Final verification and V1 decision

- **Goal:** Close M7 only with stable-build, dedicated-server, and real play evidence, plus a documented V1 decision.
- **Automated and static checks:**
  - `./gradlew test`
  - `./gradlew clean build`
  - confirm no `net.minecraft.client` imports under `src/main/java`
  - `git diff --check`
  - record final test count and focused M7 coverage
- **Loom and Docker checks:**
  - Loom client startup plus HUD, camera, and resource/model checks; solo M5 regression re-check (hole start, restart, abandon → start).
  - Loom dedicated server reaches `Done` with all handlers registered.
  - `./scripts/dev-server-sync.sh` + `./scripts/dev-server-up.sh`; container health and staged/container JAR SHA-256 identity.
  - `/golf dev preparecourse` idempotence and no layout drift.
- **Manual/playtest closeout:**
  - Completed playtest checklist records; documented two-player matrix results; any four-player session results; prioritized defect list; tuning changes; resolved MVP-critical issues.
  - Written V1 decision (proceed / iterate in M7 / stop) with rationale tied to the M7 success questions.
- **Exit criteria:** The MVP is stable enough for repeated family play and has demonstrated enough fun/replay value to continue development; the V1 decision is recorded.

## Risk register and stop conditions

| Risk | Mitigation / proof |
|---|---|
| Block-break guard blocks the fun clearing of trees/rocks | Protect only authored zones (tee/green/cup vicinity) with a configurable radius; everything outside stays breakable; operator/dev repair path exempt. |
| Guard prevents `/golf dev preparecourse` recovery | Operator/dev exemption proven by re-running preparecourse to zero changes after a damage scenario. |
| Full auto-advance regresses the M6 barrier contract | Rejected for M7 by the recorded S2 decision (HUD button chosen); barrier/authority logic unchanged; `/golf nexthole` and the chat action remain as fallbacks. |
| Tuning drifts the accepted M5 feel | Small, single-purpose changes with carry/physics tests pinned; re-run M5 regression checks. |
| No second golfer available for the two-player matrix | Document the gap explicitly; M7 family sessions are the scheduled opportunity; four-player evidence stays unit-test + integration + code review as in M6. |
| Family sessions produce vague/unrecorded feedback | Repeatable checklists and a findings log with severity/priority; findings tie every S4/S5 change to a recorded observation. |
| Scope creep into the deferred V1 list | Non-goals enforced; M7 fixes only MVP-critical items. |

Stop and consult the user before proceeding if:

- resolving the advance-UX finding requires changing server authority or the M6 barrier contract without an explicit recorded decision;
- the block-break guard cannot be implemented with Fabric events and requires a Mixin without justification;
- a proposed fix expands into persistence, matchmaking, multiple simultaneous rounds, cosmetic systems, or the deferred V1 feature list;
- any verification rung fails and cannot be repaired within the current slice.

## Non-goals

The deferred V1 feature list in `docs/MILESTONES.md` (wind, advanced terrain/lie effects, cinematic camera, landing-area indicator, optional teleport-to-ball, water-recovery choice, chip-shot mode, expanded clubs, richer sounds/particles, refined HUD, survival/mob options, traditional turns) remains out of scope. Also out of scope: persistence/global statistics, matchmaking, course authoring, multiple courses or simultaneous rounds, and any new infrastructure or speculative abstractions.

## Completion record to capture

At closeout, record the final commit, test count, block-break guard design and config, the recorded advance-UX decision, playtest checklist results, two-player matrix outcomes, any four-player session results, prioritized defects, tuning changes (each tied to a finding), Loom/Docker/JAR evidence, solo M5 regression results, and the V1 go/no-go decision with rationale.
