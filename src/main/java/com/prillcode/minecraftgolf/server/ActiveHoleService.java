package com.prillcode.minecraftgolf.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.CupDetector;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PenaltyType;
import com.prillcode.minecraftgolf.hole.PlayerHoleSession;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;

/** Server-authoritative lifecycle for the one configured M4 hole. */
public final class ActiveHoleService {

	public enum ShotPermission {
		PRACTICE,
		SCORING,
		WRONG_BALL,
		HOLE_COMPLETE
	}

	private static final ActiveHoleService INSTANCE = new ActiveHoleService();

	private final Map<UUID, PlayerHoleSession> sessions = new HashMap<>();
	private final Set<UUID> overspeedCupEntries = new HashSet<>();
	private HoleDefinition hole;

	private ActiveHoleService() {
	}

	public static ActiveHoleService instance() {
		return INSTANCE;
	}

	public void initialize(HoleDefinition configuredHole) {
		hole = Objects.requireNonNull(configuredHole, "configuredHole");
		sessions.clear();
		overspeedCupEntries.clear();
		MinecraftGolf.LOGGER.info("Loaded hole {} (#{} par {}, Double Par + 2 limit {}) in {}",
			hole.id(), hole.number(), hole.par(), hole.strokeLimit(), hole.dimension());
	}

	public HoleDefinition configuredHole() {
		if (hole == null) {
			throw new IllegalStateException("active-hole service has not been initialized");
		}
		return hole;
	}

	public StartResult start(ServerPlayer player) {
		HoleDefinition definition = configuredHole();
		ServerLevel level = player.level();
		String currentDimension = level.dimension().identifier().toString();
		if (!definition.dimension().equals(currentDimension)) {
			return new StartResult(false, "[golf] hole " + definition.number() + " is in "
				+ definition.dimension() + "; you are in " + currentDimension);
		}

		removePreviousBall(level, player.getUUID());
		BlockPos cupBlockPos = BlockPos.containing(
			definition.cup().x(), definition.cup().y() - GolfBallEntity.BALL_RADIUS, definition.cup().z());
		level.setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());

		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(level, EntitySpawnReason.COMMAND);
		if (ball == null) {
			return new StartResult(false, "[golf] failed to create the tee ball");
		}
		ball.setOwner(player.getUUID());
		ball.placeAtRest(definition.tee());
		if (!level.addFreshEntity(ball)) {
			return new StartResult(false, "[golf] failed to add the tee ball to the world");
		}

		sessions.put(player.getUUID(), PlayerHoleSession.start(definition, ball.getUUID()));
		player.teleportTo(definition.tee().x(), definition.tee().y() + 1.0, definition.tee().z() + 2.0);
		MinecraftGolf.LOGGER.info("{} started hole {} with ball {} at tee {}",
			player.getName().getString(), definition.id(), ball.getUUID(), definition.tee());
		return new StartResult(true, "[golf] Hole " + definition.number() + " — Par "
			+ definition.par() + " | Double Par + 2 limit " + definition.strokeLimit());
	}

	public Optional<PlayerHoleState> state(UUID playerId) {
		PlayerHoleSession session = sessions.get(playerId);
		return session == null ? Optional.empty() : Optional.of(session.state());
	}

	public String status(UUID playerId) {
		PlayerHoleSession session = sessions.get(playerId);
		if (session == null) {
			HoleDefinition definition = configuredHole();
			return "[golf] no active hole | configured Hole " + definition.number()
				+ " Par " + definition.par();
		}
		PlayerHoleState state = session.state();
		String result = state.isComplete() ? " | COMPLETE: " + state.completionReason() : "";
		String relativeScore = state.strokes() == 0 ? "No strokes yet" : formatToPar(state.scoreToPar());
		return "[golf] Hole " + state.hole().number() + " | strokes " + state.strokes()
			+ "/" + state.hole().strokeLimit() + " | " + relativeScore + result;
	}

	public ShotPermission shotPermission(ServerPlayer player, GolfBallEntity ball) {
		PlayerHoleSession session = sessions.get(player.getUUID());
		if (session == null) {
			return ShotPermission.PRACTICE;
		}
		if (!session.ballUuid().equals(ball.getUUID())) {
			return ShotPermission.WRONG_BALL;
		}
		if (session.state().isComplete()) {
			return ShotPermission.HOLE_COMPLETE;
		}
		return ShotPermission.SCORING;
	}

	/** Called only after ShotService has performed the authoritative launch. */
	public PlayerHoleState recordAcceptedShot(ServerPlayer player, GolfBallEntity ball, Vec3 shotOrigin) {
		PlayerHoleSession session = requiredSession(player.getUUID(), ball.getUUID());
		PlayerHoleSession updatedSession = session.recordAcceptedShot(shotOrigin);
		PlayerHoleState updated = updatedSession.state();
		sessions.put(player.getUUID(), updatedSession);
		player.sendSystemMessage(Component.literal("[golf] Stroke " + updated.strokes() + " of "
			+ updated.hole().strokeLimit() + " | " + formatToPar(updated.scoreToPar())), true);
		if (updated.isComplete()) {
			sendCompletion(player, updated);
		}
		return updated;
	}

	/** Evaluates authoritative movement for physical cup completion. */
	public void onBallMoved(GolfBallEntity ball, Vec3 from, Vec3 to) {
		UUID owner = ball.owner();
		if (owner == null) {
			return;
		}
		PlayerHoleSession session = sessions.get(owner);
		if (session == null || session.state().isComplete() || !session.ballUuid().equals(ball.getUUID())) {
			return;
		}
		if (!session.state().hole().boundary().contains(to)) {
			applyPenalty(ball, owner, session, PenaltyType.OUT_OF_BOUNDS);
			return;
		}
		if (crossesWater((ServerLevel) ball.level(), from, to)) {
			applyPenalty(ball, owner, session, PenaltyType.WATER);
			return;
		}

		Vec3 cup = session.state().hole().cup();
		double speed = ball.ballState() == null ? 0.0 : ball.ballState().velocity().length();
		if (overspeedCupEntries.contains(ball.getUUID())) {
			if (!CupDetector.contains(to, cup)) {
				overspeedCupEntries.remove(ball.getUUID());
			}
			return;
		}
		if (speed > CupDetector.MAX_ENTRY_SPEED && CupDetector.intersects(from, to, cup)) {
			overspeedCupEntries.add(ball.getUUID());
			MinecraftGolf.LOGGER.info("Golf ball {} crossed cup {} too fast at {} blocks/tick",
				ball.getUUID(), session.state().hole().id(), speed);
			return;
		}
		if (!CupDetector.entered(from, to, cup, speed)) {
			return;
		}

		PlayerHoleSession completedSession = session.holeOut();
		PlayerHoleState completed = completedSession.state();
		sessions.put(owner, completedSession);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(completed.hole().cup());
		ball.level().playSound(null, BlockPos.containing(completed.hole().cup().x(),
			completed.hole().cup().y(), completed.hole().cup().z()),
			SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.4F);
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			sendCompletion(player, completed);
		}
		MinecraftGolf.LOGGER.info("Player {} holed out hole {} in {} strokes",
			owner, completed.hole().id(), completed.strokes());
	}

	private void applyPenalty(GolfBallEntity ball, UUID owner, PlayerHoleSession session, PenaltyType penalty) {
		PlayerHoleSession updatedSession = session.applyPenalty(penalty);
		PlayerHoleState updated = updatedSession.state();
		sessions.put(owner, updatedSession);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(session.lastSafePosition());
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			String label = penalty == PenaltyType.WATER ? "Water" : "Out of Bounds";
			player.sendSystemMessage(Component.literal("[golf] " + label + " — one penalty stroke; ball returned"
				+ " | " + updated.strokes() + "/" + updated.hole().strokeLimit()));
			if (updated.isComplete()) {
				sendCompletion(player, updated);
			}
		}
		MinecraftGolf.LOGGER.info("Applied {} penalty to player {} on hole {}; recovered ball {} to {}",
			penalty, owner, updated.hole().id(), ball.getUUID(), session.lastSafePosition());
	}

	private static boolean crossesWater(ServerLevel level, Vec3 from, Vec3 to) {
		double distance = to.subtract(from).length();
		int steps = Math.max(1, (int) Math.ceil(distance / 0.2));
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			double x = from.x() + (to.x() - from.x()) * t;
			double y = from.y() + (to.y() - from.y()) * t;
			double z = from.z() + (to.z() - from.z()) * t;
			if (level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)
					|| level.getFluidState(BlockPos.containing(x, y - GolfBallEntity.BALL_RADIUS, z))
						.is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	public StartResult pickUp(ServerPlayer player) {
		PlayerHoleSession session = sessions.get(player.getUUID());
		if (session == null) {
			return new StartResult(false, "[golf] start the configured hole before picking up");
		}
		if (session.state().isComplete()) {
			return new StartResult(false, "[golf] this hole is already complete");
		}
		PlayerHoleSession updatedSession = session.pickUp();
		PlayerHoleState updated = updatedSession.state();
		sessions.put(player.getUUID(), updatedSession);
		Entity ball = player.level().getEntity(session.ballUuid());
		overspeedCupEntries.remove(session.ballUuid());
		if (ball != null) {
			ball.discard();
		}
		sendCompletion(player, updated);
		return new StartResult(true, "[golf] Pick Up Ball — score recorded as " + updated.strokes());
	}

	private PlayerHoleSession requiredSession(UUID playerId, UUID ballId) {
		PlayerHoleSession session = sessions.get(playerId);
		if (session == null || !session.ballUuid().equals(ballId)) {
			throw new IllegalStateException("accepted scoring shot has no matching active session");
		}
		return session;
	}

	private void removePreviousBall(ServerLevel level, UUID playerId) {
		PlayerHoleSession previous = sessions.remove(playerId);
		if (previous == null) {
			return;
		}
		Entity entity = level.getEntity(previous.ballUuid());
		overspeedCupEntries.remove(previous.ballUuid());
		if (entity != null) {
			entity.discard();
		}
	}

	private static void sendCompletion(ServerPlayer player, PlayerHoleState state) {
		String term = state.strokes() > 0 ? state.scoreTerm().name().replace('_', ' ') : "NO SCORE";
		player.sendSystemMessage(Component.literal("[golf] HOLE COMPLETE — " + state.strokes()
			+ " strokes, " + term + " (" + formatToPar(state.scoreToPar()) + ")"));
	}

	private static String formatToPar(int scoreToPar) {
		if (scoreToPar == 0) {
			return "Even Par";
		}
		return scoreToPar > 0 ? "+" + scoreToPar : Integer.toString(scoreToPar);
	}

	public record StartResult(boolean success, String message) {
		public StartResult {
			Objects.requireNonNull(message, "message");
		}
	}
}
