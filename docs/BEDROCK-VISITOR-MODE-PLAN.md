# Bedrock Visitor Mode — Plan

**Status:** Implemented (2026-09).
**Supersedes:** the Bedrock play paths of `docs/BEDROCK-GUEST-MODE-PLAN.md` (the play mode is
dormant; visitors cannot play).
**Related:** `docs/M10.3-CLOSEOUT.md` (why Bedrock play was shelved).

## Purpose

Turn Bedrock (and unmodified-Java) connections into a **promotional visitor experience**: they
can watch a live round and are invited to join on Java, instead of playing a degraded version.
This is the deliberate compromise after the 2026-09 playtest showed Bedrock play feel was
unacceptable and the project returned its development focus to Java.

## What a visitor is

Any client that cannot receive the modded payloads —
`!ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE)`, i.e. Bedrock through Geyser or
unmodified Java — is a **visitor**. The modded Java client is never affected.

## Two view modes (neither plays golf)

Visitors choose one view; golf itself stays Java-only:

| Mode | Command | Behaviour |
|---|---|---|
| Spectator (default) | `/golf spectator` | Fly, phase through blocks, invisible, no interaction |
| Survival | `/golf survival` | Normal world play — build and break freely outside authored course regions (which the course guard protects) |

Both are blocked from all golf commands (`/golf swing`, hole/round start/restart/join, practice
ball, pickup, tap-in, next-hole) with a message pointing to Java. `ShotService` also rejects
visitors as defence in depth, and the G1 tap meter is dormant. Survival visitors are ordinary
world players otherwise; only the golf commands are gated.

## Server model: peaceful survival with protected courses

BirdieBiome is intended to be a normal multiplayer world where players do ordinary Minecraft
things outside the golf courses. The mod does not restrict general play; it protects authored
course regions only (breaks, placements, and TNT inside tee/cup vicinities and whole-course
landscape perimeters — see `CourseBlockBreakGuard`). Recommendations for the server:

- Set `difficulty=peaceful` so there are no hostile mobs.
- Lock course landscape perimeters (`/golf landscape ...`) so even operators cannot grief them;
  unlocked perimeters already deny non-operators.
- Everything outside course regions remains fully editable for every player, including Bedrock
  survival visitors.

## Onboarding and promotion

- On join, a visitor is put into spectator mode, optionally teleported to a configured
  viewpoint, and sent a welcome: the two view-mode commands plus the "join on Java to golf"
  invite (address and optional mod link).
- A periodic action-bar reminder repeats the invite (default every 60s; `0` disables).
- In-world **signage is world content** — vanilla signs such as "Wanna play? Join on Java!"
  at spawn/course entrances. Bedrock visitors see vanilla signs fine (unlike the custom
  cup/flag blocks). No code is required for signage.

## Configuration

Persisted to `data/minecraft_golf_visitor.json` in the world folder (same pattern as the
practice range). Operator commands (game-master level):

```text
/golf visitor status
/golf visitor address <address>     # e.g. golf.example.com:25565
/golf visitor link <url>            # optional mod download
/golf visitor reminders <seconds>   # 0 disables
/golf visitor spawn set | clear     # where visitors arrive
```

Visitors themselves only need `/golf spectator` and `/golf survival`.

## Verification performed

- `./gradlew test` / `./gradlew build`: 337 tests passing (adds `VisitorTextTest`).
- Docker dev server synced and restarted; healthy; host/container JAR hashes matched.
- RCON exercised the config commands and confirmed the world JSON persists reload.
- Real Bedrock client (mobile): visitor welcome received and spectator behaviour confirmed.
- **Not verified:** reminder visibility on a long session.

## Risks / notes

- **Geyser spectator quirks.** Bedrock spectator exists (1.19.50+) and Geyser maps Java
  spectator onto it, but open cosmetic issues exist. If it misbehaves, Survival is the fallback;
  the mode is switchable at runtime.
- **Survival switch position.** A player switching from spectator to survival can land inside
  terrain (survival does not noclip); `/golf spectator` recovers.
- **Address required.** The invite is only useful once `/golf visitor address` is set; until
  then the welcome asks the host for it.
- **Ops on vanilla Java** become visitors too (they are client-light); run `/golf spectator`
  or `/golf survival`, or use a modded client.

## Out of scope

- Bedrock play, Bedrock visual parity, and a native Bedrock Add-On
  (`docs/BEDROCK-NATIVE-ADDON-RESEARCH.md`).
- Server-side clickable CTAs in chat (Bedrock click support varies; typed commands are the
  reliable path).
