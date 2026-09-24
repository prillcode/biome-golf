package com.prillcode.minecraftgolf.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.HeldShotRules;
import com.prillcode.minecraftgolf.club.ShotType;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.ShotOutcome;
import com.prillcode.minecraftgolf.net.ShotService;

/**
 * M10.1 S1b / M10.3 G1: tap-based shot input for client-light players.
 *
 * <p>A player without the mod taps use near their own resting ball. Each tap advances a
 * visible power step (boss bar); the shot fires shortly after the last tap, or immediately at
 * the maximum step. This intentionally does not depend on a use/release pair: a 2026-09
 * playtest showed Geyser never delivered a release for a club (an item with no use duration),
 * so the earlier hold-based model always auto-fired at full power after the safety cap.</p>
 *
 * <p>Aim is the player's look direction and shot type stays {@code STANDARD}. Modded clients
 * keep the three-click meter and are ignored here (detected via
 * {@link BallCameraService#isClientLight}). Everything remains server-authoritative: the
 * client only produces taps, never an outcome; {@code /golf swing} remains the precise floor.</p>
 */
public final class HeldShotService {

	private static final double MAX_STRIKE_DISTANCE_SQ = 6.0 * 6.0;

	/** Ticks after a shot during which new taps are refused. */
	private static final int FIRE_COOLDOWN_TICKS = 8;

	private static final Map<UUID, Meter> METERS = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
	private static final Set<UUID> HINTED = new HashSet<>();

	/** M10.3 S3: per-player boss bar showing the tap-meter power step. */
	private static final Map<UUID, ServerBossEvent> METER_BARS = new HashMap<>();
	private static final Component METER_BAR_TITLE = Component.literal("Shot power");

	private static boolean registered;

	/** One in-progress tap meter for a player. */
	private record Meter(int ballId, int taps, int lastTapTick) {
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
			"Registered M10.1/G1 tap-meter shot input (client-light players; /golf swing remains the command floor)");
	}

	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		return tap(player, level, hand);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
			BlockHitResult hitResult) {
		return tap(player, level, hand);
	}

	private static InteractionResult tap(Player player, Level level, InteractionHand hand) {
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
		GolfBallEntity ball = eligibleBall(serverPlayer);
		if (ball == null) {
			return InteractionResult.PASS;
		}

		UUID playerId = serverPlayer.getUUID();
		Meter current = METERS.get(playerId);
		int gap = current == null ? -1 : tick - current.lastTapTick();
		if (gap >= 0 && HeldShotRules.isRepeatWithinGap(gap)) {
			// Geyser may repeat the use action while a button is held; count it once.
			MinecraftGolf.LOGGER.debug("{} use action ignored (repeat after {} ticks)",
				serverPlayer.getName().getString(), gap);
			return InteractionResult.SUCCESS;
		}
		int taps;
		if (current == null || current.ballId() != ball.getId()
				|| HeldShotRules.isNewMeter(current.taps(), gap)) {
			taps = 1;
		} else {
			taps = Math.min(current.taps() + 1, HeldShotRules.MAX_TAPS);
		}
		METERS.put(playerId, new Meter(ball.getId(), taps, tick));
		showMeterBar(serverPlayer, taps);
		MinecraftGolf.LOGGER.info("{} tapped shot power step {}/{} on ball {} (gap {})",
			serverPlayer.getName().getString(), taps, HeldShotRules.MAX_TAPS, ball.getId(),
			gap < 0 ? "new" : Integer.toString(gap));
		sendHintOnce(serverPlayer);
		return InteractionResult.SUCCESS;
	}

	/** Nearest ball this client-light player may tap right now, or {@code null}. */
	private static GolfBallEntity eligibleBall(ServerPlayer player) {
		GolfBallEntity ball = ShotService.nearestStrikeableBall(player);
		if (ball == null || !ball.isResting()) {
			return null;
		}
		if (ball.owner() != null && !ball.owner().equals(player.getUUID())) {
			return null;
		}
		if (player.distanceToSqr(ball) > MAX_STRIKE_DISTANCE_SQ) {
			return null;
		}
		ActiveHoleService.ShotPermission permission =
			ActiveHoleService.instance().shotPermission(player, ball);
		if (permission == ActiveHoleService.ShotPermission.WRONG_BALL
				|| permission == ActiveHoleService.ShotPermission.MISSING_BALL) {
			return null;
		}
		return ball;
	}

	private static void onServerTick(MinecraftServer server) {
		if (METERS.isEmpty()) {
			return;
		}
		int tick = server.getTickCount();
		Iterator<Map.Entry<UUID, Meter>> iterator = METERS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Meter> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				hideMeterBar(entry.getKey());
				iterator.remove();
				continue;
			}
			Meter meter = entry.getValue();
			int sinceTap = tick - meter.lastTapTick();
			GolfBallEntity current = eligibleBall(player);
			boolean stillValid = current != null && current.getId() == meter.ballId();
			if (!stillValid) {
				hideMeterBar(player.getUUID());
				iterator.remove();
				continue;
			}
			if (!HeldShotRules.shouldFire(meter.taps(), sinceTap)) {
				continue;
			}
			iterator.remove();
			hideMeterBar(player.getUUID());
			fire(player, current, meter.taps());
		}
	}

	/** Creates or updates the tap-meter boss bar for a client-light player. */
	private static void showMeterBar(ServerPlayer player, int taps) {
		ServerBossEvent bar = METER_BARS.get(player.getUUID());
		if (bar == null) {
			bar = new ServerBossEvent(java.util.UUID.randomUUID(), METER_BAR_TITLE,
				BossBarColor.GREEN, BossBarOverlay.PROGRESS);
			bar.addPlayer(player);
			METER_BARS.put(player.getUUID(), bar);
		}
		float power = HeldShotRules.powerForTaps(taps);
		bar.setProgress(power);
		bar.setName(METER_BAR_TITLE.copy().append(" " + Math.round(power * 100) + "% ("
			+ taps + "/" + HeldShotRules.MAX_TAPS + ")"));
	}

	/** Hides and forgets a player's meter boss bar, if any. */
	private static void hideMeterBar(UUID playerId) {
		ServerBossEvent bar = METER_BARS.remove(playerId);
		if (bar != null) {
			bar.removeAllPlayers();
		}
	}

	/** One-time discoverability hint; the boss bar then carries the live power step. */
	private static void sendHintOnce(ServerPlayer player) {
		if (!HINTED.add(player.getUUID())) {
			return;
		}
		player.sendSystemMessage(Component.literal(
			"[golf] Tap once for a light shot; tap up to " + HeldShotRules.MAX_TAPS
				+ " times for full power. Precise: /golf swing <power>"));
	}

	/** Forgets all transient tap state for a disconnecting player. */
	public static void forget(UUID playerId) {
		METERS.remove(playerId);
		COOLDOWNS.remove(playerId);
		HINTED.remove(playerId);
		hideMeterBar(playerId);
	}

	private static void fire(ServerPlayer player, GolfBallEntity ball, int taps) {
		float power = HeldShotRules.powerForTaps(taps);
		ShotOutcome outcome = ShotService.attempt(player, ball.getId(),
			player.getYRot(), player.getXRot(), power, HeldShotRules.HELD_ACCURACY, ShotType.STANDARD);
		if (outcome != ShotOutcome.SUCCESS) {
			player.sendSystemMessage(Component.literal(outcome.description()), true);
		} else {
			MinecraftGolf.LOGGER.info("{} tap-meter shot power={} ({} taps)",
				player.getName().getString(), power, taps);
		}
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			COOLDOWNS.put(player.getUUID(), server.getTickCount() + FIRE_COOLDOWN_TICKS);
		}
	}
}
