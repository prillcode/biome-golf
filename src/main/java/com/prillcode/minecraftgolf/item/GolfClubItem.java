package com.prillcode.minecraftgolf.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.ClubDefinition;
import com.prillcode.minecraftgolf.club.ShotResolver;
import com.prillcode.minecraftgolf.ball.PhysicsConfig;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;

/**
 * A registered Minecraft item representing one golf club (ARCHITECTURE.md §12).
 * <p>
 * Two independent behaviors, deliberately separated (ARCH §12 "Club Items as
 * Weapons" — swinging as a weapon must never mutate golf/trigger a shot):
 * <ul>
 *   <li><b>Right-click {@code use()}</b> initiates a golf shot at full power
 *       toward the player's camera aim. The server resolves the launch from the
 *       club's {@link ClubDefinition} via {@link ShotResolver} and strikes the
 *       player's own (or an unowned) stationary ball; ownership is claimed on
 *       first hit. This is the M2 "client sends intent ({@code use}), server
 *       owns the outcome" path.</li>
 *   <li><b>Melee attach</b> deals a small flat club {@code meleeDamage} bonus and
 *       cannot start a golf shot (right-click only does).</li>
 * </ul>
 */
public class GolfClubItem extends Item {

	/** How close a player must be to a stationary ball to strike it. */
	private static final double MAX_STRIKE_DISTANCE_SQ = 6.0 * 6.0;

	private final ClubDefinition club;

	public GolfClubItem(Properties properties, ClubDefinition club) {
		super(properties);
		this.club = club;
	}

	public ClubDefinition club() {
		return club;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			// Let the server decide; return SUCCESS so the swing animation plays
			// but do not mutate any server-authoritative state here.
			return InteractionResult.SUCCESS;
		}
		ServerLevel server = (ServerLevel) level;
		net.minecraft.server.level.ServerPlayer sp = (net.minecraft.server.level.ServerPlayer) player;

		GolfBallEntity ball = findStrikeableBall(server, player);
		if (ball == null) {
			sp.sendSystemMessage(Component.literal(
					"[golf] no golf ball in range (6 blocks) for your " + club.displayName()), true);
			return InteractionResult.FAIL;
		}

		// Claim on first strike by a new owner (findStrikeableBall already excluded
		// balls owned by another player, and confirmed this ball is resting).
		if (ball.owner() == null) {
			ball.setOwner(player.getUUID());
		}

		Vec3 velocity = ShotResolver.initialVelocity(club,
				player.getYRot(), player.getXRot(), PhysicsConfig.DEFAULT.maxLaunchSpeed());
		if (velocity == null) {
			sp.sendSystemMessage(Component.literal("[golf] aim higher to take a shot"), true);
			return InteractionResult.FAIL;
		}

		ball.launch(velocity);
		MinecraftGolf.LOGGER.info("{} hit a {} shot (vel {}) toward yaw {}",
				player.getName().getString(), club.id(), velocity, (int) player.getYRot());
		return InteractionResult.SUCCESS;
	}

	@Override
	public float getAttackDamageBonus(net.minecraft.world.entity.Entity target, float base,
			net.minecraft.world.damagesource.DamageSource source) {
		// Turn the club into a simple melee weapon. This path never launches a ball.
		return (float) club.meleeDamage();
	}

	private static GolfBallEntity findStrikeableBall(ServerLevel server, Player player) {
		java.util.UUID me = player.getUUID();
		double px = player.getX();
		double py = player.getY();
		double pz = player.getZ();

		java.util.List<? extends GolfBallEntity> balls =
				server.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);

		GolfBallEntity mine = null;
		GolfBallEntity unowned = null;
		double mineD = Double.MAX_VALUE;
		double unD = Double.MAX_VALUE;
		for (GolfBallEntity ball : balls) {
			if (!ball.isResting() || !ball.canBeStruckBy(me)) {
				continue;
			}
			double d = ball.distanceToSqr(px, py, pz);
			if (d > MAX_STRIKE_DISTANCE_SQ) {
				continue;
			}
			if (ball.owner() != null && ball.owner().equals(me)) {
				if (d < mineD) {
					mineD = d;
					mine = ball;
				}
			} else if (mine == null) {
				// unowned candidate kept only until a personal ball is found
				if (d < unD) {
					unD = d;
					unowned = ball;
				}
			}
		}
		return mine != null ? mine : unowned;
	}
}
