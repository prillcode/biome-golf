package com.prillcode.minecraftgolf.server;

import java.util.HashSet;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.CupDetector;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.HoleLifecycle;
import com.prillcode.minecraftgolf.hole.PenaltyType;
import com.prillcode.minecraftgolf.hole.PlayerHoleSession;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;
import com.prillcode.minecraftgolf.item.GolfClubItem;
import com.prillcode.minecraftgolf.item.GolfItems;
import com.prillcode.minecraftgolf.net.HoleStateNetworking;
import com.prillcode.minecraftgolf.net.HoleStatePayload;

/** Server-authoritative lifecycle for the one configured M4 hole. */
public final class ActiveHoleService {

	public enum ShotPermission {
		PRACTICE,
		SCORING,
		WRONG_BALL,
		HOLE_COMPLETE,
		MISSING_BALL
	}

	private static final ActiveHoleService INSTANCE = new ActiveHoleService();

	private final HoleLifecycle lifecycle = new HoleLifecycle();
	private final Set<UUID> overspeedCupEntries = new HashSet<>();
	private HoleDefinition hole;

	private ActiveHoleService() {
	}

	public static ActiveHoleService instance() {
		return INSTANCE;
	}

	public void initialize(HoleDefinition configuredHole) {
		hole = Objects.requireNonNull(configuredHole, "configuredHole");
		lifecycle.clear();
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
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return new StartResult(false,
				"[golf] a hole attempt already exists; use /golf hole restart or /golf hole abandon");
		}
		return createAttempt(player, false);
	}

	public StartResult restart(ServerPlayer player) {
		if (lifecycle.session(player.getUUID()).isEmpty()) {
			return new StartResult(false, "[golf] no hole attempt to restart; use /golf hole start");
		}
		return createAttempt(player, true);
	}

	public StartResult abandon(ServerPlayer player) {
		Optional<PlayerHoleSession> session = lifecycle.session(player.getUUID());
		if (session.isEmpty()) {
			return new StartResult(false, "[golf] no hole attempt to abandon");
		}
		lifecycle.abandon(player.getUUID());
		discardAssignedBall(player, session.orElseThrow());
		MinecraftGolf.LOGGER.info("{} abandoned hole {}",
			player.getName().getString(), session.orElseThrow().state().hole().id());
		HoleStateNetworking.send(player, HoleStatePayload.practice(configuredHole()));
		return new StartResult(true, "[golf] hole abandoned; practice shots are available");
	}

	private StartResult createAttempt(ServerPlayer player, boolean restart) {
		HoleDefinition definition = configuredHole();
		ServerLevel level = player.level();
		String currentDimension = level.dimension().identifier().toString();
		if (!definition.dimension().equals(currentDimension)) {
			return new StartResult(false, "[golf] hole " + definition.number() + " is in "
				+ definition.dimension() + "; you are in " + currentDimension);
		}
		Optional<String> terrainIssue = validateTerrain(level, definition);
		if (terrainIssue.isPresent()) {
			return new StartResult(false, "[golf] unsafe configured hole: " + terrainIssue.orElseThrow()
				+ "; prepare the terrain or update config/minecraft_golf/hole.json");
		}

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

		int clearedPlayerBalls = discardPlayerOwnedBalls(player, ball.getUUID());
		if (restart) {
			lifecycle.restart(player.getUUID(), definition, ball.getUUID());
		} else {
			lifecycle.start(player.getUUID(), definition, ball.getUUID());
		}
		player.teleportTo(definition.tee().x(), definition.tee().y() + 1.0, definition.tee().z() + 2.0);
		int grantedClubs = grantMissingClubs(player);
		MinecraftGolf.LOGGER.info(
			"{} {} hole {} with ball {} at tee {}; cleared {} prior player balls; granted {} missing clubs",
			player.getName().getString(), restart ? "restarted" : "started",
			definition.id(), ball.getUUID(), definition.tee(), clearedPlayerBalls, grantedClubs);
		PlayerHoleState startedState = lifecycle.session(player.getUUID())
			.map(PlayerHoleSession::state).orElse(null);
		if (startedState != null) {
			HoleStateNetworking.send(player, HoleStatePayload.active(startedState));
		}
		String cleanup = clearedPlayerBalls == 0
			? ""
			: " | cleared " + clearedPlayerBalls + " previous player ball(s)";
		String equipment = grantedClubs == 0 ? "" : " | granted " + grantedClubs + " missing clubs";
		return new StartResult(true, "[golf] Hole " + definition.number() + " — Par "
			+ definition.par() + " | Double Par + 2 limit " + definition.strokeLimit()
			+ cleanup + equipment);
	}

	public Optional<PlayerHoleState> state(UUID playerId) {
		return lifecycle.session(playerId).map(PlayerHoleSession::state);
	}

	public String status(ServerPlayer player) {
		sendCurrentSnapshot(player);
		Optional<PlayerHoleSession> currentSession = lifecycle.session(player.getUUID());
		if (currentSession.isEmpty()) {
			HoleDefinition definition = configuredHole();
			return "[golf] no active hole | configured Hole " + definition.number()
				+ " Par " + definition.par() + " | practice shots available";
		}
		PlayerHoleSession session = currentSession.orElseThrow();
		PlayerHoleState state = session.state();
		String result = state.isComplete() ? " | COMPLETE: " + state.completionReason() : "";
		if (!state.isComplete() && assignedBall(player, session.ballUuid()).isEmpty()) {
			result = " | RECOVERY NEEDED: assigned ball is missing; use /golf hole restart";
		}
		String relativeScore = state.strokes() == 0 ? "No strokes yet" : formatToPar(state.scoreToPar());
		return "[golf] Hole " + state.hole().number() + " | strokes " + state.strokes()
			+ "/" + state.hole().strokeLimit() + " | " + relativeScore + result;
	}

	public ShotPermission shotPermission(ServerPlayer player, GolfBallEntity ball) {
		Optional<PlayerHoleSession> session = lifecycle.session(player.getUUID());
		boolean assignedBallPresent = session
			.map(current -> assignedBall(player, current.ballUuid()).isPresent())
			.orElse(false);
		return switch (lifecycle.shotPermission(
			player.getUUID(), ball.getUUID(), assignedBallPresent)) {
			case PRACTICE -> ShotPermission.PRACTICE;
			case SCORING -> ShotPermission.SCORING;
			case WRONG_BALL -> ShotPermission.WRONG_BALL;
			case HOLE_COMPLETE -> ShotPermission.HOLE_COMPLETE;
			case MISSING_BALL -> ShotPermission.MISSING_BALL;
		};
	}

	/** Called only after ShotService has performed the authoritative launch. */
	public PlayerHoleState recordAcceptedShot(ServerPlayer player, GolfBallEntity ball, Vec3 shotOrigin) {
		PlayerHoleSession session = requiredSession(player.getUUID(), ball.getUUID());
		PlayerHoleSession updatedSession = session.recordAcceptedShot(shotOrigin);
		PlayerHoleState updated = updatedSession.state();
		lifecycle.update(player.getUUID(), updatedSession);
		player.sendSystemMessage(Component.literal("[golf] Stroke " + updated.strokes() + " of "
			+ updated.hole().strokeLimit() + " | " + formatToPar(updated.scoreToPar())), true);
		if (updated.isComplete()) {
			sendCompletion(player, updated);
			HoleStateNetworking.send(player, HoleStatePayload.complete(updated));
		} else {
			HoleStateNetworking.send(player, HoleStatePayload.active(updated));
		}
		return updated;
	}

	/** Evaluates authoritative movement for physical cup completion. */
	public void onBallMoved(GolfBallEntity ball, Vec3 from, Vec3 to) {
		UUID owner = ball.owner();
		if (owner == null) {
			return;
		}
		PlayerHoleSession session = lifecycle.session(owner).orElse(null);
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
		lifecycle.update(owner, completedSession);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(completed.hole().cup());
		ball.level().playSound(null, BlockPos.containing(completed.hole().cup().x(),
			completed.hole().cup().y(), completed.hole().cup().z()),
			SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.4F);
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			sendCompletion(player, completed);
			HoleStateNetworking.send(player, HoleStatePayload.complete(completed));
		}
		MinecraftGolf.LOGGER.info("Player {} holed out hole {} in {} strokes",
			owner, completed.hole().id(), completed.strokes());
	}

	private void applyPenalty(GolfBallEntity ball, UUID owner, PlayerHoleSession session, PenaltyType penalty) {
		PlayerHoleSession updatedSession = session.applyPenalty(penalty);
		PlayerHoleState updated = updatedSession.state();
		lifecycle.update(owner, updatedSession);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(session.lastSafePosition());
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			String label = penalty == PenaltyType.WATER ? "Water" : "Out of Bounds";
			player.sendSystemMessage(Component.literal("[golf] " + label + " — one penalty stroke; ball returned"
				+ " | " + updated.strokes() + "/" + updated.hole().strokeLimit()));
			if (updated.isComplete()) {
				sendCompletion(player, updated);
				HoleStateNetworking.send(player, HoleStatePayload.complete(updated));
			} else {
				HoleStateNetworking.send(player, HoleStatePayload.active(updated));
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
		PlayerHoleSession session = lifecycle.session(player.getUUID()).orElse(null);
		if (session == null) {
			return new StartResult(false, "[golf] start the configured hole before picking up");
		}
		if (session.state().isComplete()) {
			return new StartResult(false, "[golf] this hole is already complete");
		}
		PlayerHoleSession updatedSession = session.pickUp();
		PlayerHoleState updated = updatedSession.state();
		lifecycle.update(player.getUUID(), updatedSession);
		Entity ball = player.level().getEntity(session.ballUuid());
		overspeedCupEntries.remove(session.ballUuid());
		if (ball != null) {
			ball.discard();
		}
		sendCompletion(player, updated);
		HoleStateNetworking.send(player, HoleStatePayload.complete(updated));
		return new StartResult(true, "[golf] Pick Up Ball — score recorded as " + updated.strokes());
	}

	/** Sends a MISSING_BALL snapshot to the player if they have an active in-progress session. */
	public void notifyMissingBall(ServerPlayer player) {
		lifecycle.session(player.getUUID()).ifPresent(session -> {
			if (!session.state().isComplete()) {
				HoleStateNetworking.send(player, HoleStatePayload.missingBall(session.state()));
			}
		});
	}

	/**
	 * Sends the player's current authoritative hole-state snapshot, covering all four
	 * phases including missing-ball detection. Used for join and explicit status refresh.
	 */
	public void sendCurrentSnapshot(ServerPlayer player) {
		PlayerHoleSession session = lifecycle.session(player.getUUID()).orElse(null);
		if (session == null) {
			HoleStateNetworking.send(player, HoleStatePayload.practice(configuredHole()));
			return;
		}
		PlayerHoleState state = session.state();
		if (state.isComplete()) {
			HoleStateNetworking.send(player, HoleStatePayload.complete(state));
		} else if (assignedBall(player, session.ballUuid()).isEmpty()) {
			HoleStateNetworking.send(player, HoleStatePayload.missingBall(state));
		} else {
			HoleStateNetworking.send(player, HoleStatePayload.active(state));
		}
	}

	private PlayerHoleSession requiredSession(UUID playerId, UUID ballId) {
		PlayerHoleSession session = lifecycle.session(playerId).orElse(null);
		if (session == null || !session.ballUuid().equals(ballId)) {
			throw new IllegalStateException("accepted scoring shot has no matching active session");
		}
		return session;
	}

	private static Optional<String> validateTerrain(ServerLevel level, HoleDefinition definition) {
		Optional<String> teeIssue = validatePoint(level, "tee", definition.tee());
		if (teeIssue.isPresent()) {
			return teeIssue;
		}
		return validatePoint(level, "cup", definition.cup());
	}

	private static Optional<String> validatePoint(ServerLevel level, String label, Vec3 point) {
		BlockPos occupied = BlockPos.containing(point.x(), point.y(), point.z());
		BlockPos support = BlockPos.containing(
			point.x(), point.y() - GolfBallEntity.BALL_RADIUS - 0.01, point.z());
		if (!level.isLoaded(occupied) || !level.isLoaded(support)) {
			return Optional.of(label + " chunks are not loaded near " + point);
		}
		if (!level.getFluidState(occupied).isEmpty()) {
			return Optional.of(label + " position " + point + " is submerged");
		}
		if (level.getBlockState(support).getCollisionShape(level, support).isEmpty()) {
			return Optional.of(label + " has no solid support at " + support.toShortString());
		}
		return Optional.empty();
	}

	private static int grantMissingClubs(ServerPlayer player) {
		int granted = 0;
		for (GolfClubItem club : GolfItems.CLUB_ITEMS.values()) {
			ItemStack stack = new ItemStack(club);
			if (player.getInventory().contains(stack)) {
				continue;
			}
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
			granted++;
		}
		return granted;
	}

	private int discardPlayerOwnedBalls(ServerPlayer player, UUID keepBallUuid) {
		int removed = 0;
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (GolfBallEntity ball : level.getEntities(
				EntityTypeTest.forClass(GolfBallEntity.class),
				candidate -> player.getUUID().equals(candidate.owner())
					&& !keepBallUuid.equals(candidate.getUUID()))) {
				overspeedCupEntries.remove(ball.getUUID());
				ball.discard();
				removed++;
			}
		}
		return removed;
	}

	private void discardAssignedBall(ServerPlayer player, PlayerHoleSession session) {
		overspeedCupEntries.remove(session.ballUuid());
		assignedBall(player, session.ballUuid()).ifPresent(Entity::discard);
	}

	private static Optional<Entity> assignedBall(ServerPlayer player, UUID ballUuid) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			Entity entity = level.getEntity(ballUuid);
			if (entity != null) {
				return Optional.of(entity);
			}
		}
		return Optional.empty();
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
