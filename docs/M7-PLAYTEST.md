# M7 — Family/LAN Playtest Record

Status: **Ready for manual sessions.** Complete one copy of the session record for
each playtest. Findings in this document are the only inputs authorized to drive
M7 S4 tuning and S5 defect fixes.

## Session record template

- Date/time:
- Build commit/JAR SHA-256:
- Server environment: Loom / Docker LAN / other
- Course seed: `-1928790872702396508`
- Players and Minecraft familiarity:
- Player count:
- Holes completed:
- Session duration:
- Observer/recorder:

### Setup

1. Run `/golf dev preparecourse` twice; record the first and second changed-block counts.
2. One golfer runs `/golf round create`; the others run `/golf round join`.
3. The coordinator runs `/golf round start`.
4. Confirm each golfer owns a separate ball and sees only their own score/HUD state.

### Experience checklist

Rate each item 1–5 and add a short observation. A rating without an observation is
not sufficient evidence for a tuning change.

| Area | Rating | Observation |
|---|---:|---|
| Ball flight and roll feel |  |  |
| Three-click swing understanding |  |  |
| Swing difficulty |  |  |
| Shot pacing |  |  |
| Club selection/balance |  |  |
| Putting |  |  |
| Post-shot camera |  |  |
| Ready Golf synchronization |  |  |
| Hole pacing |  |  |
| Scoring clarity |  |  |
| Navigation to the next tee |  |  |
| Minecraft terrain interactions |  |  |

### M7 protection and advance checks

- [x] Ordinary creative-mode golfer cannot break Hole 1 tee-box blocks.
- [x] Ordinary creative-mode golfer cannot break blocks at each green/cup vicinity.
- [x] Ordinary golfer cannot break the golf cup/flag block.
- [x] Ordinary golfer can clear a tree or rock outside all protected zones.
- [x] Operator can modify a protected block for repair work.
- [x] `/golf dev preparecourse` repairs deliberate operator damage.
- [x] A second `/golf dev preparecourse` reports zero changed blocks.
- [x] A completed golfer cannot advance while another active golfer is unfinished.
- [x] When all active golfers finish Hole 1 or 2, every client receives the advance prompt.
- [x] “Not now” closes the prompt without advancing.
- [x] “Go to next tee” advances everyone exactly once.
- [x] `/golf nexthole` and the clickable chat action still work as fallbacks.
- [x] Hole 3 finalizes automatically without an advance prompt.

### Deferred M6 two-player matrix

- [x] Create/join/start one shared round; two owned balls and player-specific HUD values.
- [x] Overlapping shots with different clubs; both launches, cameras, rests, and owner travel.
- [x] Different stroke/penalty totals remain independent.
- [x] One player finishes first; waiting state and early advancement rejection are correct.
- [x] Second player finishes; one coordinated transition and correct hole results.
- [ ] Individual restart, missing-ball recovery, and withdrawal do not affect the other player.
- [ ] Disconnect/reconnect, offline advancement, reconnect after withdrawal, disconnect in flight,
      and all-disconnected recovery behave as specified in `docs/M6-PLAN.md` S5.
- [x] Hole 3 produces accurate player-specific scorecards and shared final results.
- [ ] Solo `/golf hole start`, restart, abandon → start, and replay remain correct afterward.

### Presentation checks

- [x] Every club renders correctly in inventory.
- [x] Every club renders correctly in first person.
- [ ] Every club renders correctly in third person on both clients.
- [x] No magenta/black missing-texture presentation appears.

### Success questions

- Was hitting the ball satisfying? Why or why not?
- Did players understand the swing without coaching? Where did they hesitate?
- Did Ready Golf keep the round moving?
- Which Minecraft interactions were fun or frustrating?
- Did players want to replay the course?
- Did players propose or begin imagining new holes?
- Would the family voluntarily play again?

## Findings log

Use severity `Critical`, `High`, `Medium`, or `Low`. Use disposition `S4 tuning`,
`S5 defect`, `Deferred`, or `No change`. Do not implement a finding until its
reproduction/evidence and intended result are recorded.

| ID | Session | Severity | Area | Evidence/reproduction | Intended result | Disposition | Status |
|---|---|---|---|---|---|---|---|
| M7-F01 | M6 S4 | High | Course integrity | Creative golfers could destroy tees, greens, and cup/flag | Protect authored tee/cup vicinities while leaving ordinary terrain editable | S5 defect | Accepted on Docker 2026-09-10 |
| M7-F02 | M6 S4 | High | Hole transition UX | Players wanted a no-text-input advance action | Player-initiated HUD prompt using the existing server barrier | S5 defect | Solo paths accepted; two-player barrier verification pending |
| M7-F03 | M7 smoke test | High | Network compatibility | An M6 client connecting to the M7 server was disconnected because `hole_state` gained one byte under the same payload ID | Version changed payload channels so Fabric capability negotiation suppresses unsupported schemas instead of invoking an incompatible decoder | S5 defect | Fixed as `hole_state_v2`; matching M7 client reconnect verified 2026-09-10 |
| M7-F04 | M7 manual verification | Low | Course integrity | The 12-block boundary worked as designed, but the tester may prefer a larger protected surrounding area later | Keep the accepted M7 radius; reconsider configurable or broader authoring protection from future playtest evidence | Deferred | Recorded for M8 consideration |
| M7-F05 | M7 manual verification | Medium | Minecraft interaction | Tester wants normal golf play to use Survival with Peaceful difficulty; vanilla block interaction is sufficient and need not be club-exclusive | Make Survival + Peaceful the persistent Docker gameplay profile while preserving vanilla breaking outside protected course zones | S5 defect | Applied and live-verified 2026-09-10 |
| M7-F06 | M7 manual verification | Low | Club presentation | All seven clubs rendered in inventory, first person, and third person on the connected client; models are flat 2D items | Flat item presentation is acceptable for MVP | No change | Accepted 2026-09-10; second-client third-person check pending |
| M7-F07 | M7 solo round | Medium | Finalization UX | Server correctly reported no active round after Hole 3, but the retained Hole 3 `COMPLETE` HUD made the player feel stuck | Keep the final score visible with an explicit `ROUND COMPLETE` label and replay/exit guidance | S5 defect | Accepted in two-player Docker session 2026-09-10 |
| M7-F08 | M7 two-player LAN | Medium | Client compatibility | Both golfers appeared to lose the hole HUD after the server payload was versioned; the running clients had not been restarted after their JARs were replaced | Make the required full client restart explicit when distributing an updated JAR; reconnect restores authoritative HUD state | No change | Diagnosed and accepted after full client restart 2026-09-10 |
| M7-F09 | M7 two-player LAN | Medium | Scoring clarity | After a completed 4/3/3 round, `/golf hole status` reported 13 strokes while the final HUD correctly reported 10; the status formatter added the final hole twice | Completed-round status must use the final authoritative scorecard totals | S5 defect | Fixed 2026-09-10; Docker recheck pending |

## Prioritized defects

| Priority | Finding | Decision |
|---:|---|---|
| 1 | M7-F01 course destructibility | Accepted on Docker: non-OP protection, OP repair, `10 changed` then `0 changed` |
| 2 | M7-F02 advance UX | Solo prompt/button/dismissal/chat/command/final-hole paths accepted; two-client barrier pending |
| 3 | M7-F03 stale-client packet decode failure | Versioned the changed clientbound channel as `hole_state_v2`; matching M7 client reconnect passed |
| 4 | M7-F07 ambiguous final HUD | Accepted: final score remains visible under explicit `ROUND COMPLETE` with replay/exit guidance |
| 5 | M7-F09 completed-round command total | Fixed status formatting to use final scorecard totals instead of adding the retained final hole twice |

Add session-discovered crashes, desync, stuck balls, impossible recovery, misleading
HUD behavior, scoring errors, and major usability problems here before starting S5.

## Tuning candidates awaiting evidence

- Hole 3 may play short for a par 5.
- Driver and Fairway Wood may need slightly more rollout than irons.

These are observations carried from M5, not authorization to change the accepted
course or physics baseline. Promote either item to S4 only when a recorded M7
session provides a clear signal and a specific desired outcome.

## Session records

Duplicate the session record template below this heading for each completed session.

### M7 implementation verification — 2026-09-10

- Build: `0861513`; JAR SHA-256
  `cde1846f5db84573440e0e5af78d7e5f3d11cd7988e8d4130c860fdbe1dabd81`
- Environment: Docker server on localhost; one connected player (`PrLLager207`)
- Initial preparation: `574612 planned`, first run `6 changed`, second run `0 changed`
- Non-operator Creative verification: Hole 1 and Hole 2 tees, every cup/green,
  and the cup/flag resisted breaking; ordinary blocks beyond the protected radius
  remained editable.
- Operator verification: a protected Hole 3 green block could be broken.
- Repair verification: `/golf dev preparecourse` reported `10 changed`, followed
  immediately by `0 changed`.
- Gameplay-profile decision: live player and server default changed to Survival;
  difficulty confirmed Peaceful. Vanilla obstacle breaking remains available and
  is not restricted exclusively to golf clubs.
- Solo advance verification: **Not now** dismissed without movement; the HUD
  button advanced Hole 1 to Hole 2 exactly once; clickable chat advanced Hole 2
  to Hole 3 exactly once; Hole 3 finalized with a scorecard and no prompt.
- Command fallback verification: a second round used the HUD for Hole 1 to Hole
  2, then `/golf nexthole` for Hole 2 to Hole 3; authoritative status showed one
  active Hole 3 ball. The temporary round was then cleanly left.
- Presentation verification: all seven clubs rendered without missing textures in
  inventory, first person, and third person on the connected client. The tester
  accepted the flat 2D item style for MVP; second-client third-person presentation
  remains part of the multiplayer session.
- Scope note: this was an implementation acceptance pass, not the required
  two-or-more-player family/LAN session.

### Two-player family/LAN acceptance — 2026-09-10

- Build/JAR: `a0f8444` / SHA-256
  `0b43f8c9b75717a10f3fd889204c4302e6e64fe5e0538f31deff573bbb095520`
- Environment: Docker LAN; two family players (`PrLLager207`, `pomzii_YT`)
- Two complete three-hole rounds were observed in the authoritative server log.
  Both rounds used separate player-owned balls with independent strokes and
  near-overlapping shots using different clubs. Natural rests and owner travel
  continued independently.
- Barrier verification: `PrLLager207` was completed first by Pick Up while
  `pomzii_YT` remained active. An immediate next-hole request was rejected at
  `1/2 complete`. Completing the second golfer fanned the advance prompt out to
  both clients; a client action advanced both players exactly once. Subsequent
  Hole 1→2 and Hole 2→3 transitions also occurred exactly once.
- Independent scoring was preserved (including different Hole 2 totals), and
  Hole 3 completion produced player-specific totals plus shared final results.
- HUD diagnosis: clients whose JAR was replaced without fully quitting Minecraft
  continued running the prior payload schema and therefore lacked the hole HUD.
  Fully restarting Minecraft loaded `hole_state_v3`; reconnect restored the
  authoritative display. The final-state screenshot visibly confirms the
  retained score and explicit `ROUND COMPLETE` label.
- Defect found: `/golf hole status` double-counted the retained final hole after
  round completion even though the HUD score was correct. M7-F09 tracks the fix.
- Still pending from the deferred matrix: individual restart/missing-ball/
  withdrawal isolation, the full disconnect matrix, solo regression after the
  multiplayer session, and second-client third-person club presentation.
- Experience ratings and M7 success-question answers were not inferred from the
  structural acceptance run and remain to be captured from the players.
