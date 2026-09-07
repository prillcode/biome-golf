# M2 — Clubs and Shot Execution: In-Progress Notes

Branch: `m2-clubs-and-shots` (base `main` @ `20af1b7`, all 3 M1 commits pushed).

## Scope (from MILESTONES.md M2)
Club item definitions + registration, club property model, camera-direction aiming,
server-authoritative shot request, ball ownership, club melee — **without** the
three-click meter/HUD (that is M3).

## Design decision (D009) — shot trigger before M3 exists
Right-click `use()` on an equipped club resolves the shot **server-side**: the
owner's camera yaw/pitch via `ShotResolver` through the club's `ClubDefinition`
into a full-power launch of the owner's own (or an unowned) *resting* ball.
Melee (left-click attack) is strictly separate and never starts a shot. M3 later
threads power/accuracy into the same resolver/launch contract.

## Delivered and committed on `m2-clubs-and-shots`
- (`44118d0`) Pure club domain + resolution:
  - `club/ClubDefinition` (immutable, Minecraft-free; add meleeDamage in later commit)
  - `club/GolfClubs` — 7 PRD clubs, speeds under the 4-blocks/tick M1 ceiling
  - `club/ShotResolver.initialVelocity` — pure club+aim→velocity, Minecraft yaw
    convention (+Z=yaw0, east=+X@-90), steep-down rejected, capped at maxSpeed
  - `src/test/.../club/ClubDomainTest` — 16 headless tests
- (`1081fd3`) Items + ownership + shot trigger:
  - `item/GolfClubItem` (extends Item): server `use()` full-power shot toward camera;
    `getAttackDamageBonus` = club.meleeDamage (melee separated from shots)
  - `item/GolfItems` registry — 7 items registered in `onInitialize`
  - `GolfBallEntity`: persistent owner UUID (claim-on-first-hit; `canBeStruckBy`
    only own-or-unowned), NBT round-trip, wired into `MinecraftGolf`
  - Client resources: `en_us.json` names, per-club generated item models over one
    shared simple `club.png` (real per-club art deferred)
- (`ef67d5e`) dev: `/golf inspect` prints ball owner (`unowned`/UUID prefix)

## Verification done
- `./gradlew cleanTest test build` green (suite 75+ → now 76 tests incl. melee
  validation); zero `net.minecraft.client` / `minecraftgolf.client` imports in `src/main`
- Dedicated server boots the M2 jar clean:
  - `Registered 7 golf club items`, entity + dev commands registered, 0 errors
  - rcon `/golf spawn`/`inspect` show owner field; ball settles RESTING `owner unowned`
- Evidence log: `build/m2-devserver-evidence.log` (gitignored, disposable)

## Not yet verified (needs a real player/client — human visual/manual gate)
- Right-click `use()` shot actually fires a ball toward the camera in a live client,
  and ownership claim-on-first-hit + "belongs to another player" rejection behave
  in 2-player play.
- Club items visually distinguish / feel okay when given (`/give @s
  minecraft_golf:club_driver`) and used; real per-club art pending.
- Per-club distance/lie feel (tuning is M7).

## Trying it in a client (cheats/creative, op)
1. `./gradlew runClient` (or connect to the Docker server — dev-server is creative)
2. `/give @s minecraft_golf:club_driver 1` (and wedge/putter) from creative menu too
3. Start with no ball owned: `/golf spawn` then walk near it, aim your camera level
   at an open lane, right-click the club → ball should fly along your aim.
4. `/golf inspect` should then show an owner prefix on that ball; a second player
   cannot strike it.

## Next (still M2 or spillover to M3)
- Confirm the `use()` visual shot + ownership in 2 clients (this session or the
  human-manual pass).
- M3: three-click meter + HUD; power/accuracy layers into `ShotResolver` contract.
