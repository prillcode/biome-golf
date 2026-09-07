# M3 — Three-Click Swing and HUD: In-Progress Notes

Branch: `m3-three-click-swing` (base `main` @ the validated M2 merge).

## Scope (MILESTONES.md M3, PRD §5–7)
Skill-based three-click shot interaction + a lightweight HUD + a simple post-shot
ball-follow camera. Power/accuracy influence the shot; server validates and owns
the outcome. NOT in scope: cinematic camera, landing prediction, wind, scoring,
advanced lies.

## Design decisions
- **D010 / input model**: three consecutive right-clicks against moving meters.
  1st right-click starts the swing (power bar sweeps); 2nd locks power (0..1);
  3rd locks accuracy (a window → lateral deviation). Server resolves from camera
  aim + equipped club + power + accuracy.
- **D009 contract**: power/accuracy thread into the existing pure
  `ShotResolver` + `GolfBallEntity.launch` path used by M2 — no new transport
  shape beyond the payload that finally carries the meters.
- Meter timing is client-local; only the finalized shot intent (power/accuracy +
  aim) is sent to the server, which re-validates and launches. Server auth.
- HUD & swing UI are client-only; no `net.minecraft.client` import may reach
  `src/main`.

## Planned increments (build order)
- **Inc1 (pure, headless):** extend/confirm pure resolver for partial power
  (powerCurve 0..1 -> fraction of fullPowerSpeed) and accuracy dispersion
  (0..1 timing -> lateral angle deviation); deterministic, unit-tested
  (determinism, partial-power monotonic, perfect vs poor accuracy spread).
- **Inc2 (server networking + validation):** typed ShotRequestPayload
  (ballId, aimYaw, aimPitch, power 0..1, accuracy 0..1); register via
  fabric-networking-api-v1 (PayloadTypeRegistry.serverboundPlay +
  ServerPlayNetworking.registerGlobalReceiver); server validates ownership,
  stationary ball, equipped club, legal ranges, then resolves+launches. Fully
  server-side, dedicated-server safe.
- **Inc3 (client swing state machine + HUD + input):** client controller that
  captures the three right-clicks against local meters, draws a lightweight HUD
  (club, nominal distance, power bar, accuracy window, shot state) client-only,
  and sends the finalized ShotRequestPayload. Client-only; .gitignore-split.
- **Inc4 (post-shot ball-follow camera + verify):** simple follow camera; full
  verification (client leak scan, Docker boot, manual three-click feel +
  partial-power + good/poor timing).

## Verification notes
- Headless: `./gradlew test` — Inc1 carries the deterministic shot calculations.
- Dedicated server: Inc2 boots the mod, payload receiver registered, no client
  leak; live rcon can exercise validate/reject via unit-less server path.
- Human/feel (runClient): three-click consistency, partial power, accuracy
  deviation on good vs poor timing, meter responsiveness, follow camera.
