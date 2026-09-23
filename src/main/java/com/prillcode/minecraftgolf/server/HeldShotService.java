package com.prillcode.minecraftgolf.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.BlockHitResult;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.ClubDefinition;
import com.prillcode.minecraftgolf.club.HeldShotRules;
import com.prillcode.minecraftgolf.club.ShotType;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.ShotOutcome;
import com.prillcode.minecraftgolf.net.ShotService;

/**
 * M10.1 S1b: held-use shot input for client-light players.
 *
 * <p>A player without the mod holds right-click near their own resting ball. The
 * server enters the vanilla "using item" state (so the client sends a
 * {@code RELEASE_USE_ITEM} packet on release — Geyser does the same for Bedrock),
 * times the hold, and on release launches the ball through the unchanged
 * {@link ShotService} with power derived from {@link HeldShotRules}. Aim is the
 * player's look direction; shot type stays {@code STANDARD}.</p>
 *
 * <p>Modded clients keep the three-click meter and are ignored here (detected via
 * {@link BallCameraService#isClientLight}). Everything remains server-authoritative:
 * the client only produces press/release, never an outcome.</p>
 */
public final class HeldShotService {

	private static final double MAX_STRIKE_DISTANCE_SQ = 6.0 * 6.0;

	/** Ticks after a shot during which a new charge is refused. */
	private static final int FIRE_COOLDOWN_TICKS = 8;

	private static final Map<UUID, Charge> CHARGES = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
	private static boolean registered;

	private record Charge(int ballId, int startTick) {
	}

	private HeldShotService() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		UseItemCallback.EVENT.register(HeldShotService::onUseItem);
		UseBlockCallback.EVENT.register(HeldShotService::onUseBlock);
		ServerTickEvents.END_SERVER_TICK.register(HeldShotService::onServerTick);
		MinecraftGolf.LOGGER.info(
			"Registered M10.1 held-use shot input (client-light players; /golf swing remains the command floor)");
	}

	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		return tryStartCharge(player, level, hand);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
			BlockHitResult hitResult) {
		return tryStartCharge(player, level, hand);
	}

	private static InteractionResult tryStartCharge(Player player, Level level, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		if (hand != InteractionHand.MAIN_HAND || serverPlayer.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!BallCameraService.isClientLight(serverPlayer)) {
			// Modded clients drive the three-click meter themselves.
			return InteractionResult.PASS;
		}
		if (GolfItems.clubOf(serverPlayer.getMainHandItem()) == null) {
			return InteractionResult.PASS;
		}
		MinecraftServer server = serverPlayer.level().getServer();
		if (server == null) {
			return InteractionResult.PASS;
		}
		int tick = server.getTickCount();
		Integer cooldownUntil = COOLDOWNS.get(serverPlayer.getUUID());
		if (cooldownUntil != null) {
			if (tick < cooldownUntil) {
				return InteractionResult.PASS;
			}
			COOLDOWNS.remove(serverPlayer.getUUID());
		}
		if (CHARGES.containsKey(serverPlayer.getUUID())) {
			return InteractionResult.SUCCESS;
		}
		GolfBallEntity ball = ShotService.nearestStrikeableBall(serverPlayer);
		if (ball == null || !ball.isResting()) {
			return InteractionResult.PASS;
		}
		if (ball.owner() != null && !ball.owner().equals(serverPlayer.getUUID())) {
			return InteractionResult.PASS;
		}
		if (serverPlayer.distanceToSqr(ball) > MAX_STRIKE_DISTANCE_SQ) {
			return InteractionResult.PASS;
		}
		ActiveHoleService.ShotPermission permission =
			ActiveHoleService.instance().shotPermission(serverPlayer, ball);
		if (permission == ActiveHoleService.ShotPermission.WRONG_BALL
				|| permission == ActiveHoleService.ShotPermission.MISSING_BALL) {
			return InteractionResult.PASS;
		}
		serverPlayer.startUsingItem(hand);
		CHARGES.put(serverPlayer.getUUID(), new Charge(ball.getId(), tick));
		return InteractionResult.SUCCESS;
	}

	private static void onServerTick(MinecraftServer server) {
		if (CHARGES.isEmpty()) {
			return;
		}
		int tick = server.getTickCount();
		Iterator<Map.Entry<UUID, Charge>> iterator = CHARGES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Charge> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				iterator.remove();
				continue;
			}
			Charge charge = entry.getValue();
			int heldTicks = tick - charge.startTick;
			GolfBallEntity ball = findBall(player, charge.ballId);
			boolean stillValid = ball != null && ball.isResting()
				&& (ball.owner() == null || ball.owner().equals(player.getUUID()))
				&& player.distanceToSqr(ball) <= MAX_STRIKE_DISTANCE_SQ
				&& GolfItems.clubOf(player.getMainHandItem()) != null;
			if (!stillValid) {
				player.stopUsingItem();
				iterator.remove();
				continue;
			}
			boolean released = !player.isUsingItem();
			boolean autoFire = HeldShotRules.shouldAutoFire(heldTicks);
			if (!released && !autoFire) {
				continue;
			}
			iterator.remove();
			if (autoFire) {
				player.stopUsingItem();
			}
			fire(player, ball, heldTicks);
		}
	}

	private static void fire(ServerPlayer player, GolfBallEntity ball, int heldTicks) {
		float power = HeldShotRules.power(heldTicks);
		ShotOutcome outcome = ShotService.attempt(player, ball.getId(),
			player.getYRot(), player.getXRot(), power, HeldShotRules.HELD_ACCURACY, ShotType.STANDARD);
		if (outcome != ShotOutcome.SUCCESS) {
			player.sendSystemMessage(Component.literal(outcome.description()), true);
		} else {
			MinecraftGolf.LOGGER.info("{} held-use shot power={} after {} ticks",
				player.getName().getString(), power, heldTicks);
		}
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			COOLDOWNS.put(player.getUUID(), server.getTickCount() + FIRE_COOLDOWN_TICKS);
		}
	}

	private static GolfBallEntity findBall(ServerPlayer player, int id) {
		ServerLevel level = (ServerLevel) player.level();
		for (GolfBallEntity ball : level.getEntities(
				EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true)) {
			if (ball.getId() == id) {
				return ball;
			}
		}
		return null;
	}
}
