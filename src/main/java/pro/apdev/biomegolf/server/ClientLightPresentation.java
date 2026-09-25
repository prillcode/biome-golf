package pro.apdev.biomegolf.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import pro.apdev.biomegolf.hole.ClientLightHudText;
import pro.apdev.biomegolf.net.HoleStatePayload;
import pro.apdev.biomegolf.net.RoundScorecardPayload;
import pro.apdev.biomegolf.net.RoundScorecardText;

/**
 * M10.3 S3: vanilla presentation fallbacks for client-light players (vanilla Java and
 * Bedrock through Geyser). Everything here is gated on the single client-light
 * discriminator — {@code !ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE)} —
 * so the modded HUD, payloads, ball renderer, and camera are untouched.
 *
 * <p>Mirrors authoritative server state into naturally per-player vanilla channels: the
 * action bar for hole/par/strokes/distance, and chat for the final scorecard. Clients that
 * can receive the payloads never reach these paths.</p>
 */
public final class ClientLightPresentation {

	private ClientLightPresentation() {
	}

	/** True only for clients that cannot receive the modded hole-state payload. */
	public static boolean isClientLight(ServerPlayer player) {
		return BallCameraService.isClientLight(player);
	}

	/** Emits the per-player action bar for an authoritative snapshot, when client-light. */
	public static void onSnapshot(ServerPlayer player, HoleStatePayload payload) {
		if (!isClientLight(player)) {
			return;
		}
		if (VisitorService.isVisitor(player)) {
			// Visitors are spectators who cannot play; the visitor reminder owns the action
			// bar, so hole-status fallbacks are suppressed.
			return;
		}
		String text = switch (payload.phase()) {
			case ACTIVE -> ClientLightHudText.active(payload.holeNumber(), payload.par(),
				payload.strokes(), payload.strokeLimit(), payload.distanceToCupBlocks(),
				payload.shotDistanceBlocks());
			case MISSING_BALL -> ClientLightHudText.missingBall();
			case COMPLETE, ROUND_COMPLETE -> ClientLightHudText.complete(payload.holeNumber(),
				payload.par(), payload.strokes(), payload.scoreToPar(), termLabel(payload),
				payload.courseStrokes(), payload.courseParPlayed(), payload.courseTotalPar());
			case PRACTICE -> ClientLightHudText.practice(payload.holeNumber(), payload.par(),
				payload.shotDistanceBlocks());
		};
		player.sendSystemMessage(Component.literal(text), true);
	}

	/** Chat fallback for the final scorecard where the payload would be dropped. */
	public static void sendScorecard(ServerPlayer player, RoundScorecardPayload payload) {
		if (!isClientLight(player)) {
			return;
		}
		for (String line : RoundScorecardText.lines(payload)) {
			player.sendSystemMessage(Component.literal(line));
		}
	}

	private static String termLabel(HoleStatePayload payload) {
		return payload.scoreTerm() == null ? null : payload.scoreTerm().name().replace('_', ' ');
	}
}
