# Minecraft Golf Course Creation Steps

This is the basic workflow for creating a course on existing Minecraft terrain.
The authoring commands require server operator permissions.

## 1. Build the Terrain

Build the fairways, rough, hazards, greens, and other course scenery in Minecraft
first. The golf mod does not generate or reshape authored terrain.

Choose the order in which players should play the holes. Hole numbers determine
play order, even if you author the physical holes in a different order.

## 2. Create the Course

Create a course with a lowercase, normalized ID and a display name:

```text
/golf course create sonscourse "Son's Shoreline Course"
```

The display name is optional. The command also makes the new course your current
draft. To switch back to an existing draft:

```text
/golf course edit sonscourse
```

To make a safe editable variant of an existing course, clone it with a new ID:

```text
/golf course clone sonscourse sonscourse-v2 "Son's Shoreline Course v2"
```

The source course is unchanged. The clone becomes your current draft and can be
modified, extended with additional holes, and finalized independently.

List available courses at any time:

```text
/golf course list
```

## 3. Define Each Hole

Stand on the desired tee location and capture it. The tee position is based on
the block where the player is standing:

```text
/golf hole tee 1
```

Stand at the desired cup/pin location and capture it:

```text
/golf hole cup 1
```

The command places the golf cup block automatically when the block position is
replaceable. If it cannot place the block, place `minecraft_golf:golf_cup`
manually at the desired location.

Set the hole's par:

```text
/golf hole par 1 4
```

## 4. Capture the Playable Bounds (Optional)

Bounds are optional. If omitted, the finalized hole is unbounded for out-of-bounds
purposes. To add a boundary, use an axis-aligned X/Z rectangle. It is not rotated
to follow the direction of a diagonal hole. Use F3 to watch coordinates while
choosing corners, and press **F3+G** to show chunk gridlines as a visual placement
reference.

Stand at one corner outside the entire hole and run:

```text
/golf hole bounds 1
```

Move to the opposite corner, making sure the rectangle contains the tee, cup,
fairway, hazards, and reasonable shot margins, then run the same command again:

```text
/golf hole bounds 1
```

The Y range automatically expands to the world's full build height. Remember that
Minecraft's Z axis increases toward the south, so use the actual F3 coordinates
rather than relying on compass directions.

## 5. Inspect the Draft

Check that every required field is present:

```text
/golf course status sonscourse
```

Each hole must have:

- tee
- cup
- positive par
- optional playable bounds containing the tee and cup

If the course has multiple holes, define them as consecutive numbers starting at
1. For example:

```text
/golf hole tee 2
/golf hole cup 2
/golf hole par 2 3
/golf hole bounds 2
```

Run the bounds command twice for any hole that should have an explicit boundary.

## 6. Finalize the Course

When all holes are complete and the status output looks correct:

```text
/golf course finalize sonscourse
```

Finalization validates the complete course and makes it playable. Invalid courses
are rejected with an explanation; fix the draft and try again.

## 7. Select and Play-Test

Select the finalized course:

```text
/golf course select sonscourse
```

Start the first hole:

```text
/golf hole start
```

Play through the course using the normal hole and round commands. The course
definition is saved in the world at:

```text
world/data/minecraft_golf_authored_courses.json
```

Course definitions survive server restarts. The active course selection does not;
after a restart, select the authored course again.

## Useful Commands

```text
/golf course list
/golf course status <id>
/golf course select <id>
/golf course delete <id>
/golf clubs equip
```

Course deletion is refused while a round or hole attempt is active. Complete or
abandon active play first.

## Not Available Yet

These planned commands are not implemented yet:

```text
/golf course rename <id> <new display name>
```
