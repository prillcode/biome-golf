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
- [ ] A completed golfer cannot advance while another active golfer is unfinished.
- [ ] When all active golfers finish Hole 1 or 2, every client receives the advance prompt.
- [ ] “Not now” closes the prompt without advancing.
- [ ] “Go to next tee” advances everyone exactly once.
- [ ] `/golf nexthole` and the clickable chat action still work as fallbacks.
- [ ] Hole 3 finalizes automatically without an advance prompt.

### Deferred M6 two-player matrix

- [ ] Create/join/start one shared round; two owned balls and player-specific HUD values.
- [ ] Overlapping shots with different clubs; both launches, cameras, rests, and owner travel.
- [ ] Different stroke/penalty totals remain independent.
- [ ] One player finishes first; waiting state and early advancement rejection are correct.
- [ ] Second player finishes; one coordinated transition and correct hole results.
- [ ] Individual restart, missing-ball recovery, and withdrawal do not affect the other player.
- [ ] Disconnect/reconnect, offline advancement, reconnect after withdrawal, disconnect in flight,
      and all-disconnected recovery behave as specified in `docs/M6-PLAN.md` S5.
- [ ] Hole 3 produces accurate player-specific scorecards and shared final results.
- [ ] Solo `/golf hole start`, restart, abandon → start, and replay remain correct afterward.

### Presentation checks

- [ ] Every club renders correctly in inventory.
- [ ] Every club renders correctly in first person.
- [ ] Every club renders correctly in third person on both clients.
- [ ] No magenta/black missing-texture presentation appears.

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
| M7-F02 | M6 S4 | High | Hole transition UX | Players wanted a no-text-input advance action | Player-initiated HUD prompt using the existing server barrier | S5 defect | Implemented; manual verification pending |
| M7-F03 | M7 smoke test | High | Network compatibility | An M6 client connecting to the M7 server was disconnected because `hole_state` gained one byte under the same payload ID | Version changed payload channels so Fabric capability negotiation suppresses unsupported schemas instead of invoking an incompatible decoder | S5 defect | Fixed as `hole_state_v2`; matching M7 client reconnect verified 2026-09-10 |
| M7-F04 | M7 manual verification | Low | Course integrity | The 12-block boundary worked as designed, but the tester may prefer a larger protected surrounding area later | Keep the accepted M7 radius; reconsider configurable or broader authoring protection from future playtest evidence | Deferred | Recorded for M8 consideration |
| M7-F05 | M7 manual verification | Medium | Minecraft interaction | Tester wants normal golf play to use Survival with Peaceful difficulty; vanilla block interaction is sufficient and need not be club-exclusive | Make Survival + Peaceful the persistent Docker gameplay profile while preserving vanilla breaking outside protected course zones | S5 defect | Applied and live-verified 2026-09-10 |

## Prioritized defects

| Priority | Finding | Decision |
|---:|---|---|
| 1 | M7-F01 course destructibility | Accepted on Docker: non-OP protection, OP repair, `10 changed` then `0 changed` |
| 2 | M7-F02 advance UX | M7 S2 implementation complete; manual two-client acceptance pending |
| 3 | M7-F03 stale-client packet decode failure | Versioned the changed clientbound channel as `hole_state_v2`; matching M7 client reconnect passed |

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
- Scope note: this was an implementation acceptance pass, not the required
  two-or-more-player family/LAN session.
