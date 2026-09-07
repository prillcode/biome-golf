package com.prillcode.minecraftgolf.net;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.ball.PhysicsConfig;
import com.prillcode.minecraftgolf.club.ClubDefinition;
import com.prillcode.minecraftgolf.club.ShotResolver;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.item.GolfClubItem;

/**
 * Server-side authoritative shot execution (M3, ARCH §8.2/§16): validates a
 * finalized shot request against the acting {@link ServerPlayer} and, if legal,
 * resolves and launches the ball through the equipped club + power/accuracy.
 * Shared by the networking receiver so there is exactly one place a shot can be
 * committed — a repeatable input yields a repeatable launch (no per-client
 * outcome control).
 */
public final class ShotService {

	private ShotService() {
	}

	/**
	 * Attempts a shot by {@code player} on the ball {@code ballId}.
	 *
	 * @return the {@link ShotOutcome}; {@code SUCCESS} when a launch occurred
	 */
	public static ShotOutcome attempt(ServerPlayer player, int ballId,
			float aimYawDeg, float aimPitchDeg, float power, float accuracy) {
		ServerLevel level = (ServerLevel) player.level();
		GolfBallEntity ball = findBall(level, ballId);
		if (ball == null) {
			return ShotOutcome.BALL_NOT_FOUND;
		}

		UUID me = player.getUUID();
		if (ball.owner() != null && !ball.owner().equals(me)) {
			return ShotOutcome.NOT_YOUR_BALL;
		}
		if (!ball.isResting()) {
			return ShotOutcome.BALL_MOVING;
		}

		ClubDefinition club = heldClub(player);
		if (club == null) {
			return ShotOutcome.NO_CLUB;
		}

		double maxSpeed = PhysicsConfig.DEFAULT.maxLaunchSpeed();
		// Defensive clamps happen inside the resolver; power/accuracy sent here may
		// already be client-clamped but must never be trusted raw.
		double p = ShotResolver.legalPower(power);
		double a = ShotResolver.legalAccuracy(accuracy);
		Vec3 velocity = ShotResolver.initialVelocity(club, aimYawDeg, aimPitchDeg, p, a, maxSpeed);
		if (velocity == null) {
			return ShotOutcome.AIM_NOT_LEGAL;
		}

		// Claim on first strike; launch is the single server-authoritative mutation.
		if (ball.owner() == null) {
			ball.setOwner(me);
		}
		ball.launch(velocity);
		MinecraftGolf.LOGGER.info("{} shot via {} power={} acc={} (wind-free) launched v={}",
				player.getName().getString(), club.id(), p, a, velocity);
		return ShotOutcome.SUCCESS;
	}

	private static GolfBallEntity findBall(ServerLevel level, int id) {
		List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		for (GolfBallEntity ball : balls) {
			if (ball.getId() == id) {
				return ball;
			}
		}
		return null;
	}

	private static ClubDefinition heldClub(Player player) {
		ItemStack held = player.getMainHandItem();
		if (held.getItem() instanceof GolfClubItem clubItem) {
			return clubItem.club();
		}
		return null;
	}
}
