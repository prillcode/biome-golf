# M8 — V1 Course and Hole Authoring: Closeout Record

Status: **Complete**.

M8 delivered a bounded, server-authoritative authoring workflow for defining and
playing additional Minecraft-native courses on existing terrain. The existing M5
three-hole course remains the regression fixture.

## Delivered

| Area | Result |
|---|---|
| Domain store | Authored courses support 1..N holes, draft editing, validation, finalization, normalized ids, and safe deletion. |
| Persistence | Authored course drafts and finalized definitions persist in each world's `data/minecraft_golf_authored_courses.json`; malformed or incompatible data fails closed. |
| Operator workflow | Operators can create, list, inspect, edit, finalize, delete, and select authored courses with `/golf course ...`. Hole tee, cup, par, and bounds metadata are captured with `/golf hole ...`. |
| Bounds | Two captured X/Z corners create an axis-aligned playable boundary spanning the world's full build height. Bounds are optional; omitted authored holes use an unbounded playable region. Explicit bounds validate tee, cup, and transition positions before finalization. |
| Runtime safety | Course selection and deletion are refused during active play. Selection is intentionally runtime-only; no course is active after restart. |
| Equipment recovery | `/golf clubs equip` resets the complete seven-club set into hotbar slots 1-7, removes duplicate club stacks, and preserves displaced non-club items when inventory space allows. Hole start retains the same automatic missing-club grant. |

## Manual Acceptance

The user confirmed all required M8 checks on the Docker development server:

- created and authored `testhole1` with one diagonal shoreline hole;
- corrected an invalid boundary and finalized the course successfully;
- selected and played the authored hole end-to-end, including cup completion and scoring;
- confirmed invalid course definitions are rejected with clear validation feedback;
- restarted the server and confirmed the authored course remained in the course list;
- explicitly selected the authored `m5` regression course and confirmed it still
  plays correctly; the course was not selected implicitly after restart.

## Verification

- `./gradlew test` — passed, 228 tests before the equipment command addition;
- `./gradlew build` — passed;
- Docker server loaded the rebuilt JAR and reached `Done`;
- startup registered authored course persistence and course-authoring commands;
- authored course JSON survived a full Docker container restart.

## Decisions and Deferred Work

- Course selection is runtime-only; persisted course definitions are not automatically
  selected after restart.
- Bounds are axis-aligned rather than rotated to the hole's direction.
- Multiple tee boxes per hole, including per-player tee selection, remain deferred.
- A future `/golf clubs add` command for newly introduced clubs remains deferred.
- M9 remains deferred for future alternate game modes.
