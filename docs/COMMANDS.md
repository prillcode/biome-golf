# Biome Golf Commands

This is a reference for the commands registered by Biome Golf. Commands are shown
with the `/` prefix for in-game use. Replace values in angle brackets with the value
described; square brackets mark optional values.

Operator-only commands require Minecraft's gamemaster permission level. In a dedicated
server, this generally means an operator. Visitors using Bedrock through Geyser or
unmodified Java can join the world and watch, but cannot play golf.

## Regular player commands

These commands are available to regular players. Whether an action succeeds can still
depend on your round, lobby, ball, or course state.

### Help and course discovery

| Command | What it does |
|---|---|
| `/golf help` | Print the short player command guide. |
| `/golf help admin` | Show operator/gamemaster commands. Visible to everyone; execution still requires the listed permissions. |
| `/golf hud` | Ask your compatible Biome Golf client to toggle both the Swing and Hole HUDs. No operator permission needed; visibility resets when the game session ends. |
| `/golf mode status` | Show your current player mode (Golf, World, or Builder) and the available switches. |
| `/golf mode golf` | Enter Golf mode: Creative-style flight and damage protection with your normal Survival items. |
| `/golf mode world` | Switch to ordinary Survival/Peaceful play. An unfinished attempt must be abandoned with `/golf mode world confirm`. |
| `/golf mode build` | Operator: enter Creative Builder mode. Lists authored courses as a reference to mark. |
| `/golf mode build <courseId> confirm` | Operator: enter Builder mode and select a course in one step. `confirm` abandons an unfinished attempt first. |
| `/golf builder course <courseId>` | Operator: mark the course you are designing while in Builder mode (shown in mode status; Creative editing itself is not scope-limited). |
| `/golf builder restock` | Operator: reset the Builder inventory to the palette starter kit (Driver through Wedge clubs plus one of every configured palette block). |
| `/golf builder palette reload` | Operator: reload `config/minecraft_golf/builder_palette.json`. Malformed files, unknown items, and unsafe items retain the last valid palette. |

Builder mode is true Creative for operators: one-click break/place, flight, and the
full item catalogue. The palette starter kit fills the inventory on entry — Driver
through Wedge clubs plus one of every configured palette block — so you can drop a
practice ball and play-test your design as you build (`/golf practice ball`).
`/golf builder restock` resets the kit and `/golf builder palette reload` redefines it.
Items you carry out of Builder stay with you in World play; authored landscape
perimeters still protect courses, and a locked perimeter denies even the Builder.
| `/golf browse` | Open the in-game course browser. |
| `/golf round list` | List open Ready Golf lobbies and finalized courses. Lobby entries include a clickable join action. |
| `/golf visitor status` | Show the visitor welcome configuration, including any Java address or mod link set by an operator. |

### Multiplayer rounds

| Command | What it does |
|---|---|
| `/golf round create <courseId>` | Create a lobby for a finalized course. The creator becomes the coordinator. |
| `/golf round create` | Create a lobby using the currently selected course. |
| `/golf round join <roundId>` | Join a particular open lobby using its full, server-issued UUID. Use `/golf round list` to find it. |
| `/golf round join` | Join the only open lobby. If there are multiple open lobbies, use `/golf round list` and join by ID. |
| `/golf round start` | Start your lobby. Only its coordinator can start it. |
| `/golf round status` | Show your lobby or round status. |
| `/golf round leave` | Leave a lobby or multiplayer round. It also abandons solo play and exits a completed round. |
| `/golf round restart` | After a round is complete, restart it from Hole 1 for the remaining golfers. |

Players use Ready Golf: each golfer plays independently without waiting for a fixed
turn order.

### Solo play

| Command | What it does |
|---|---|
| `/golf course play` | Start the selected or default course at Hole 1. |
| `/golf course play <courseId>` | Start the named finalized course at Hole 1. |
| `/golf course play <courseId> <hole>` | Start the named course at the specified hole. |
| `/golf hole start` | Start the selected or default course at Hole 1. |
| `/golf hole start <hole>` | Start the selected or default course at the specified hole. |

An explicit `/golf course play ...` replaces your current solo attempt, including a
completed attempt. It does not silently leave a multiplayer lobby or round; use
`/golf round leave` first. A failed course-play request leaves the current attempt
intact.

### Playing a hole

| Command | What it does |
|---|---|
| `/golf hole status` | Show your current-hole status. |
| `/golf round status` | Show your current round or lobby status. |
| `/golf hole restart` | Restart your current hole. |
| `/golf pickup` | Pick up your ball and finish the hole at its configured stroke limit. |
| `/golf tapin` | Take a one-stroke Tap-In when the server says your ball is close enough to the cup. |
| `/golf nexthole` | Advance to the next hole after completing the current one. The on-screen prompt and clickable chat action are alternatives. |
| `/golf clubs equip` | Restore your golf clubs. |

### Swing by command

`/golf swing` hits the nearest eligible resting ball along your look direction. The
optional values are power and accuracy from `0.0` to `1.0`, followed by a shot type.
Omitted values use full power, the default accuracy value, and Standard.

| Command | What it does |
|---|---|
| `/golf swing` | Take a Standard shot using the defaults. |
| `/golf swing <power>` | Set shot power. |
| `/golf swing <power> <accuracy>` | Set power and accuracy. |
| `/golf swing <power> <accuracy> <shotType>` | Also request `standard`, `chip`, `stinger`, or `flop`. The server checks that the held club allows that shot type. |

For example: `/golf swing 0.7 0.5 standard`. Modded Java players can also use the
in-game swing meter and shot-type controls. Press `H` to hide or show both golf HUDs;
a right-click swing attempt with a golf club automatically reveals them again.

### Practice

| Command | What it does |
|---|---|
| `/golf practice ball` | Drop a practice ball. |
| `/golf practice clear` | Remove only practice balls you own, across dimensions. Your assigned in-play ball and other players' balls are preserved. |
| `/golf practice tee` | Teleport to the configured practice tee, if it is set in your current dimension. |
| `/golf practice target list` | Show the saved practice tee and targets. |

Practice tee and target setup commands are operator-only; see the operator section.

### Visitor view controls

Bedrock and unmodified-Java visitors can use these commands to watch golf, but cannot
play:

| Command | What it does |
|---|---|
| `/golf spectator` | Switch to spectator mode to watch a round. |
| `/golf spectator leave` | Leave spectator mode and return to survival. |

## Operator commands

The following commands require gamemaster permission. They configure the server, practice
range, and visitor onboarding. Course creation and editing commands are in the next
section.

### Default course

| Command | What it does |
|---|---|
| `/golf course default set <id>` | Set the persisted fallback course used when there is no runtime course selection. |
| `/golf course default status` | Show the persisted default course. |
| `/golf course default clear` | Remove the persisted default course. |

### Practice range setup

| Command | What it does |
|---|---|
| `/golf practice tee set` | Save your current standing position as the practice tee. |
| `/golf practice target set <1-8>` | Set or move a numbered target at your current position and place its cup and flag. |
| `/golf practice target clear <1-8>` | Remove that target's cup and flag and clear its saved location. |

Target setup requires the target position to be in the same dimension as the player.

### Visitor setup

| Command | What it does |
|---|---|
| `/golf visitor status` | Show the visitor configuration. This command is also available to regular players. |
| `/golf visitor address <address>` | Set the Java server address shown to visitors. |
| `/golf visitor link <url>` | Set the optional mod download link shown to visitors. |
| `/golf visitor spawn set` | Save your current position as the visitor arrival point. |
| `/golf visitor spawn clear` | Remove the configured visitor arrival point. |

Visitor mode is for joining the world or watching a round; golf gameplay remains Java
mod-client-only.

### Golf ball diagnostics and development helpers

These operator commands are intended for administration or development, rather than
normal play.

| Command | What it does |
|---|---|
| `/golf spawn` | Spawn an unowned debug golf ball a short distance in front of and above you. |
| `/golf spawn <x> <y> <z>` | Spawn a golf ball at the specified coordinates. |
| `/golf launch <forward> <up>` | Apply a launch to the nearest golf ball, using horizontal speed along your facing direction and a vertical speed. Components are limited to ±8 blocks per tick. |
| `/golf launch <forward> <up> <id>` | Apply a launch to a golf ball by entity ID. |
| `/golf inspect` | Inspect the nearest golf ball. |
| `/golf inspect <id>` | Inspect a golf ball by entity ID. |
| `/golf dev preparehole` | Prepare the configured development hole. |
| `/golf dev preparecourse` | Generate the development course layout in its approved development world. |
| `/golf dev testhole <1-3>` | Select one of the generated development test holes. |

## Course builder commands

Course authoring commands require gamemaster permission. They define golf metadata on
existing Minecraft terrain; they do not generate or terraform a course. To build a new
course, run `/golf course create <id>`, stand at two opposite perimeter corners and run
`/golf course landscape bounds` at each, then enter `/golf mode build` and select it
with `/golf builder course <id>`. Drafts with a perimeter are buildable before any
hole is defined; only finalized courses can be played. Hole metadata can be authored
in the draft with `/golf course edit <id>` and the hole commands below.

### Create, clone, and select courses

| Command | What it does |
|---|---|
| `/golf course create <id>` | Create a course draft using the ID as its display name. |
| `/golf course create <id> <display name>` | Create a course draft with a separate display name. |
| `/golf course clone <sourceId> <newId>` | Clone a course into a separate draft. |
| `/golf course clone <sourceId> <newId> <display name>` | Clone a course and assign the draft a display name. |
| `/golf course list` | List authored courses. |
| `/golf course status <id>` | Show a draft or finalized course and its hole metadata. |
| `/golf course edit <id>` | Select a draft as the current course to edit. |
| `/golf course select <id>` | Select a finalized course for play and as the runtime active course. |
| `/golf course finalize <id>` | Validate and publish a draft as a finalized course. |
| `/golf course delete <id>` | Delete an authored course. |

### Define holes in the current draft

Stand at the position to record, then run the relevant command. Hole numbering starts
at 1.

| Command | What it does |
|---|---|
| `/golf hole tee <hole>` | Record your position as the hole's tee. |
| `/golf hole cup <hole>` | Place the cup at your position. |
| `/golf hole par <hole> <par>` | Set the hole's par. |
| `/golf hole bounds <hole>` | Capture one corner of the hole boundary; run it again at the opposite corner to complete the rectangular boundary. |

Hole bounds are optional. A hole without bounds does not use an out-of-bounds boundary.
Use `/golf course status <id>` to review the draft, then finalize it when it is ready.

### Protect a course landscape

Landscape commands apply to the current draft, or otherwise to the selected course. The
optional course ID lets an operator specify a course directly, including from the
server console for commands that do not require standing at a position.

| Command | What it does |
|---|---|
| `/golf course landscape bounds` | Record the first perimeter corner at your position; run again at the opposite corner to save the course landscape perimeter. |
| `/golf course landscape status [id]` | Show the perimeter, dimension, course state, and lock status. |
| `/golf course landscape lock [id]` | Lock an existing perimeter so it protects against operators as well as other players. |
| `/golf course landscape unlock [id]` | Unlock an existing perimeter; non-operators remain blocked from modifying protected terrain. |
| `/golf course landscape clear [id]` | Remove the course landscape perimeter and its lock. |

An unlocked perimeter protects the course from non-operators. A locked perimeter also
blocks operators. Protection covers block breaking, block placement, and TNT blasts.

## Notes

- Commands that change a golfer's round state require the player to be eligible for that
  action. For example, a coordinator starts their lobby, and `/golf nexthole` requires
  the current hole to be complete.
- Bedrock and unmodified-Java visitors cannot play golf, even though the server exposes
  visitor view commands.
- This reference lists registered `/golf` commands. Minecraft's built-in commands,
  such as `/op`, are outside its scope.
