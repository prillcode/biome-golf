package com.prillcode.minecraftgolf.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
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
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseScorecard;
import com.prillcode.minecraftgolf.course.HoleScore;
import com.prillcode.minecraftgolf.course.PlayerCourseState;
import com.prillcode.minecraftgolf.club.GolfClubs;
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
import com.prillcode.minecraftgolf.round.ParticipantStatus;
import com.prillcode.minecraftgolf.round.ReadyGolfParticipant;
import com.prillcode.minecraftgolf.round.ReadyGolfRound;
import com.prillcode.minecraftgolf.round.RoundPhase;
import com.prillcode.minecraftgolf.server.TravelDestinationSearch.Destination;

/** Server-authoritative solo and Ready Golf lifecycle for the configured M5 course. */
public final class ActiveHoleService {

	private static final double PRACTICE_SPAWN_FORWARD = 2.0;
	private static final double PRACTICE_SPAWN_UP = 1.0;

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
	private CourseDefinition course;
	private ReadyGolfRound activeRound;

	private ActiveHoleService() {
	}

	public static ActiveHoleService instance() {
		return INSTANCE;
	}

	public void initialize(HoleDefinition configuredHole) {
		hole = Objects.requireNonNull(configuredHole, "configuredHole");
		course = null;
		activeRound = null;
		lifecycle.clear();
		overspeedCupEntries.clear();
		MinecraftGolf.LOGGER.info("Loaded hole {} (#{} par {}, Double Par + 2 limit {}) in {}",
			hole.id(), hole.number(), hole.par(), hole.strokeLimit(), hole.dimension());
	}

	public void initializeCourse(CourseDefinition configuredCourse) {
		course = Objects.requireNonNull(configuredCourse, "configuredCourse");
		hole = course.hole(1);
		activeRound = null;
		lifecycle.clear();
		overspeedCupEntries.clear();
		MinecraftGolf.LOGGER.info("Loaded course {} (3 holes, par {}) using layout {} v{}",
			course.id(), course.totalPar(), course.generatedLayout().id(), course.generatedLayout().version());
	}

	public HoleDefinition configuredHole() {
		if (hole == null) {
			throw new IllegalStateException("active-hole service has not been initialized");
		}
		return hole;
	}

	/** The configured course, or {@code null} when only a single hole is initialized. */
	public CourseDefinition configuredCourseOrNull() {
		return course;
	}

	/**
	 * True while any play is in flight: a non-complete Ready Golf lobby/round or
	 * any player's solo hole attempt. Used to gate course selection and deletion.
	 */
	public boolean hasActivePlay() {
		return (activeRound != null && activeRound.phase() != RoundPhase.COMPLETE)
			|| lifecycle.hasAnySession();
	}

	/** The currently configured hole, or {@code null} before any initialization. */
	public HoleDefinition configuredHoleOrNull() {
		return hole;
	}

	public StartResult createRound(ServerPlayer creator) {
		if (course == null) {
			return new StartResult(false, "[golf] no configured course is available");
		}
		if (activeRound != null && activeRound.phase() != RoundPhase.COMPLETE) {
			return new StartResult(false, "[golf] a Ready Golf lobby or round already exists");
		}
		if (activeRound != null) {
			cleanupRoundSessions(creator, activeRound);
			activeRound = null;
		}
		if (lifecycle.session(creator.getUUID()).isPresent()) {
			return new StartResult(false, "[golf] abandon your current hole before creating a round");
		}
		activeRound = ReadyGolfRound.create(UUID.randomUUID(), course, creator.getUUID());
		MinecraftGolf.LOGGER.info("{} created Ready Golf lobby {}",
			creator.getName().getString(), activeRound.roundId());
		return new StartResult(true, "[golf] Ready Golf lobby created; other golfers may use /golf round join");
	}

	public StartResult joinRound(ServerPlayer player) {
		if (activeRound == null || activeRound.phase() != RoundPhase.LOBBY) {
			return new StartResult(false, "[golf] no open Ready Golf lobby; use /golf round create");
		}
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return new StartResult(false, "[golf] abandon your current hole before joining a round");
		}
		try {
			activeRound = activeRound.join(player.getUUID());
		} catch (IllegalStateException exception) {
			return new StartResult(false, "[golf] " + exception.getMessage());
		}
		MinecraftGolf.LOGGER.info("{} joined Ready Golf lobby {}",
			player.getName().getString(), activeRound.roundId());
		return new StartResult(true, "[golf] joined Ready Golf lobby ("
			+ activeRound.participants().size() + "/" + ReadyGolfRound.MAX_PARTICIPANTS + ")");
	}

	public StartResult startRound(ServerPlayer coordinator) {
		if (activeRound == null || activeRound.phase() != RoundPhase.LOBBY) {
			return new StartResult(false, "[golf] create or join a lobby before starting");
		}
		ReadyGolfRound lobby = activeRound;
		ReadyGolfRound started;
		try {
			started = lobby.start(coordinator.getUUID());
		} catch (IllegalStateException exception) {
			return new StartResult(false, "[golf] " + exception.getMessage());
		}
		HoleDefinition firstHole = course.hole(1);
		for (ReadyGolfParticipant participant : started.participants()) {
			ServerPlayer golfer = coordinator.level().getServer().getPlayerList().getPlayer(participant.playerId());
			if (golfer == null) {
				return new StartResult(false, "[golf] every lobby golfer must be online before starting");
			}
			Optional<String> issue = attemptPreflight(golfer, firstHole);
			if (issue.isPresent()) {
				return new StartResult(false, issue.orElseThrow());
			}
		}

		activeRound = started;
		for (ReadyGolfParticipant participant : started.participants()) {
			ServerPlayer golfer = coordinator.level().getServer().getPlayerList().getPlayer(participant.playerId());
			StartResult result = createAttempt(golfer, false, firstHole);
			if (!result.success()) {
				cleanupStartedRound(coordinator, started);
				activeRound = lobby;
				return new StartResult(false, "[golf] round start rolled back: " + result.message());
			}
		}
		MinecraftGolf.LOGGER.info("Started Ready Golf round {} with {} golfers",
			activeRound.roundId(), activeRound.participants().size());
		return new StartResult(true, "[golf] Ready Golf started with "
			+ activeRound.participants().size() + " golfer(s)");
	}

	public StartResult leaveRound(ServerPlayer player) {
		if (activeRound == null || activeRound.phase() == RoundPhase.COMPLETE
				|| activeRound.findParticipant(player.getUUID()).isEmpty()) {
			return new StartResult(false, "[golf] you are not in an active Ready Golf round");
		}
		Optional<PlayerHoleSession> session = lifecycle.session(player.getUUID());
		session.ifPresent(current -> discardAssignedBall(player, current));
		lifecycle.abandon(player.getUUID());
		activeRound = activeRound.withdraw(player.getUUID());
		HoleStateNetworking.send(player, HoleStatePayload.practice(configuredHole()));
		notifyTerminalBarrier(player.level().getServer());
		MinecraftGolf.LOGGER.info("{} left Ready Golf round", player.getName().getString());
		return new StartResult(true, "[golf] left the Ready Golf round; practice shots are available");
	}

	public String roundStatus(ServerPlayer player) {
		if (activeRound == null || activeRound.phase() == RoundPhase.COMPLETE) {
			return "[golf] no active Ready Golf lobby or round";
		}
		long active = activeRound.participants().stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE).count();
		return "[golf] Ready Golf " + activeRound.phase() + " | " + active + "/"
			+ activeRound.participants().size() + " golfers | Hole "
			+ (activeRound.currentHoleIndex() + 1);
	}

	public StartResult start(ServerPlayer player) {
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return new StartResult(false,
				"[golf] a hole attempt already exists; use /golf hole restart or /golf hole abandon");
		}
		if (course != null) {
			PlayerCourseState existing = courseState(player.getUUID());
			if (existing != null) {
				return new StartResult(false, existing.isComplete()
					? "[golf] course complete; use /golf hole restart to replay"
					: "[golf] course recovery needed; use /golf hole restart");
			}
			if (activeRound != null && activeRound.phase() != RoundPhase.COMPLETE) {
				return new StartResult(false, "[golf] a Ready Golf lobby or round already exists; use /golf round join");
			}
			if (activeRound != null) {
				cleanupRoundSessions(player, activeRound);
			}
			activeRound = ReadyGolfRound.create(UUID.randomUUID(), course, player.getUUID())
				.start(player.getUUID());
			PlayerCourseState started = courseState(player.getUUID());
			StartResult result = createAttempt(player, false, started.currentHole().hole());
			if (!result.success()) {
				activeRound = null;
			}
			return result;
		}
		return createAttempt(player, false, configuredHole());
	}

	public StartResult dropPracticeBall(ServerPlayer player) {
		if (!lifecycle.allowsPracticeBall(player.getUUID())) {
			return new StartResult(false,
				"[golf] finish, restart, or abandon the current hole before dropping a practice ball");
		}
		ServerLevel level = player.level();
		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(level, EntitySpawnReason.COMMAND);
		if (ball == null) {
			return new StartResult(false, "[golf] failed to create a practice ball");
		}
		double yaw = Math.toRadians(player.getYRot());
		ball.setOwner(player.getUUID());
		ball.setPos(
			player.getX() - Math.sin(yaw) * PRACTICE_SPAWN_FORWARD,
			player.getY() + PRACTICE_SPAWN_UP,
			player.getZ() + Math.cos(yaw) * PRACTICE_SPAWN_FORWARD);
		if (!level.addFreshEntity(ball)) {
			return new StartResult(false, "[golf] failed to add the practice ball to the world");
		}
		MinecraftGolf.LOGGER.info("{} dropped player-owned practice ball {}",
			player.getName().getString(), ball.getUUID());
		return new StartResult(true, "[golf] dropped a practice ball");
	}

	/** Resets the current golf set into hotbar slots 0 through 6. */
	public StartResult equipClubs(ServerPlayer player) {
		List<ItemStack> displaced = new ArrayList<>();
		for (int slot = 0; slot < 7; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && !(stack.getItem() instanceof GolfClubItem)) {
				displaced.add(stack.copy());
			}
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
		for (int slot = 7; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).getItem() instanceof GolfClubItem) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
		for (int slot = 0; slot < GolfClubs.ALL.size(); slot++) {
			player.getInventory().setItem(slot,
				new ItemStack(GolfItems.itemFor(GolfClubs.ALL.get(slot).id())));
		}
		for (ItemStack stack : displaced) {
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
		MinecraftGolf.LOGGER.info("{} reset the golf club set into hotbar slots 0-6 via /golf clubs equip",
			player.getName().getString());
		return new StartResult(true, "[golf] reset the full club set into hotbar slots 1-7");
	}

	public StartResult restart(ServerPlayer player) {
		PlayerCourseState courseState = courseState(player.getUUID());
		if (course != null && courseState != null) {
			boolean hadSession = lifecycle.session(player.getUUID()).isPresent();
			try {
				activeRound = courseState.isComplete()
					? activeRound.replaySolo(player.getUUID())
					: activeRound.restartCurrentHole(player.getUUID());
			} catch (IllegalStateException exception) {
				return new StartResult(false, "[golf] " + exception.getMessage());
			}
			PlayerCourseState restarted = courseState(player.getUUID());
			return createAttempt(player, hadSession, restarted.currentHole().hole());
		}
		if (lifecycle.session(player.getUUID()).isEmpty()) {
			return new StartResult(false, "[golf] no hole attempt to restart; use /golf hole start");
		}
		return createAttempt(player, true, configuredHole());
	}

	public StartResult abandon(ServerPlayer player) {
		Optional<PlayerHoleSession> session = lifecycle.session(player.getUUID());
		if (session.isEmpty() && courseState(player.getUUID()) == null) {
			return new StartResult(false, "[golf] no hole attempt to abandon");
		}
		lifecycle.abandon(player.getUUID());
		session.ifPresent(current -> discardAssignedBall(player, current));
		if (activeRound != null && activeRound.findParticipant(player.getUUID()).isPresent()
				&& activeRound.phase() != RoundPhase.COMPLETE) {
			activeRound = activeRound.withdraw(player.getUUID());
			notifyTerminalBarrier(player.level().getServer());
		}
		MinecraftGolf.LOGGER.info("{} abandoned the active golf attempt", player.getName().getString());
		HoleStateNetworking.send(player, HoleStatePayload.practice(configuredHole()));
		return new StartResult(true, "[golf] hole abandoned; practice shots are available");
	}

	private Optional<String> attemptPreflight(ServerPlayer player, HoleDefinition definition) {
		ServerLevel level = player.level();
		String currentDimension = level.dimension().identifier().toString();
		if (!definition.dimension().equals(currentDimension)) {
			return Optional.of("[golf] " + player.getName().getString() + " must be in "
				+ definition.dimension() + " before the round starts");
		}
		loadPointChunk(level, definition.tee());
		loadPointChunk(level, definition.cup());
		return validateTerrain(level, definition)
			.map(issue -> "[golf] unsafe configured hole: " + issue);
	}

	private void cleanupStartedRound(ServerPlayer coordinator, ReadyGolfRound started) {
		for (ReadyGolfParticipant participant : started.participants()) {
			ServerPlayer golfer = coordinator.level().getServer().getPlayerList().getPlayer(participant.playerId());
			if (golfer == null) {
				continue;
			}
			lifecycle.session(participant.playerId()).ifPresent(session -> discardAssignedBall(golfer, session));
			lifecycle.abandon(participant.playerId());
			HoleStateNetworking.send(golfer, HoleStatePayload.practice(configuredHole()));
		}
	}

	private void cleanupRoundSessions(ServerPlayer serverContext, ReadyGolfRound round) {
		for (ReadyGolfParticipant participant : round.participants()) {
			lifecycle.session(participant.playerId())
				.ifPresent(session -> discardAssignedBall(serverContext, session));
			lifecycle.abandon(participant.playerId());
		}
	}

	private StartResult createAttempt(ServerPlayer player, boolean restart, HoleDefinition definition) {
		ServerLevel level = player.level();
		String currentDimension = level.dimension().identifier().toString();
		if (!definition.dimension().equals(currentDimension)) {
			return new StartResult(false, "[golf] hole " + definition.number() + " is in "
				+ definition.dimension() + "; you are in " + currentDimension);
		}
		if (course != null) {
			loadPointChunk(level, definition.tee());
			loadPointChunk(level, definition.cup());
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
		player.teleportTo(level,
			definition.transition().playerPosition().x(),
			definition.transition().playerPosition().y(),
			definition.transition().playerPosition().z(),
			Set.of(), (float) definition.transition().yaw(), (float) definition.transition().pitch(), true);
		int grantedClubs = grantMissingClubs(player);
		MinecraftGolf.LOGGER.info(
			"{} {} hole {} with ball {} at tee {}; cleared {} prior player balls; granted {} missing clubs",
			player.getName().getString(), restart ? "restarted" : "started",
			definition.id(), ball.getUUID(), definition.tee(), clearedPlayerBalls, grantedClubs);
		PlayerHoleState startedState = lifecycle.session(player.getUUID())
			.map(PlayerHoleSession::state).orElse(null);
		if (startedState != null) {
			sendActiveSnapshot(player, startedState, definition.tee());
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
			PlayerCourseState courseState = courseState(player.getUUID());
			if (courseState != null && courseState.isComplete()) {
				CourseScorecard scorecard = courseState.finalScorecard();
				return "[golf] course complete | " + scorecard.totalStrokes() + " strokes | "
					+ formatToPar(scorecard.scoreToPar()) + " through 3 holes";
			}
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
		String cumulative = cumulativeStatus(player.getUUID(), state);
		return "[golf] Hole " + state.hole().number() + " | strokes " + state.strokes()
			+ "/" + state.hole().strokeLimit() + " | " + relativeScore + cumulative + result;
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
		updateCourseState(player.getUUID(), updated);
		player.sendSystemMessage(Component.literal("[golf] Stroke " + updated.strokes() + " of "
			+ updated.hole().strokeLimit() + " | " + formatToPar(updated.scoreToPar())), true);
		if (updated.isComplete()) {
			sendCompletion(player, updated);
			sendCompleteSnapshot(player, updated);
			notifyTerminalBarrier(player.level().getServer());
		} else {
			sendActiveSnapshot(player, updated, shotOrigin);
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
		// S5: a suspended or withdrawn golfer's hole state is frozen; their ball may
		// still coast to a rest, but it must not score or penalize while offline.
		if (!isActiveParticipant(owner)) {
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
		updateCourseState(owner, completed);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(completed.hole().cup());
		ball.level().playSound(null, BlockPos.containing(completed.hole().cup().x(),
			completed.hole().cup().y(), completed.hole().cup().z()),
			SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.4F);
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			sendCompletion(player, completed);
			sendCompleteSnapshot(player, completed);
			notifyTerminalBarrier(player.level().getServer());
		}
		MinecraftGolf.LOGGER.info("Player {} holed out hole {} in {} strokes",
			owner, completed.hole().id(), completed.strokes());
	}

	/** Automatically moves the owner near an active ball only after a natural physics rest. */
	public void onBallCameToRest(GolfBallEntity ball) {
		UUID owner = ball.owner();
		if (owner == null) {
			return;
		}
		PlayerHoleSession session = lifecycle.session(owner).orElse(null);
		if (session == null || session.state().isComplete()
				|| !session.ballUuid().equals(ball.getUUID())) {
			return;
		}
		if (!isActiveParticipant(owner)) {
			return;
		}
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player == null) {
			return;
		}
		sendActiveSnapshot(player, session.state(), ball.ballState().position());
		StartResult travel = travelToNextShot(player, ball);
		player.sendSystemMessage(Component.literal(travel.message()));
	}

	private void applyPenalty(GolfBallEntity ball, UUID owner, PlayerHoleSession session, PenaltyType penalty) {
		PlayerHoleSession updatedSession = session.applyPenalty(penalty);
		PlayerHoleState updated = updatedSession.state();
		lifecycle.update(owner, updatedSession);
		updateCourseState(owner, updated);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(session.lastSafePosition());
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			String label = penalty == PenaltyType.WATER ? "Water" : "Out of Bounds";
			player.sendSystemMessage(Component.literal("[golf] " + label + " — one penalty stroke; ball returned"
				+ " | " + updated.strokes() + "/" + updated.hole().strokeLimit()));
			if (updated.isComplete()) {
				sendCompletion(player, updated);
				sendCompleteSnapshot(player, updated);
				notifyTerminalBarrier(player.level().getServer());
			} else {
				sendActiveSnapshot(player, updated, session.lastSafePosition());
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
		updateCourseState(player.getUUID(), updated);
		Entity ball = player.level().getEntity(session.ballUuid());
		overspeedCupEntries.remove(session.ballUuid());
		if (ball != null) {
			ball.discard();
		}
		sendCompletion(player, updated);
		sendCompleteSnapshot(player, updated);
		notifyTerminalBarrier(player.level().getServer());
		return new StartResult(true, "[golf] Pick Up Ball — score recorded as " + updated.strokes());
	}

	public StartResult nextHole(ServerPlayer player) {
		PlayerCourseState state = courseState(player.getUUID());
		if (state == null) {
			return new StartResult(false, "[golf] start the course before advancing");
		}
		ReadyGolfParticipant caller = activeRound.findParticipant(player.getUUID()).orElse(null);
		if (caller == null || caller.status() != ParticipantStatus.ACTIVE) {
			return new StartResult(false, "[golf] only an active participant can advance the round");
		}
		if (state.isComplete()) {
			return new StartResult(false, "[golf] course is already complete; use /golf hole restart to replay");
		}
		if (!state.currentHole().isComplete()) {
			return new StartResult(false, "[golf] complete the current hole before advancing");
		}
		if (!activeRound.allActiveTerminal()) {
			MinecraftGolf.LOGGER.info(
				"Rejected early Hole {} advancement in Ready Golf round {}: {}/{} active golfers complete",
				activeRound.currentHoleIndex() + 1, activeRound.roundId(),
				activeRound.terminalActiveParticipantCount(), activeRound.activeParticipantCount());
			return new StartResult(false, waitingMessage());
		}

		int expectedHoleIndex = activeRound.currentHoleIndex();
		HoleDefinition nextDefinition = course.holes().get(expectedHoleIndex + 1);
		List<PreparedAttempt> prepared = prepareTransition(player, nextDefinition);
		if (prepared == null) {
			return new StartResult(false,
				"[golf] transition cancelled; every golfer remains on the current hole");
		}

		sendMultiplayerHoleResults(player.level().getServer(), activeRound);
		ReadyGolfRound previousRound = activeRound;
		activeRound = activeRound.advanceNextHole(expectedHoleIndex);
		cleanupSuspendedAtTransition(player, previousRound);
		commitTransition(prepared, nextDefinition);
		MinecraftGolf.LOGGER.info("Advanced Ready Golf round {} from Hole {} to Hole {} for {} golfers",
			activeRound.roundId(), expectedHoleIndex + 1, expectedHoleIndex + 2, prepared.size());
		return new StartResult(true, "[golf] all golfers advanced to Hole " + (expectedHoleIndex + 2));
	}

	private List<PreparedAttempt> prepareTransition(ServerPlayer serverContext,
			HoleDefinition nextDefinition) {
		List<PreparedAttempt> prepared = new ArrayList<>();
		for (ReadyGolfParticipant participant : activeRound.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE) {
				continue;
			}
			ServerPlayer golfer = serverContext.level().getServer().getPlayerList()
				.getPlayer(participant.playerId());
			PlayerHoleSession oldSession = lifecycle.session(participant.playerId()).orElse(null);
			if (golfer == null || oldSession == null) {
				discardPreparedAttempts(prepared);
				return null;
			}
			Optional<String> issue = attemptPreflight(golfer, nextDefinition);
			if (issue.isPresent()) {
				MinecraftGolf.LOGGER.warn("Ready Golf transition preflight failed: {}", issue.orElseThrow());
				discardPreparedAttempts(prepared);
				return null;
			}
			GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(
				golfer.level(), EntitySpawnReason.COMMAND);
			if (ball == null) {
				discardPreparedAttempts(prepared);
				return null;
			}
			ball.setOwner(participant.playerId());
			ball.placeAtRest(nextDefinition.tee());
			if (!golfer.level().addFreshEntity(ball)) {
				ball.discard();
				discardPreparedAttempts(prepared);
				return null;
			}
			prepared.add(new PreparedAttempt(golfer, oldSession, ball));
		}
		return prepared;
	}

	private static void discardPreparedAttempts(List<PreparedAttempt> prepared) {
		prepared.forEach(attempt -> attempt.ball().discard());
	}

	private void cleanupSuspendedAtTransition(ServerPlayer serverContext, ReadyGolfRound previousRound) {
		for (ReadyGolfParticipant participant : previousRound.participants()) {
			if (participant.status() != ParticipantStatus.SUSPENDED) {
				continue;
			}
			lifecycle.session(participant.playerId())
				.ifPresent(session -> discardAssignedBall(serverContext, session));
			lifecycle.abandon(participant.playerId());
		}
	}

	private void commitTransition(List<PreparedAttempt> prepared, HoleDefinition nextDefinition) {
		for (PreparedAttempt attempt : prepared) {
			BlockPos cupBlockPos = BlockPos.containing(nextDefinition.cup().x(),
				nextDefinition.cup().y() - GolfBallEntity.BALL_RADIUS, nextDefinition.cup().z());
			attempt.player().level().setBlockAndUpdate(
				cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
			discardAssignedBall(attempt.player(), attempt.oldSession());
			lifecycle.restart(attempt.player().getUUID(), nextDefinition, attempt.ball().getUUID());
			discardPlayerOwnedBalls(attempt.player(), attempt.ball().getUUID());
			teleportToTransition(attempt.player(), nextDefinition);
			grantMissingClubs(attempt.player());
			PlayerHoleState started = lifecycle.session(attempt.player().getUUID())
				.orElseThrow().state();
			sendActiveSnapshot(attempt.player(), started, nextDefinition.tee());
		}
	}

	private static void teleportToTransition(ServerPlayer player, HoleDefinition definition) {
		player.teleportTo(player.level(),
			definition.transition().playerPosition().x(),
			definition.transition().playerPosition().y(),
			definition.transition().playerPosition().z(),
			Set.of(), (float) definition.transition().yaw(),
			(float) definition.transition().pitch(), true);
	}

	private StartResult travelToNextShot(ServerPlayer player, GolfBallEntity ball) {
		BlockPos ballBlock = BlockPos.containing(ball.getX(), ball.getY(), ball.getZ());
		Optional<Destination> destination = TravelDestinationSearch.find(
			ballBlock.getX(), ballBlock.getY(), ballBlock.getZ(),
			candidate -> isSafeTravelDestination(player.level(), candidate));
		if (destination.isEmpty()) {
			return new StartResult(false, "[golf] no safe standing position found near the ball");
		}
		Destination safe = destination.orElseThrow();
		player.teleportTo(safe.x() + 0.5, safe.y(), safe.z() + 0.5);
		return new StartResult(true, "[golf] Ball stopped — moved safely to your next shot");
	}

	private static boolean isSafeTravelDestination(ServerLevel level, Destination destination) {
		BlockPos feet = new BlockPos(destination.x(), destination.y(), destination.z());
		BlockPos head = feet.above();
		BlockPos support = feet.below();
		return level.getWorldBorder().isWithinBounds(feet)
			&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
			&& level.getBlockState(head).getCollisionShape(level, head).isEmpty()
			&& !level.getBlockState(support).getCollisionShape(level, support).isEmpty()
			&& level.getFluidState(feet).isEmpty()
			&& level.getFluidState(head).isEmpty();
	}

	/**
	 * S5: reapplies the authoritative snapshot on join and, if this golfer was
	 * suspended from the active round, reconnects them before entity validation.
	 */
	public void onPlayerConnected(ServerPlayer player) {
		UUID playerId = player.getUUID();
		if (activeRound != null && activeRound.phase() == RoundPhase.PLAYING) {
			ReadyGolfParticipant participant = activeRound.findParticipant(playerId).orElse(null);
			if (participant != null && participant.status() == ParticipantStatus.SUSPENDED) {
				activeRound = activeRound.reconnect(playerId);
				MinecraftGolf.LOGGER.info("{} reconnected to Ready Golf round {}",
					player.getName().getString(), activeRound.roundId());
			}
		}
		sendCurrentSnapshot(player);
	}

	/**
	 * S5: removes a lobby golfer or suspends an in-progress golfer on disconnect.
	 * Suspension removes the golfer from the advancement barrier so an offline
	 * player can never deadlock the remaining golfers.
	 */
	public void onPlayerDisconnected(ServerPlayer player, MinecraftServer server) {
		if (activeRound == null || activeRound.phase() == RoundPhase.COMPLETE) {
			return;
		}
		UUID playerId = player.getUUID();
		ReadyGolfParticipant participant = activeRound.findParticipant(playerId).orElse(null);
		if (participant == null || participant.status() != ParticipantStatus.ACTIVE) {
			return;
		}
		if (activeRound.phase() == RoundPhase.LOBBY) {
			activeRound = activeRound.disconnect(playerId);
			MinecraftGolf.LOGGER.info("{} left Ready Golf lobby {}",
				player.getName().getString(), activeRound.roundId());
			return;
		}
		activeRound = activeRound.disconnect(playerId);
		MinecraftGolf.LOGGER.info("{} suspended from Ready Golf round {}",
			player.getName().getString(), activeRound.roundId());
		notifyTerminalBarrier(server);
	}

	/** Sends a MISSING_BALL snapshot to the player if they have an active in-progress session. */
	public void notifyMissingBall(ServerPlayer player) {
		lifecycle.session(player.getUUID()).ifPresent(session -> {
			if (!session.state().isComplete()) {
				sendMissingSnapshot(player, session.state());
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
			sendCompleteSnapshot(player, state);
		} else if (assignedBall(player, session.ballUuid()).isEmpty()) {
			sendMissingSnapshot(player, state);
		} else {
			Entity entity = assignedBall(player, session.ballUuid()).orElseThrow();
			Vec3 ballPosition = entity instanceof GolfBallEntity ball && ball.ballState() != null
				? ball.ballState().position()
				: new Vec3(entity.getX(), entity.getY(), entity.getZ());
			sendActiveSnapshot(player, state, ballPosition);
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

	private static void loadPointChunk(ServerLevel level, Vec3 point) {
		BlockPos block = BlockPos.containing(point.x(), point.y(), point.z());
		level.getChunk(block.getX() >> 4, block.getZ() >> 4);
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

	private void updateCourseState(UUID playerId, PlayerHoleState updated) {
		if (activeRound == null || activeRound.phase() != RoundPhase.PLAYING) {
			return;
		}
		ReadyGolfParticipant participant = activeRound.findParticipant(playerId).orElse(null);
		if (participant == null || participant.status() != ParticipantStatus.ACTIVE) {
			return;
		}
		activeRound = activeRound.updateCurrentHole(playerId, updated);
	}

	/** True while no round owns the player, or while they are an ACTIVE participant. */
	private boolean isActiveParticipant(UUID playerId) {
		if (activeRound == null) {
			return true;
		}
		if (activeRound.phase() != RoundPhase.PLAYING) {
			return false;
		}
		return activeRound.findParticipant(playerId)
			.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.orElse(false);
	}

	private PlayerCourseState courseState(UUID playerId) {
		if (activeRound == null) {
			return null;
		}
		// Only an ACTIVE participant owns live course progress. A withdrawn (or
		// suspended) golfer must not block the solo fast path with stale state.
		return activeRound.findParticipant(playerId)
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.map(ReadyGolfParticipant::courseState)
			.orElse(null);
	}

	private String cumulativeStatus(UUID playerId, PlayerHoleState current) {
		return cumulativeStatus(courseState(playerId), current);
	}

	static String cumulativeStatus(PlayerCourseState state, PlayerHoleState current) {
		if (state == null) {
			return "";
		}
		if (state.isComplete()) {
			CourseScorecard scorecard = state.finalScorecard();
			return " | course " + scorecard.totalStrokes() + " strokes ("
				+ formatToPar(scorecard.scoreToPar()) + ")";
		}
		int strokes = state.completedStrokes() + current.strokes();
		int par = state.completedPar() + current.hole().par();
		return " | course " + strokes + " strokes (" + formatToPar(strokes - par) + ")";
	}

	private HoleStatePayload withCourseTotals(UUID playerId, HoleStatePayload payload, PlayerHoleState current) {
		PlayerCourseState state = courseState(playerId);
		if (state == null) {
			return payload;
		}
		if (state.isComplete()) {
			CourseScorecard scorecard = state.finalScorecard();
			return payload.withCourseTotals(
				scorecard.totalStrokes(), scorecard.totalPar(), scorecard.totalPar());
		}
		return payload.withCourseTotals(
			state.completedStrokes() + current.strokes(),
			state.completedPar() + (current.strokes() > 0 || current.isComplete()
				? current.hole().par() : 0),
			state.course().totalPar());
	}

	private void sendActiveSnapshot(ServerPlayer player, PlayerHoleState state, Vec3 ballPosition) {
		HoleStateNetworking.send(player, withCourseTotals(player.getUUID(),
			HoleStatePayload.active(state, ballPosition), state));
	}

	private void sendMissingSnapshot(ServerPlayer player, PlayerHoleState state) {
		HoleStateNetworking.send(player, withCourseTotals(player.getUUID(),
			HoleStatePayload.missingBall(state), state));
	}

	private void sendCompleteSnapshot(ServerPlayer player, PlayerHoleState state) {
		HoleStatePayload payload = withCourseTotals(player.getUUID(),
			HoleStatePayload.complete(state), state);
		if (isRoundComplete(player.getUUID())) {
			payload = payload.asRoundComplete();
		} else if (roundAdvanceAvailable(player.getUUID(), state)) {
			payload = payload.withRoundAdvanceAvailable(true);
		}
		HoleStateNetworking.send(player, payload);
	}

	private boolean isRoundComplete(UUID playerId) {
		return activeRound != null && activeRound.phase() == RoundPhase.COMPLETE
			&& activeRound.findParticipant(playerId)
				.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
				.orElse(false);
	}

	private boolean roundAdvanceAvailable(UUID playerId, PlayerHoleState state) {
		if (!state.isComplete() || activeRound == null || course == null
				|| activeRound.phase() != RoundPhase.PLAYING
				|| activeRound.currentHoleIndex() >= course.holes().size() - 1
				|| !activeRound.allActiveTerminal()) {
			return false;
		}
		return activeRound.findParticipant(playerId)
			.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.orElse(false);
	}

	private void sendFinalScorecard(ServerPlayer player, CourseScorecard scorecard) {
		player.sendSystemMessage(Component.literal("[golf] FINAL SCORECARD").withStyle(ChatFormatting.GOLD));
		for (HoleScore score : scorecard.holes()) {
			player.sendSystemMessage(Component.literal("Hole " + score.holeNumber() + " — "
				+ score.strokes() + " strokes on Par " + score.par() + " ("
				+ formatToPar(score.scoreToPar()) + ")"));
		}
		player.sendSystemMessage(Component.literal("Total — " + scorecard.totalStrokes()
			+ " strokes on Par " + scorecard.totalPar() + " ("
			+ formatToPar(scorecard.scoreToPar()) + ")").withStyle(ChatFormatting.GOLD));
	}

	private void notifyTerminalBarrier(MinecraftServer server) {
		if (activeRound == null) {
			return;
		}
		if (activeRound.phase() == RoundPhase.COMPLETE) {
			for (ReadyGolfParticipant participant : activeRound.participants()) {
				if (participant.status() != ParticipantStatus.ACTIVE) {
					continue;
				}
				ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
				if (recipient != null) {
					sendCompleteSnapshot(recipient,
						participant.courseState().completedHoles().getLast());
				}
			}
			sendMultiplayerFinalResults(server, activeRound);
			return;
		}
		if (activeRound.phase() != RoundPhase.PLAYING) {
			return;
		}
		for (ReadyGolfParticipant participant : activeRound.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE
					|| !participant.courseState().currentHole().isComplete()) {
				continue;
			}
			ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
			if (recipient != null) {
				if (activeRound.allActiveTerminal()) {
					sendCompleteSnapshot(recipient, participant.courseState().currentHole());
				}
				sendBarrierMessage(recipient);
			}
		}
	}

	private void sendMultiplayerHoleResults(MinecraftServer server, ReadyGolfRound round) {
		if (round.activeParticipantCount() <= 1) {
			return;
		}
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("[golf] HOLE " + (round.currentHoleIndex() + 1)
			+ " RESULTS").withStyle(ChatFormatting.GOLD));
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE) {
				continue;
			}
			PlayerHoleState result = participant.courseState().currentHole();
			lines.add(Component.literal(playerLabel(server, participant.playerId()) + " — "
				+ result.strokes() + " strokes (" + formatToPar(result.scoreToPar()) + ")"));
		}
		sendToActiveParticipants(server, round, lines);
	}

	private void sendMultiplayerFinalResults(MinecraftServer server, ReadyGolfRound round) {
		if (round.activeParticipantCount() > 1) {
			List<Component> lines = new ArrayList<>();
			lines.add(Component.literal("[golf] READY GOLF FINAL RESULTS")
				.withStyle(ChatFormatting.GOLD));
			for (ReadyGolfParticipant participant : round.participants()) {
				if (participant.status() != ParticipantStatus.ACTIVE) {
					continue;
				}
				CourseScorecard scorecard = participant.courseState().finalScorecard();
				lines.add(Component.literal(playerLabel(server, participant.playerId()) + " — "
					+ scorecard.totalStrokes() + " strokes ("
					+ formatToPar(scorecard.scoreToPar()) + ")"));
			}
			sendToActiveParticipants(server, round, lines);
		}
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE) {
				continue;
			}
			ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
			if (recipient != null) {
				sendFinalScorecard(recipient, participant.courseState().finalScorecard());
			}
		}
	}

	private static void sendToActiveParticipants(MinecraftServer server, ReadyGolfRound round,
			List<Component> lines) {
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE) {
				continue;
			}
			ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
			if (recipient != null) {
				lines.forEach(recipient::sendSystemMessage);
			}
		}
	}

	private static String playerLabel(MinecraftServer server, UUID playerId) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		return player == null ? playerId.toString().substring(0, 8) : player.getName().getString();
	}

	private String waitingMessage() {
		return "[golf] waiting for golfers: " + activeRound.terminalActiveParticipantCount()
			+ "/" + activeRound.activeParticipantCount() + " complete";
	}

	private void sendBarrierMessage(ServerPlayer player) {
		if (!activeRound.allActiveTerminal()) {
			player.sendSystemMessage(Component.literal(waitingMessage()));
			return;
		}
		Component action = Component.literal("[Go to next tee]")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/golf nexthole"))
				.withHoverEvent(new HoverEvent.ShowText(
					Component.literal("Advance every active golfer"))));
		player.sendSystemMessage(Component.literal("[golf] all golfers complete — ").append(action));
	}

	private void sendCompletion(ServerPlayer player, PlayerHoleState state) {
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

	private record PreparedAttempt(
		ServerPlayer player,
		PlayerHoleSession oldSession,
		GolfBallEntity ball
	) {
	}

	public record StartResult(boolean success, String message) {
		public StartResult {
			Objects.requireNonNull(message, "message");
		}
	}
}
