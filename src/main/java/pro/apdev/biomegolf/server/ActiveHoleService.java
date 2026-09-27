package pro.apdev.biomegolf.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.ball.PracticeBallCleanup;
import pro.apdev.biomegolf.block.GolfBlocks;
import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.CourseScorecard;
import pro.apdev.biomegolf.course.HoleScore;
import pro.apdev.biomegolf.course.PlayerCourseState;
import pro.apdev.biomegolf.club.ClubDefinition;
import pro.apdev.biomegolf.club.GolfClubs;
import pro.apdev.biomegolf.entity.GolfBallEntities;
import pro.apdev.biomegolf.entity.GolfBallEntity;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.CupDetector;
import pro.apdev.biomegolf.hole.HazardDropSearch;
import pro.apdev.biomegolf.hole.HazardFluidCrossing;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;
import pro.apdev.biomegolf.hole.HoleLifecycle;
import pro.apdev.biomegolf.hole.HoleScoringDisplay;
import pro.apdev.biomegolf.hole.PenaltyType;
import pro.apdev.biomegolf.hole.PlayerHoleSession;
import pro.apdev.biomegolf.hole.PlayerHoleState;
import pro.apdev.biomegolf.hole.TapInRules;
import pro.apdev.biomegolf.item.GolfItems;
import pro.apdev.biomegolf.net.HoleStateNetworking;
import pro.apdev.biomegolf.net.HoleStatePayload;
import pro.apdev.biomegolf.net.RoundScorecardNetworking;
import pro.apdev.biomegolf.net.RoundScorecardPayload;
import pro.apdev.biomegolf.net.CourseListPayload;
import pro.apdev.biomegolf.net.LobbyStatePayload;
import pro.apdev.biomegolf.round.ParticipantStatus;
import pro.apdev.biomegolf.round.ReadyGolfParticipant;
import pro.apdev.biomegolf.round.ReadyGolfRound;
import pro.apdev.biomegolf.round.ReadyGolfRoundRegistry;
import pro.apdev.biomegolf.round.RoundLobbyProjection;
import pro.apdev.biomegolf.round.RoundPhase;
import pro.apdev.biomegolf.server.TravelDestinationSearch.Destination;
import pro.apdev.biomegolf.world.CollisionShapeTop;
import pro.apdev.biomegolf.world.GolfBlockSurfaceResolver;

/** Server-authoritative solo and Ready Golf lifecycle for the selected course. */
public final class ActiveHoleService {

	private static final double PRACTICE_SPAWN_FORWARD = 2.0;
	private static final double PRACTICE_SPAWN_UP = 1.0;

	/** Vertical window (blocks) around a hazard entry point that the drop scan inspects. */
	private static final int DROP_SCAN_BLOCKS_UP = 6;
	private static final int DROP_SCAN_BLOCKS_DOWN = 10;

	/** Classifies a drop column's support block so bunkers and other hazards are avoided. */
	private static final GolfBlockSurfaceResolver DROP_SURFACE_RESOLVER = new GolfBlockSurfaceResolver();

	public enum ShotPermission {
		PRACTICE,
		SCORING,
		WRONG_BALL,
		HOLE_COMPLETE,
		MISSING_BALL
	}

	private static final ActiveHoleService INSTANCE = new ActiveHoleService();

	private final HoleLifecycle lifecycle = new HoleLifecycle();
	private final ReadyGolfRoundRegistry rounds = new ReadyGolfRoundRegistry();
	private final Set<UUID> overspeedCupEntries = new HashSet<>();
	private final Map<UUID, PlayerCourseState> soloCourseStates = new HashMap<>();
	private HoleDefinition hole;
	private CourseDefinition course;

	private ActiveHoleService() {
	}

	/** Clears runtime-only course and round selection when a server stops. */
	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> instance().clearRuntimeState());
	}

	public static ActiveHoleService instance() {
		return INSTANCE;
	}

	private void clearRuntimeState() {
		hole = null;
		course = null;
		clearRounds();
		soloCourseStates.clear();
		lifecycle.clear();
		overspeedCupEntries.clear();
		CourseBlockBreakGuard.clearConfigured();
		MinecraftGolf.LOGGER.info("Cleared runtime golf course selection and round state");
	}

	public void initialize(HoleDefinition configuredHole) {
		if (hasActivePlay()) throw new IllegalStateException("cannot reconfigure golf while play is active");
		hole = Objects.requireNonNull(configuredHole, "configuredHole");
		course = null;
		clearRounds();
		soloCourseStates.clear();
		lifecycle.clear();
		overspeedCupEntries.clear();
		CourseBlockBreakGuard.replaceConfiguredHole(configuredHole);
		MinecraftGolf.LOGGER.info("Loaded hole {} (#{} par {}, Double Par + 2 limit {}) in {}",
			hole.id(), hole.number(), hole.par(), hole.strokeLimit(), hole.dimension());
	}

	public void initializeCourse(CourseDefinition configuredCourse) {
		if (hasActivePlay()) throw new IllegalStateException("cannot switch courses while play is active");
		course = Objects.requireNonNull(configuredCourse, "configuredCourse");
		hole = course.hole(1);
		clearRounds();
		soloCourseStates.clear();
		lifecycle.clear();
		overspeedCupEntries.clear();
		CourseBlockBreakGuard.replaceConfiguredCourse(configuredCourse);
		MinecraftGolf.LOGGER.info("Loaded course {} ({} holes, par {}) using layout {} v{}",
			course.id(), course.holes().size(), course.totalPar(), course.generatedLayout().id(),
			course.generatedLayout().version());
	}

	private void clearRounds() {
		for (ReadyGolfRound round : rounds.list()) {
			for (ReadyGolfParticipant participant : round.participants()) {
				lifecycle.abandon(participant.playerId());
			}
		}
		// Runtime rounds are intentionally not persistent; withdraw all members.
		for (ReadyGolfRound round : rounds.list()) {
			for (ReadyGolfParticipant participant : round.participants()) {
				if (participant.isParticipating()) rounds.leave(round.roundId(), participant.playerId());
			}
		}
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

	public void clearConfiguredCourseIf(String courseId) {
		if (hasActivePlay()) throw new IllegalStateException("cannot clear a configured course while play is active");
		if (course != null && course.id().equals(courseId)) {
			course = null;
			hole = null;
			CourseBlockBreakGuard.clearConfigured();
		}
	}

	/**
	 * True while any play is in flight: a non-complete Ready Golf lobby/round or
	 * any player's solo hole attempt. Used to gate course selection and deletion.
	 */
	public boolean hasActivePlay() {
		return rounds.hasActivePlay()
			|| lifecycle.hasAnySession();
	}

	/** The currently configured hole, or {@code null} before any initialization. */
	public HoleDefinition configuredHoleOrNull() {
		return hole;
	}

	public StartResult createRound(ServerPlayer creator) {
		CourseDefinition selectedCourse = selectedOrDefaultCourse();
		if (selectedCourse == null) {
			return reject(creator, "/golf round create", "[golf] no configured course is available");
		}
		if (rounds.findByPlayer(creator.getUUID()).isPresent() || lifecycle.session(creator.getUUID()).isPresent()) {
			return reject(creator, "/golf round create", "[golf] leave your current round before creating another");
		}
		if (soloCourseStates.containsKey(creator.getUUID())) {
			return reject(creator, "/golf round create", "[golf] leave your solo round before creating a Ready Golf round");
		}
		ReadyGolfRound created;
		try {
			created = rounds.create(selectedCourse, creator.getUUID());
		} catch (IllegalStateException exception) {
			return reject(creator, "/golf round create", "[golf] " + exception.getMessage());
		}
		MinecraftGolf.LOGGER.info("{} created Ready Golf lobby {}",
			creator.getName().getString(), created.roundId());
		broadcastLobbyState(creator.level().getServer());
		return new StartResult(true, "[golf] Ready Golf lobby created (" + created.roundId()
			+ "); other golfers may use /golf round join " + created.roundId());
	}

	/** Creates a round from a finalized course without changing operator selection. */
	public StartResult createRound(String courseId, ServerPlayer creator) {
		final CourseDefinition selected;
		try {
			selected = AuthoredCourseService.instance().store().finalizedCourse(courseId);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return reject(creator, "round browser", "[golf] unavailable course: " + exception.getMessage());
		}
		if (rounds.findByPlayer(creator.getUUID()).isPresent()
				|| lifecycle.session(creator.getUUID()).isPresent() || soloCourseStates.containsKey(creator.getUUID())) {
			return reject(creator, "round browser", "[golf] finish or leave your current golf round first");
		}
		ReadyGolfRound created;
		try {
			created = rounds.create(selected, creator.getUUID());
		} catch (IllegalStateException exception) {
			return reject(creator, "round browser", "[golf] " + exception.getMessage());
		}
		broadcastLobbyState(creator.level().getServer());
		return new StartResult(true, "[golf] Ready Golf lobby created for " + selected.displayName()
			+ " (" + created.roundId() + "); join with /golf round join " + created.roundId());
	}

	public StartResult joinRound(ServerPlayer player) {
		List<ReadyGolfRound> open = rounds.listOpen();
		if (open.size() != 1) {
			return reject(player, "/golf round join", open.isEmpty()
				? "[golf] no open Ready Golf lobby; use /golf round list or /golf round create"
				: "[golf] multiple open Ready Golf lobbies; use /golf round list and choose one");
		}
		return joinRound(open.getFirst().roundId(), player);
	}

	private StartResult joinRoundLobby(UUID roundId, ServerPlayer player) {
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return reject(player, "/golf round join", "[golf] leave your current round before joining another");
		}
		if (soloCourseStates.containsKey(player.getUUID())) {
			return reject(player, "/golf round join", "[golf] leave your solo round before joining another");
		}
		ReadyGolfRound current = rounds.find(roundId).orElse(null);
		if (current == null) return reject(player, "/golf round join", "[golf] that round no longer exists; use /golf round list or /golf round create");
		ReadyGolfRound joined;
		try {
			joined = rounds.join(roundId, player.getUUID());
		} catch (IllegalStateException exception) {
			return reject(player, "/golf round join", "[golf] " + exception.getMessage());
		}
		MinecraftGolf.LOGGER.info("{} joined Ready Golf lobby {}",
			player.getName().getString(), joined.roundId());
		broadcastLobbyState(player.level().getServer());
		return new StartResult(true, "[golf] joined Ready Golf lobby ("
			+ joined.participants().size() + "/" + ReadyGolfRound.MAX_PARTICIPANTS + ")");
	}

	/** Joins the explicitly identified lobby; the UUID prevents same-course ambiguity. */
	public StartResult joinRound(UUID roundId, ServerPlayer player) {
		ReadyGolfRound target = rounds.find(roundId).orElse(null);
		if (target == null) {
			return reject(player, "/golf round join <roundId>",
				"[golf] that round no longer exists; use /golf round list or /golf round create");
		}
		if (target.phase() == RoundPhase.COMPLETE) {
			return reject(player, "/golf round join <roundId>",
				"[golf] that round is complete; use /golf round list or create another round");
		}
		if (target.phase() != RoundPhase.LOBBY) {
			return reject(player, "/golf round join <roundId>",
				"[golf] that round already started; late joining is disabled; use /golf round list or create another round");
		}
		if (target.activeParticipantCount() >= ReadyGolfRound.MAX_PARTICIPANTS) {
			return reject(player, "/golf round join <roundId>",
				"[golf] that lobby is full; use /golf round list or create another round");
		}
		return joinRoundLobby(roundId, player);
	}

	public RoundLobbyProjection roundProjection(MinecraftServer server) {
		return RoundLobbyProjection.from(AuthoredCourseService.instance().store().finalizedCourses(),
			rounds.listOpen(), playerId -> playerLabel(server, playerId));
	}

	public StartResult startRound(ServerPlayer coordinator) {
		ReadyGolfRound lobby = rounds.findByPlayer(coordinator.getUUID()).orElse(null);
		if (lobby == null || lobby.phase() != RoundPhase.LOBBY) {
			return reject(coordinator, "/golf round start", "[golf] create or join a lobby before starting");
		}
		ReadyGolfRound started;
		try {
			started = lobby.start(coordinator.getUUID());
		} catch (IllegalStateException exception) {
			return reject(coordinator, "/golf round start", "[golf] " + exception.getMessage());
		}
		CourseDefinition roundCourse = lobby.course();
		HoleDefinition firstHole = roundCourse.hole(1);
		List<PreparedAttempt> prepared = prepareRoundStart(coordinator, started, firstHole);
		if (prepared == null) {
			return new StartResult(false,
				"[golf] round start cancelled; every lobby golfer must be online and ready");
		}
		ReadyGolfRound committed;
		try {
			committed = rounds.update(lobby.roundId(), lobby, started);
		} catch (IllegalStateException exception) {
			discardPreparedAttempts(prepared);
			return reject(coordinator, "/golf round start", "[golf] lobby changed while starting; try again");
		}
		commitRoundStart(prepared, firstHole);
		MinecraftGolf.LOGGER.info("Started Ready Golf round {} with {} golfers",
			committed.roundId(), committed.participants().size());
		broadcastLobbyState(coordinator.level().getServer());
		return new StartResult(true, "[golf] Ready Golf started with "
			+ started.participants().size() + " golfer(s)");
	}

	public StartResult startRound(UUID roundId, ServerPlayer coordinator) {
		ReadyGolfRound ownRound = rounds.findByPlayer(coordinator.getUUID()).orElse(null);
		if (ownRound == null || !ownRound.roundId().equals(roundId)) {
			return reject(coordinator, "round browser",
				"[golf] that lobby is no longer your active lobby; refresh and try again");
		}
		return startRound(coordinator);
	}

	public StartResult leaveRound(ServerPlayer player) {
		UUID playerId = player.getUUID();
		ReadyGolfRound playerRound = rounds.findByPlayer(playerId).orElse(null);
		ReadyGolfParticipant participant = playerRound == null ? null
			: playerRound.findParticipant(playerId).orElse(null);
		if (participant == null) {
			PlayerCourseState solo = soloCourseStates.get(playerId);
			boolean completed = solo != null && solo.isComplete();
			boolean cleaned = cleanupPlayerState(player);
			sendSnapshot(player, practiceSnapshot());
			sendLobbyState(player);
			if (cleaned) sendPracticeEntryPoint(player);
			if (completed) teleportToWorldSpawn(player);
			return new StartResult(true, cleaned
				? "[golf] left the golf round; practice shots are available"
				: "[golf] no golf round to leave; practice shots are available");
		}
		if (participant.status() != ParticipantStatus.ACTIVE) {
			cleanupPlayerState(player);
			rounds.leave(playerRound.roundId(), playerId);
			sendSnapshot(player, practiceSnapshot());
			sendLobbyState(player);
			return new StartResult(true, "[golf] no active golf round to leave; practice shots are available");
		}
		boolean completed = playerRound.phase() == RoundPhase.COMPLETE;
		Optional<PlayerHoleSession> session = lifecycle.session(player.getUUID());
		session.ifPresent(current -> discardAssignedBall(player, current));
		lifecycle.abandon(player.getUUID());
		rounds.leave(playerRound.roundId(), playerId);
		sendSnapshot(player, practiceSnapshot());
		broadcastLobbyState(player.level().getServer());
		if (!completed) notifyTerminalBarrier(player.level().getServer(), playerRound.roundId(), null);
		sendPracticeEntryPoint(player);
		if (completed) teleportToWorldSpawn(player);
		MinecraftGolf.LOGGER.info("{} left Ready Golf round", player.getName().getString());
		return new StartResult(true, completed
			? "[golf] left the completed round and returned to world spawn"
			: "[golf] left the Ready Golf round; practice shots are available");
	}

	public StartResult leaveRound(UUID roundId, ServerPlayer player) {
		ReadyGolfRound ownRound = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (ownRound == null || !ownRound.roundId().equals(roundId)) {
			return reject(player, "round browser",
				"[golf] that round is no longer your active round; refresh and try again");
		}
		return leaveRound(player);
	}

	public String roundStatus(ServerPlayer player) {
		ReadyGolfRound round = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (round == null) {
			return "[golf] no active Ready Golf lobby or round";
		}
		long active = round.participants().stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE).count();
		return "[golf] Ready Golf " + round.phase() + " | round " + round.roundId()
			+ " | course " + round.course().id() + " | " + active + "/"
			+ round.participants().size() + " golfers"
			+ (round.phase() == RoundPhase.COMPLETE ? "" : " | Hole " + (round.currentHoleIndex() + 1))
			+ (round.phase() == RoundPhase.COMPLETE ? " | use /golf round restart or /golf round leave" : "");
	}

	/** Sends finalized, display-safe course metadata to a browser client. */
	public void sendFinalizedCourses(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, CourseListPayload.TYPE)) {
			ServerPlayNetworking.send(player, CourseListPayload.from(roundProjection(player.level().getServer())));
		}
	}

	/** Validates that the caller is in a state where the course browser is useful. */
	public StartResult openCourseBrowser(ServerPlayer player) {
		ReadyGolfRound round = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (round != null && round.phase() != RoundPhase.LOBBY) {
			return reject(player, "golf menu", "[golf] finish or leave the active round before browsing courses");
		}
		if (lifecycle.session(player.getUUID()).isPresent() || soloCourseStates.containsKey(player.getUUID())) {
			return reject(player, "golf menu", "[golf] finish, replay, or leave the current round first");
		}
		sendFinalizedCourses(player);
		return new StartResult(true, "[golf] opening finalized course browser");
	}

	/** Broadcasts the current authoritative lobby state, including a clear state. */
	public void broadcastLobbyState(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) sendLobbyState(player);
	}

	private void sendLobbyState(ServerPlayer player) {
		LobbyStatePayload payload = LobbyStatePayload.none();
		ReadyGolfRound round = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (round != null) {
			boolean participating = round.findParticipant(player.getUUID())
				.map(participant -> participant.status() == ParticipantStatus.ACTIVE).orElse(false);
			boolean coordinator = round.coordinatorId().map(player.getUUID()::equals).orElse(false);
			payload = new LobbyStatePayload(
				switch (round.phase()) {
					case LOBBY -> LobbyStatePayload.Phase.LOBBY;
					case PLAYING -> LobbyStatePayload.Phase.PLAYING;
					case COMPLETE -> LobbyStatePayload.Phase.COMPLETE;
				},
				round.roundId(), round.course().id(), round.course().displayName(),
				(int) round.activeParticipantCount(), ReadyGolfRound.MAX_PARTICIPANTS,
				participating, coordinator, coordinator && round.phase() == RoundPhase.LOBBY);
		}
		if (ServerPlayNetworking.canSend(player, LobbyStatePayload.TYPE)) ServerPlayNetworking.send(player, payload);
	}

	public StartResult start(ServerPlayer player) {
		CourseDefinition selectedCourse = selectedOrDefaultCourse();
		if (selectedCourse != null) {
			return start(player, selectedCourse, 1, "/golf hole start");
		}
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return new StartResult(false,
				"[golf] a hole attempt already exists; use /golf hole restart or /golf round leave");
		}
		if (hole == null) {
			return reject(player, "/golf hole start",
				"[golf] no course is selected or configured as default; use /golf course play <courseId>");
		}
		return createAttempt(player, false, configuredHole());
	}

	public StartResult start(ServerPlayer player, int holeNumber) {
		CourseDefinition selectedCourse = selectedOrDefaultCourse();
		if (selectedCourse == null) {
			return reject(player, "/golf hole start <hole>",
				"[golf] no course is selected or configured as default; use /golf course play <courseId> <hole>");
		}
		return start(player, selectedCourse, holeNumber, "/golf hole start <hole>");
	}

	public StartResult start(String courseId, int holeNumber, ServerPlayer player) {
		if (rounds.findByPlayer(player.getUUID()).isPresent()) {
			return reject(player, "/golf course play",
				"[golf] leave the Ready Golf lobby or round with /golf round leave before starting solo play");
		}
		CourseDefinition selectedCourse;
		try {
			selectedCourse = AuthoredCourseService.instance().store().finalizedCourse(courseId);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return reject(player, "/golf course play", "[golf] " + exception.getMessage());
		}
		PlayerCourseState replacementState;
		try {
			replacementState = PlayerCourseState.start(selectedCourse, holeNumber);
		} catch (IllegalArgumentException exception) {
			return reject(player, "/golf course play", "[golf] " + exception.getMessage());
		}

		PreparedReplacement prepared = prepareReplacement(player, replacementState.currentHole().hole());
		if (prepared == null) {
			return new StartResult(false, "[golf] course play cancelled; the current attempt is unchanged");
		}
		PlayerCourseState previousState = soloCourseStates.get(player.getUUID());
		PlayerHoleSession previousSession = lifecycle.session(player.getUUID()).orElse(null);
		boolean replaced = previousState != null || previousSession != null;
		commitReplacement(prepared, replacementState);
		String message = "[golf] started " + selectedCourse.id() + " Hole " + holeNumber;
		if (replaced) {
			String oldCourse = previousState == null ? courseIdFromHole(previousSession.state().hole())
				: previousState.course().id();
			int oldHole = previousSession == null ? previousState.currentHole().hole().number()
				: previousSession.state().hole().number();
			message = "[golf] abandoned " + oldCourse + " Hole " + oldHole
				+ "; started " + selectedCourse.id() + " Hole " + holeNumber;
			MinecraftGolf.LOGGER.info("{} replaced solo {} Hole {} with {} Hole {}",
				player.getName().getString(), oldCourse, oldHole, selectedCourse.id(), holeNumber);
		}
		return new StartResult(true, message);
	}

	private static String courseIdFromHole(HoleDefinition definition) {
		String id = definition.id();
		int separator = id.lastIndexOf(':');
		return separator > 0 ? id.substring(0, separator) : id;
	}

	private PreparedReplacement prepareReplacement(ServerPlayer player, HoleDefinition definition) {
		Optional<String> issue = attemptPreflight(player, definition);
		if (issue.isPresent()) {
			MinecraftGolf.LOGGER.warn("Solo course replacement preflight failed: {}", issue.orElseThrow());
			return null;
		}
		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(player.level(), EntitySpawnReason.COMMAND);
		if (ball == null) {
			return null;
		}
		ball.setOwner(player.getUUID());
		ball.placeAtRest(definition.tee());
		if (!player.level().addFreshEntity(ball)) {
			ball.discard();
			return null;
		}
		return new PreparedReplacement(player, lifecycle.session(player.getUUID()).orElse(null), ball);
	}

	private void commitReplacement(PreparedReplacement prepared, PlayerCourseState replacementState) {
		ServerPlayer player = prepared.player();
		HoleDefinition definition = replacementState.currentHole().hole();
		if (prepared.oldSession() != null) {
			discardAssignedBall(player, prepared.oldSession());
		}
		lifecycle.abandon(player.getUUID());
		soloCourseStates.remove(player.getUUID());
		BlockPos cupBlockPos = BlockPos.containing(
			definition.cup().x(), definition.cup().y() - GolfBallEntity.BALL_RADIUS, definition.cup().z());
		player.level().setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
		placeFlag(player.level(), cupBlockPos);
		soloCourseStates.put(player.getUUID(), replacementState);
		lifecycle.start(player.getUUID(), definition, prepared.ball().getUUID());
		discardPlayerOwnedBalls(player, prepared.ball().getUUID());
		teleportToTransition(player, definition);
		grantMissingClubs(player);
		PlayerHoleState started = lifecycle.session(player.getUUID()).orElseThrow().state();
		sendActiveSnapshot(player, started, definition.tee());
	}

	private StartResult start(ServerPlayer player, CourseDefinition selectedCourse, int holeNumber,
			String command) {
		if (lifecycle.session(player.getUUID()).isPresent()) {
			return reject(player, command,
				"[golf] a hole attempt already exists; use /golf hole restart or /golf round leave");
		}
		PlayerCourseState existing = courseState(player.getUUID());
		if (existing != null) {
			return reject(player, command, existing.isComplete()
				? "[golf] course complete; use /golf round restart to replay"
				: "[golf] course recovery needed; use /golf hole restart");
		}
		if (rounds.findByPlayer(player.getUUID()).isPresent()) {
			return reject(player, command,
				"[golf] a Ready Golf lobby or round already exists; use /golf round join");
		}
		PlayerCourseState started;
		try {
			started = PlayerCourseState.start(selectedCourse, holeNumber);
		} catch (IllegalArgumentException exception) {
			return reject(player, command, "[golf] " + exception.getMessage());
		}
		soloCourseStates.put(player.getUUID(), started);
		StartResult result = createAttempt(player, false, started.currentHole().hole());
		if (!result.success()) {
			soloCourseStates.remove(player.getUUID());
		}
		return result;
	}

	private CourseDefinition selectedOrDefaultCourse() {
		if (course != null) {
			return course;
		}
		try {
			AuthoredCourseService service = AuthoredCourseService.instance();
			return service.store().defaultCourseId()
				.map(id -> service.store().finalizedCourse(id)).orElse(null);
		} catch (IllegalStateException exception) {
			return null;
		}
	}

	public StartResult dropPracticeBall(ServerPlayer player) {
		if (!lifecycle.allowsPracticeBall(player.getUUID())) {
			return new StartResult(false,
				"[golf] finish, replay, or leave the current round before dropping a practice ball");
		}
		ServerLevel level = player.level();
		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(level, EntitySpawnReason.COMMAND);
		if (ball == null) {
			return new StartResult(false, "[golf] failed to create a practice ball");
		}
		double yaw = Math.toRadians(player.getYRot());
		ball.setOwner(player.getUUID());
		ball.markAsPracticeBall();
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

	/** Removes only this player's explicitly marked practice balls; assigned balls are always preserved. */
	public StartResult clearPracticeBalls(ServerPlayer player) {
		UUID playerId = player.getUUID();
		UUID assignedBall = lifecycle.session(playerId).map(PlayerHoleSession::ballUuid).orElse(null);
		int removed = 0;
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (GolfBallEntity ball : level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class),
				candidate -> PracticeBallCleanup.shouldClear(
					playerId, candidate.owner(), candidate.isPracticeBall(), assignedBall, candidate.getUUID()))) {
				overspeedCupEntries.remove(ball.getUUID());
				ball.discard();
				removed++;
			}
		}
		String message = "[golf] cleared " + removed + " of your practice ball"
			+ (removed == 1 ? "" : "s");
		return new StartResult(true, message);
	}

	/** Resets the current golf set into hotbar slots 0 through 6. */
	public StartResult equipClubs(ServerPlayer player) {
		boolean modded = canResolveCustomItems(player);
		List<ItemStack> displaced = new ArrayList<>();
		for (int slot = 0; slot < 7; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && GolfItems.clubOf(stack) == null) {
				displaced.add(stack.copy());
			}
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
		for (int slot = 7; slot < player.getInventory().getContainerSize(); slot++) {
			if (GolfItems.clubOf(player.getInventory().getItem(slot)) != null) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
		for (int slot = 0; slot < GolfClubs.ALL.size(); slot++) {
			player.getInventory().setItem(slot,
				GolfItems.stackFor(GolfClubs.ALL.get(slot), modded));
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
		UUID playerId = player.getUUID();
		ReadyGolfRound playerRound = rounds.findByPlayer(playerId).orElse(null);
		ReadyGolfParticipant participant = playerRound == null ? null
			: playerRound.findParticipant(playerId)
				.filter(value -> value.status() == ParticipantStatus.ACTIVE)
				.orElse(null);
		PlayerCourseState current = participant == null
			? soloCourseStates.get(playerId)
			: participant.courseState();
		if (current != null) {
			if (current.isComplete()) {
				return new StartResult(false, "[golf] the round is complete; use /golf round restart to replay it");
			}
			ReadyGolfRound restartedRound = null;
			PlayerCourseState restartedSolo = null;
			try {
				if (participant != null) restartedRound = playerRound.restartCurrentHole(playerId);
				else restartedSolo = current.restartCurrentHole();
			} catch (IllegalStateException exception) {
				return reject(player, "/golf hole restart", "[golf] " + exception.getMessage());
			}
			HoleDefinition definition = current.currentHole().hole();
			PreparedRestart prepared = prepareRestart(player, definition);
			if (prepared == null) {
				return new StartResult(false, "[golf] hole restart cancelled; the current hole is unchanged");
			}
			if (participant != null) rounds.update(playerRound.roundId(), playerRound, restartedRound);
			else soloCourseStates.put(playerId, restartedSolo);
			commitRestart(prepared, definition);
			return new StartResult(true, "[golf] replayed Hole " + definition.number());
		}
		if (lifecycle.session(player.getUUID()).isEmpty()) {
			return new StartResult(false, "[golf] no hole attempt to restart; use /golf hole start");
		}
		HoleDefinition definition = lifecycle.session(playerId).orElseThrow().state().hole();
		PreparedRestart prepared = prepareRestart(player, definition);
		if (prepared == null) {
			return new StartResult(false, "[golf] hole restart cancelled; the current hole is unchanged");
		}
		commitRestart(prepared, definition);
		return new StartResult(true, "[golf] replayed Hole " + definition.number());
	}

	public StartResult restart(UUID roundId, ServerPlayer player) {
		ReadyGolfRound ownRound = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (ownRound == null || !ownRound.roundId().equals(roundId)) {
			return reject(player, "golf menu",
				"[golf] that round is no longer your active round; refresh and try again");
		}
		return restart(player);
	}

	/** Restarts the completed shared round for every participant who remained in it. */
	public StartResult restartRound(ServerPlayer player) {
		ReadyGolfRound playerRound = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (playerRound == null) {
			PlayerCourseState completed = soloCourseStates.get(player.getUUID());
			if (completed == null || !completed.isComplete()) {
				return new StartResult(false, "[golf] the round is not complete; use /golf hole restart for the current hole");
			}
			PlayerCourseState restarted = completed.reset();
			HoleDefinition firstHole = restarted.currentHole().hole();
			PreparedRestart prepared = prepareRestart(player, firstHole);
			if (prepared == null) {
				return new StartResult(false, "[golf] round restart cancelled; Hole "
					+ firstHole.number() + " could not be prepared");
			}
			soloCourseStates.put(player.getUUID(), restarted);
			commitRestart(prepared, firstHole);
			return new StartResult(true, "[golf] round restarted at Hole " + firstHole.number());
		}
		if (playerRound.phase() != RoundPhase.COMPLETE) {
			return new StartResult(false, "[golf] the round is not complete; use /golf hole restart for the current hole");
		}
		ReadyGolfParticipant caller = playerRound.findParticipant(player.getUUID()).orElse(null);
		if (caller == null || caller.status() != ParticipantStatus.ACTIVE) {
			return new StartResult(false, "[golf] you are not an active participant in the completed round");
		}

		HoleDefinition firstHole = playerRound.course().holes().getFirst();
		List<PreparedAttempt> prepared = prepareRoundReplay(player, playerRound, firstHole);
		if (prepared == null) {
			return new StartResult(false, "[golf] round restart cancelled; every remaining golfer must be online and ready for Hole 1");
		}

		ReadyGolfRound replayed = playerRound.replayRemaining();
		rounds.update(playerRound.roundId(), playerRound, replayed);
		for (PreparedAttempt attempt : prepared) {
			discardAssignedBall(attempt.player(), attempt.oldSession());
			lifecycle.restart(attempt.player().getUUID(), firstHole, attempt.ball().getUUID());
			discardPlayerOwnedBalls(attempt.player(), attempt.ball().getUUID());
			teleportToTransition(attempt.player(), firstHole);
			grantMissingClubs(attempt.player());
			PlayerHoleState startedState = lifecycle.session(attempt.player().getUUID())
				.map(PlayerHoleSession::state).orElseThrow();
			sendActiveSnapshot(attempt.player(), startedState, firstHole.tee());
		}
		broadcastLobbyState(player.level().getServer());
		MinecraftGolf.LOGGER.info("{} restarted Ready Golf round {} for {} remaining golfers",
			player.getName().getString(), replayed.roundId(), prepared.size());
		return new StartResult(true, "[golf] round restarted at Hole 1 for " + prepared.size() + " golfer(s)");
	}

	public StartResult restartRound(UUID roundId, ServerPlayer player) {
		ReadyGolfRound ownRound = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (ownRound == null || !ownRound.roundId().equals(roundId)) {
			return reject(player, "golf menu",
				"[golf] that round is no longer your completed round; refresh and try again");
		}
		return restartRound(player);
	}

	private PreparedRestart prepareRestart(ServerPlayer player, HoleDefinition definition) {
		Optional<String> issue = attemptPreflight(player, definition);
		if (issue.isPresent()) {
			MinecraftGolf.LOGGER.warn("Hole restart preflight failed: {}", issue.orElseThrow());
			return null;
		}
		ServerLevel level = player.level();
		BlockPos cupBlockPos = BlockPos.containing(
			definition.cup().x(), definition.cup().y() - GolfBallEntity.BALL_RADIUS, definition.cup().z());
		level.setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
		placeFlag(level, cupBlockPos);
		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(level, EntitySpawnReason.COMMAND);
		if (ball == null) return null;
		ball.setOwner(player.getUUID());
		ball.placeAtRest(definition.tee());
		if (!level.addFreshEntity(ball)) {
			ball.discard();
			return null;
		}
		return new PreparedRestart(player, lifecycle.session(player.getUUID()).orElse(null), ball);
	}

	private void commitRestart(PreparedRestart prepared, HoleDefinition definition) {
		if (prepared.oldSession() == null) {
			lifecycle.start(prepared.player().getUUID(), definition, prepared.ball().getUUID());
		} else {
			discardAssignedBall(prepared.player(), prepared.oldSession());
			lifecycle.restart(prepared.player().getUUID(), definition, prepared.ball().getUUID());
		}
		discardPlayerOwnedBalls(prepared.player(), prepared.ball().getUUID());
		teleportToTransition(prepared.player(), definition);
		grantMissingClubs(prepared.player());
		PlayerHoleState startedState = lifecycle.session(prepared.player().getUUID())
			.map(PlayerHoleSession::state).orElseThrow();
		sendActiveSnapshot(prepared.player(), startedState, definition.tee());
	}

	private List<PreparedAttempt> prepareRoundReplay(ServerPlayer serverContext, ReadyGolfRound round,
			HoleDefinition definition) {
		List<PreparedAttempt> prepared = new ArrayList<>();
		MinecraftServer server = serverContext.level().getServer();
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.status() != ParticipantStatus.ACTIVE) {
				continue;
			}
			ServerPlayer golfer = server.getPlayerList().getPlayer(participant.playerId());
			PlayerHoleSession oldSession = lifecycle.session(participant.playerId()).orElse(null);
			if (golfer == null || oldSession == null) {
				discardPreparedAttempts(prepared);
				return null;
			}
			Optional<String> issue = attemptPreflight(golfer, definition);
			if (issue.isPresent()) {
				MinecraftGolf.LOGGER.warn("Ready Golf round replay preflight failed: {}", issue.orElseThrow());
				discardPreparedAttempts(prepared);
				return null;
			}
			GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(golfer.level(), EntitySpawnReason.COMMAND);
			if (ball == null) {
				discardPreparedAttempts(prepared);
				return null;
			}
			ball.setOwner(participant.playerId());
			ball.placeAtRest(definition.tee());
			if (!golfer.level().addFreshEntity(ball)) {
				ball.discard();
				discardPreparedAttempts(prepared);
				return null;
			}
			prepared.add(new PreparedAttempt(golfer, oldSession, ball));
		}
		return prepared;
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

	private List<PreparedAttempt> prepareRoundStart(ServerPlayer serverContext, ReadyGolfRound started,
			HoleDefinition definition) {
		List<PreparedAttempt> prepared = new ArrayList<>();
		MinecraftServer server = serverContext.level().getServer();
		for (ReadyGolfParticipant participant : started.participants()) {
			ServerPlayer golfer = server.getPlayerList().getPlayer(participant.playerId());
			if (golfer == null || attemptPreflight(golfer, definition).isPresent()) {
				discardPreparedAttempts(prepared);
				return null;
			}
			BlockPos cupBlockPos = BlockPos.containing(definition.cup().x(),
				definition.cup().y() - GolfBallEntity.BALL_RADIUS, definition.cup().z());
			golfer.level().setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
			placeFlag(golfer.level(), cupBlockPos);
			GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(golfer.level(), EntitySpawnReason.COMMAND);
			if (ball == null) {
				discardPreparedAttempts(prepared);
				return null;
			}
			ball.setOwner(participant.playerId());
			ball.placeAtRest(definition.tee());
			if (!golfer.level().addFreshEntity(ball)) {
				ball.discard();
				discardPreparedAttempts(prepared);
				return null;
			}
			prepared.add(new PreparedAttempt(golfer, null, ball));
		}
		return prepared;
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
		loadPointChunk(level, definition.tee());
		loadPointChunk(level, definition.cup());
		Optional<String> terrainIssue = validateTerrain(level, definition);
		if (terrainIssue.isPresent()) {
			return new StartResult(false, "[golf] unsafe configured hole: " + terrainIssue.orElseThrow()
				+ "; prepare the terrain or update config/minecraft_golf/hole.json");
		}

		BlockPos cupBlockPos = BlockPos.containing(
			definition.cup().x(), definition.cup().y() - GolfBallEntity.BALL_RADIUS, definition.cup().z());
		level.setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
		placeFlag(level, cupBlockPos);

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
		sendLobbyState(player);
		Optional<PlayerHoleSession> currentSession = lifecycle.session(player.getUUID());
		if (currentSession.isEmpty()) {
			if (hole == null) {
				return "[golf] no active course selected; an operator must use /golf course select <id>";
			}
			PlayerCourseState courseState = courseState(player.getUUID());
			if (courseState != null && courseState.isComplete()) {
				CourseScorecard scorecard = courseState.finalScorecard();
				return "[golf] course complete | " + scorecard.totalStrokes() + " strokes | "
					+ formatToPar(scorecard.scoreToPar()) + " through " + scorecard.holes().size() + " holes";
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
		HoleScoringDisplay display = HoleScoringDisplay.from(state, courseState(player.getUUID()));
		return "[golf] Hole " + state.hole().number() + " | strokes " + state.strokes()
			+ "/" + state.hole().strokeLimit() + " | " + display.holeText()
			+ (state.penaltyStrokes() == 0 ? "" : " | penalties " + state.penaltyStrokes())
			+ display.courseText() + result;
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
		player.sendSystemMessage(Component.literal(HoleScoringDisplay.from(updated,
			courseState(player.getUUID())).actionBarText()), true);
		if (updated.isComplete()) {
			sendCompletion(player, updated);
			sendCompleteSnapshot(player, updated);
			notifyTerminalBarrierForPlayer(player.level().getServer(), player.getUUID());
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
		if (session == null) {
			sendPracticeShotProgress(ball);
			return;
		}
		if (session.state().isComplete() || !session.ballUuid().equals(ball.getUUID())) {
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
		ServerLevel level = (ServerLevel) ball.level();
		Optional<Vec3> hazardContact = HazardFluidCrossing.firstContact(
			from, to, GolfBallEntity.BALL_RADIUS,
			(x, y, z) -> isHazardFluid(level, BlockPos.containing(x, y, z)));
		if (hazardContact.isPresent()) {
			Vec3 entry = hazardContact.orElseThrow();
			Vec3 recovery = waterDropTarget(level, session, from, entry);
			applyPenalty(ball, owner, session, PenaltyType.WATER, recovery);
			return;
		}

		Vec3 cup = session.state().hole().cup();
		double speed = ball.ballState() == null ? 0.0 : ball.ballState().velocity().length();
		if (overspeedCupEntries.contains(ball.getUUID())) {
			if (!CupDetector.contains(to, cup)) {
				overspeedCupEntries.remove(ball.getUUID());
			}
			sendShotProgress(ball, session, to);
			return;
		}
		if (speed > CupDetector.MAX_ENTRY_SPEED && CupDetector.intersects(from, to, cup)) {
			overspeedCupEntries.add(ball.getUUID());
			MinecraftGolf.LOGGER.info("Golf ball {} crossed cup {} too fast at {} blocks/tick",
				ball.getUUID(), session.state().hole().id(), speed);
			sendShotProgress(ball, session, to);
			return;
		}
		if (!CupDetector.entered(from, to, cup, speed)) {
			sendShotProgress(ball, session, to);
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
			notifyTerminalBarrierForPlayer(player.level().getServer(), owner);
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
		if (session == null) {
			sendPracticeShotProgress(ball);
			return;
		}
		if (session.state().isComplete() || !session.ballUuid().equals(ball.getUUID())) {
			return;
		}
		if (!isActiveParticipant(owner)) {
			return;
		}
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player == null) {
			return;
		}
		sendActiveSnapshot(player, session.state(), ball.ballState().position(),
			ball.shotDistanceBlocks(), false);
		StartResult travel = travelToNextShot(player, ball);
		player.sendSystemMessage(Component.literal(travel.message()));
	}

	/** Applies a penalty that recovers to the previous-shot position. */
	private void applyPenalty(GolfBallEntity ball, UUID owner, PlayerHoleSession session, PenaltyType penalty) {
		applyPenalty(ball, owner, session, penalty, session.lastSafePosition());
	}

	private void applyPenalty(GolfBallEntity ball, UUID owner, PlayerHoleSession session,
			PenaltyType penalty, Vec3 recoveryPosition) {
		PlayerHoleSession updatedSession = session.applyPenalty(penalty);
		PlayerHoleState updated = updatedSession.state();
		lifecycle.update(owner, updatedSession);
		updateCourseState(owner, updated);
		overspeedCupEntries.remove(ball.getUUID());
		ball.placeAtRest(recoveryPosition);
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			String label = penalty == PenaltyType.WATER
				? "Water or lava — one penalty stroke; ball dropped at the hazard edge"
				: "Out of Bounds — one penalty stroke; ball returned";
			player.sendSystemMessage(Component.literal("[golf] " + label
				+ " | " + updated.strokes() + "/" + updated.hole().strokeLimit()));
			player.sendSystemMessage(Component.literal(HoleScoringDisplay.from(updated,
				courseState(owner)).actionBarText()), true);
			if (updated.isComplete()) {
				sendCompletion(player, updated);
				sendCompleteSnapshot(player, updated);
				notifyTerminalBarrierForPlayer(player.level().getServer(), owner);
			} else {
				sendActiveSnapshot(player, updated, recoveryPosition);
				if (penalty == PenaltyType.WATER) {
					movePlayerToBall(player, ball);
				}
			}
		}
		MinecraftGolf.LOGGER.info("Applied {} penalty to player {} on hole {}; recovered ball {} to {}",
			penalty, owner, updated.hole().id(), ball.getUUID(), recoveryPosition);
	}

	/**
	 * Chooses a water/lava recovery target: the nearest safe land near the entry
	 * point (back on the line, then a bounded radial fallback), or the
	 * previous-shot position when no safe drop exists.
	 */
	private static Vec3 waterDropTarget(ServerLevel level, PlayerHoleSession session,
			Vec3 from, Vec3 entry) {
		HoleBoundary boundary = session.state().hole().boundary();
		Vec3 heading = entry.subtract(from);
		Optional<Vec3> clear = HazardDropSearch.findDrop(entry, heading, boundary,
			(x, z) -> resolveDropRest(level, entry, x, z, false));
		if (clear.isPresent()) {
			return clear.orElseThrow();
		}
		// Last resort before the previous-shot position: accept a hazard surface
		// (a bunker or honey) rather than give the whole shot distance back.
		return HazardDropSearch.findDrop(entry, heading, boundary,
			(x, z) -> resolveDropRest(level, entry, x, z, true))
			.orElse(session.lastSafePosition());
	}

	/**
	 * Resolves a safe ball rest site for a horizontal column: scans down from just
	 * above the entry point for the first solid support with a fluid-free block
	 * above it, and returns the ball center resting on that support.
	 *
	 * @param allowHazardSurfaces when false, a support block that resolves to a golf
	 *                           hazard surface (bunker sand, honey) is skipped so the
	 *                           drop keeps looking for fairway-like land
	 */
	private static Optional<Vec3> resolveDropRest(ServerLevel level, Vec3 entryPoint,
			double x, double z, boolean allowHazardSurfaces) {
		int columnX = (int) Math.floor(x);
		int columnZ = (int) Math.floor(z);
		double centerX = columnX + 0.5;
		double centerZ = columnZ + 0.5;
		int top = (int) Math.floor(entryPoint.y()) + DROP_SCAN_BLOCKS_UP;
		int bottom = (int) Math.floor(entryPoint.y()) - DROP_SCAN_BLOCKS_DOWN;
		for (int blockY = top; blockY >= bottom; blockY--) {
			BlockPos support = new BlockPos(columnX, blockY, columnZ);
			BlockPos body = support.above();
			// Resolve the actual top of the support's collision shape; assuming a full
			// block top would float the ball above carpets, slabs, snow, and similar.
			double supportTop = collisionTop(level, support, centerX, centerZ);
			if (Double.isNaN(supportTop) || !level.getFluidState(support).isEmpty()) {
				continue;
			}
			if (!level.getFluidState(body).isEmpty()
					|| !level.getBlockState(body).getCollisionShape(level, body).isEmpty()) {
				continue;
			}
			if (!allowHazardSurfaces
					&& DROP_SURFACE_RESOLVER.resolve(level.getBlockState(support)).hazard()) {
				continue;
			}
			return Optional.of(Vec3.of(centerX,
				blockY + supportTop + GolfBallEntity.BALL_RADIUS, centerZ));
		}
		return Optional.empty();
	}

	/**
	 * Highest collision-shape top under the given horizontal point within one
	 * block, or {@link Double#NaN} when the block has no collision there.
	 */
	private static double collisionTop(ServerLevel level, BlockPos support,
			double centerX, double centerZ) {
		VoxelShape shape = level.getBlockState(support).getCollisionShape(level, support);
		return CollisionShapeTop.at(shape, centerX - support.getX(), centerZ - support.getZ());
	}

	private static boolean isHazardFluid(ServerLevel level, BlockPos position) {
		return level.getFluidState(position).is(FluidTags.WATER)
			|| level.getFluidState(position).is(FluidTags.LAVA);
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
		notifyTerminalBarrierForPlayer(player.level().getServer(), player.getUUID());
		return new StartResult(true, "[golf] Pick Up Ball — score recorded as " + updated.strokes());
	}

	/** Performs one server-authoritative stroke for a stationary ball beside the cup. */
	public StartResult tapIn(ServerPlayer player) {
		PlayerHoleSession session = lifecycle.session(player.getUUID()).orElse(null);
		if (session == null || session.state().isComplete()) {
			return new StartResult(false, "[golf] no active hole is eligible for a tap-in");
		}
		GolfBallEntity ball = assignedBall(player, session.ballUuid())
			.filter(GolfBallEntity.class::isInstance).map(GolfBallEntity.class::cast).orElse(null);
		if (ball == null || ball.owner() == null || !player.getUUID().equals(ball.owner())) {
			return new StartResult(false, "[golf] your active golf ball is missing");
		}
		if (!ball.isResting()) {
			return new StartResult(false, "[golf] wait for the ball to stop before tapping in");
		}
		Vec3 position = ball.ballState().position();
		if (!TapInRules.withinTapInRadius(position, session.state().hole().cup())) {
			return new StartResult(false, "[golf] tap-in is available only within "
				+ (int) TapInRules.TAP_IN_RADIUS_BLOCKS + " blocks of the cup");
		}

		PlayerHoleSession updatedSession = session.recordAcceptedShot(position);
		PlayerHoleState updated = updatedSession.state();
		if (!updated.isComplete()) {
			updatedSession = updatedSession.holeOut();
			updated = updatedSession.state();
		}
		lifecycle.update(player.getUUID(), updatedSession);
		updateCourseState(player.getUUID(), updated);
		ball.placeAtRest(updated.hole().cup());
		sendCompletion(player, updated);
		sendCompleteSnapshot(player, updated);
		notifyTerminalBarrierForPlayer(player.level().getServer(), player.getUUID());
		return new StartResult(true, "[golf] Tap in accepted (+1 stroke) — hole complete in "
			+ updated.strokes() + " strokes");
	}

	public StartResult nextHole(ServerPlayer player) {
		PlayerCourseState state = courseState(player.getUUID());
		if (state == null) {
			return reject(player, "/golf nexthole", "[golf] start the course before advancing");
		}
		ReadyGolfRound playerRound = rounds.findByPlayer(player.getUUID()).orElse(null);
		if (playerRound == null) {
			if (state.isComplete()) {
				return reject(player, "/golf nexthole", "[golf] course is already complete; use /golf round restart to replay");
			}
			if (!state.currentHole().isComplete()) {
				return reject(player, "/golf nexthole", "[golf] complete the current hole before advancing");
			}
			if (state.currentHoleIndex() >= state.course().holes().size() - 1) {
				PlayerCourseState completed = state.advance();
				soloCourseStates.put(player.getUUID(), completed);
				// Deliver the final card at the state transition itself, rather than
				// relying on a later status refresh or client reconnect.
				sendSoloScorecard(player, completed);
				sendCurrentSnapshot(player);
				return new StartResult(true, "[golf] course complete");
			}
			HoleDefinition nextDefinition = state.course().holes().get(state.currentHoleIndex() + 1);
			PlayerHoleSession oldSession = lifecycle.session(player.getUUID()).orElse(null);
			if (oldSession == null) {
				return reject(player, "/golf nexthole", "[golf] current hole session is missing; use /golf hole restart");
			}
			StartResult preflight = createAttempt(player, true, nextDefinition);
			if (!preflight.success()) {
				return preflight;
			}
			soloCourseStates.put(player.getUUID(), state.advance());
			return new StartResult(true, "[golf] advanced to Hole " + nextDefinition.number());
		}
		CourseDefinition roundCourse = playerRound.course();
		ReadyGolfParticipant caller = playerRound.findParticipant(player.getUUID()).orElse(null);
		if (caller == null || caller.status() != ParticipantStatus.ACTIVE) {
			return new StartResult(false, "[golf] only an active participant can advance the round");
		}
		if (state.isComplete()) {
			return new StartResult(false, "[golf] course is already complete; use /golf round restart to replay");
		}
		if (!state.currentHole().isComplete()) {
			return new StartResult(false, "[golf] complete the current hole before advancing");
		}
		if (!playerRound.allActiveTerminal()) {
			MinecraftGolf.LOGGER.info(
				"Rejected early Hole {} advancement in Ready Golf round {}: {}/{} active golfers complete",
				playerRound.currentHoleIndex() + 1, playerRound.roundId(),
				playerRound.terminalActiveParticipantCount(), playerRound.activeParticipantCount());
			return new StartResult(false, waitingMessage(playerRound));
		}

		int expectedHoleIndex = playerRound.currentHoleIndex();
		HoleDefinition nextDefinition = roundCourse.holes().get(expectedHoleIndex + 1);
		List<PreparedAttempt> prepared = prepareTransition(player, playerRound, nextDefinition);
		if (prepared == null) {
			return new StartResult(false,
				"[golf] transition cancelled; every golfer remains on the current hole");
		}

		sendMultiplayerHoleResults(player.level().getServer(), playerRound);
		ReadyGolfRound previousRound = playerRound;
		ReadyGolfRound advanced = playerRound.advanceNextHole(expectedHoleIndex);
		rounds.update(playerRound.roundId(), playerRound, advanced);
		cleanupSuspendedAtTransition(player, previousRound);
		commitTransition(prepared, nextDefinition);
		MinecraftGolf.LOGGER.info("Advanced Ready Golf round {} from Hole {} to Hole {} for {} golfers",
			advanced.roundId(), expectedHoleIndex + 1, expectedHoleIndex + 2, prepared.size());
		return new StartResult(true, "[golf] all golfers advanced to Hole " + (expectedHoleIndex + 2));
	}

	private List<PreparedAttempt> prepareTransition(ServerPlayer serverContext, ReadyGolfRound round,
			HoleDefinition nextDefinition) {
		List<PreparedAttempt> prepared = new ArrayList<>();
		for (ReadyGolfParticipant participant : round.participants()) {
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

	private void commitRoundStart(List<PreparedAttempt> prepared, HoleDefinition definition) {
		for (PreparedAttempt attempt : prepared) {
			lifecycle.start(attempt.player().getUUID(), definition, attempt.ball().getUUID());
			discardPlayerOwnedBalls(attempt.player(), attempt.ball().getUUID());
			teleportToTransition(attempt.player(), definition);
			grantMissingClubs(attempt.player());
			sendActiveSnapshot(attempt.player(), lifecycle.session(attempt.player().getUUID())
				.orElseThrow().state(), definition.tee());
		}
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
			placeFlag(attempt.player().level(), cupBlockPos);
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

	private static void placeFlag(ServerLevel level, BlockPos cupBlockPos) {
		BlockPos middlePos = cupBlockPos.above();
		BlockPos topPos = middlePos.above();
		if (level.getBlockState(middlePos).canBeReplaced()
			|| level.getBlockState(middlePos).getBlock() == GolfBlocks.GOLF_FLAG) {
			level.setBlockAndUpdate(middlePos, GolfBlocks.GOLF_FLAG.defaultBlockState());
		}
		if (level.getBlockState(topPos).canBeReplaced()
			|| level.getBlockState(topPos).getBlock() == GolfBlocks.GOLF_FLAG_TOP) {
			level.setBlockAndUpdate(topPos, GolfBlocks.GOLF_FLAG_TOP.defaultBlockState());
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

	private static void teleportToWorldSpawn(ServerPlayer player) {
		ServerLevel overworld = player.level().getServer().overworld();
		BlockPos spawn = overworld.getRespawnData().pos();
		player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
			Set.of(), player.getYRot(), player.getXRot(), true);
	}

	private StartResult travelToNextShot(ServerPlayer player, GolfBallEntity ball) {
		Optional<Destination> destination = safeStandingNear(player, ball);
		if (destination.isEmpty()) {
			return new StartResult(false, "[golf] no safe standing position found near the ball");
		}
		Destination safe = destination.orElseThrow();
		player.teleportTo(safe.x() + 0.5, safe.y(), safe.z() + 0.5);
		return new StartResult(true, "[golf] Ball stopped — moved safely to your next shot");
	}

	/**
	 * Moves the player beside a just-relocated ball after a hazard drop, matching
	 * the automatic move performed on a natural physics rest. Best-effort: with no
	 * safe standing position the player simply walks.
	 */
	private static void movePlayerToBall(ServerPlayer player, GolfBallEntity ball) {
		safeStandingNear(player, ball).ifPresent(safe ->
			player.teleportTo(safe.x() + 0.5, safe.y(), safe.z() + 0.5));
	}

	private static Optional<Destination> safeStandingNear(ServerPlayer player, GolfBallEntity ball) {
		BlockPos ballBlock = BlockPos.containing(ball.getX(), ball.getY(), ball.getZ());
		return TravelDestinationSearch.find(
			ballBlock.getX(), ballBlock.getY(), ballBlock.getZ(),
			candidate -> isSafeTravelDestination(player.level(), candidate));
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
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (round != null && round.phase() == RoundPhase.PLAYING) {
			ReadyGolfParticipant participant = round.findParticipant(playerId).orElse(null);
			if (participant != null && participant.status() == ParticipantStatus.SUSPENDED) {
				ReadyGolfRound reconnected = round.reconnect(playerId);
				rounds.update(round.roundId(), round, reconnected);
				MinecraftGolf.LOGGER.info("{} reconnected to Ready Golf round {}",
					player.getName().getString(), reconnected.roundId());
			}
		}
		sendCurrentSnapshot(player);
		sendLobbyState(player);
		if (round == null || round.phase() == RoundPhase.COMPLETE) sendPracticeEntryPoint(player);
	}

	/** Offers the player-facing course browser without requiring a custom keybind. */
	public void sendPracticeEntryPoint(ServerPlayer player) {
		if (VisitorService.isVisitor(player)) {
			// Visitors watch; the visitor welcome/reminder carries the call to action instead.
			return;
		}
		Component action = Component.literal("[Play a Round]")
			.withStyle(style -> style.withColor(ChatFormatting.GOLD).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/golf browse"))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Open Course List"))));
		player.sendSystemMessage(Component.literal("[golf] Practice Mode — ").append(action));
	}

	/**
	 * S5: removes a lobby golfer or suspends an in-progress golfer on disconnect.
	 * Suspension removes the golfer from the advancement barrier so an offline
	 * player can never deadlock the remaining golfers.
	 */
	public void onPlayerDisconnected(ServerPlayer player, MinecraftServer server) {
		UUID playerId = player.getUUID();
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (round == null) return;
		ReadyGolfParticipant participant = round.findParticipant(playerId).orElse(null);
		if (participant == null || participant.status() != ParticipantStatus.ACTIVE) {
			return;
		}
		if (round.phase() == RoundPhase.COMPLETE) {
			lifecycle.session(playerId).ifPresent(session -> discardAssignedBall(player, session));
			lifecycle.abandon(playerId);
			rounds.disconnect(round.roundId(), playerId);
			broadcastLobbyState(server);
			MinecraftGolf.LOGGER.info("{} left completed Ready Golf round on disconnect",
				player.getName().getString());
			return;
		}
		if (round.phase() == RoundPhase.LOBBY) {
			UUID roundId = round.roundId();
			rounds.disconnect(roundId, playerId);
			broadcastLobbyState(server);
			MinecraftGolf.LOGGER.info("{} left Ready Golf lobby {}",
				player.getName().getString(), roundId);
			return;
		}
		ReadyGolfRound disconnected = rounds.disconnect(round.roundId(), playerId);
		ReadyGolfParticipant updatedParticipant = disconnected.findParticipant(playerId).orElse(null);
		if (updatedParticipant != null && updatedParticipant.status() == ParticipantStatus.WITHDRAWN) {
			lifecycle.session(playerId).ifPresent(session -> discardAssignedBall(player, session));
			lifecycle.abandon(playerId);
		}
		broadcastLobbyState(server);
		MinecraftGolf.LOGGER.info("{} {} Ready Golf round {}",
			player.getName().getString(), updatedParticipant != null
				&& updatedParticipant.status() == ParticipantStatus.WITHDRAWN ? "withdrew from" : "suspended from",
			round.roundId());
		notifyTerminalBarrier(server, round.roundId(), null);
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
			sendSnapshot(player, hole == null
				? HoleStatePayload.noCourse() : HoleStatePayload.practice(configuredHole()));
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
			if (entity instanceof GolfBallEntity ball) {
				sendActiveSnapshot(player, state, ballPosition, ball.shotDistanceBlocks(), true);
			} else {
				sendActiveSnapshot(player, state, ballPosition);
			}
		}
	}

	private HoleStatePayload practiceSnapshot() {
		return hole == null ? HoleStatePayload.noCourse() : HoleStatePayload.practice(hole);
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

	/**
	 * Whether this player's client can resolve the custom club items. A modded client
	 * advertises our payloads; a vanilla/Bedrock client does not, and receives the
	 * M10 vanilla fallback representation instead.
	 */
	private static boolean canResolveCustomItems(ServerPlayer player) {
		return ServerPlayNetworking.canSend(player, HoleStatePayload.TYPE);
	}

	/** Whether the player already carries the logical club {@code clubId} in any representation. */
	private static boolean hasClub(ServerPlayer player, String clubId) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ClubDefinition held = GolfItems.clubOf(player.getInventory().getItem(slot));
			if (held != null && held.id().equals(clubId)) {
				return true;
			}
		}
		return false;
	}

	private static int grantMissingClubs(ServerPlayer player) {
		boolean modded = canResolveCustomItems(player);
		int granted = 0;
		for (ClubDefinition club : GolfClubs.ALL) {
			if (hasClub(player, club.id())) {
				continue;
			}
			ItemStack stack = GolfItems.stackFor(club, modded);
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
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (round == null || round.phase() != RoundPhase.PLAYING) {
			PlayerCourseState solo = soloCourseStates.get(playerId);
			if (solo != null) {
				soloCourseStates.put(playerId, solo.updateCurrentHole(updated));
			}
			return;
		}
		ReadyGolfParticipant participant = round.findParticipant(playerId).orElse(null);
		if (participant == null || participant.status() != ParticipantStatus.ACTIVE) {
			return;
		}
		ReadyGolfRound updatedRound = round.updateCurrentHole(playerId, updated);
		rounds.update(round.roundId(), round, updatedRound);
	}

	/** True while no round owns the player, or while they are an ACTIVE participant. */
	private boolean isActiveParticipant(UUID playerId) {
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (round == null) {
			return true;
		}
		if (round.phase() != RoundPhase.PLAYING) {
			return false;
		}
		return round.findParticipant(playerId)
			.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.orElse(false);
	}

	private PlayerCourseState courseState(UUID playerId) {
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (round != null) {
			PlayerCourseState roundState = round.findParticipant(playerId)
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.map(ReadyGolfParticipant::courseState)
			.orElse(null);
			if (roundState != null) {
				return roundState;
			}
		}
		return soloCourseStates.get(playerId);
	}

	private boolean cleanupPlayerState(ServerPlayer player) {
		UUID playerId = player.getUUID();
		Optional<PlayerHoleSession> session = lifecycle.session(playerId);
		boolean hadSession = session.isPresent();
		session.ifPresent(current -> discardAssignedBall(player, current));
		lifecycle.abandon(playerId);
		boolean hadSoloState = soloCourseStates.remove(playerId) != null;
		return hadSession || hadSoloState;
	}

	private StartResult reject(ServerPlayer player, String command, String message) {
		ReadyGolfRound round = rounds.findByPlayer(player.getUUID()).orElse(null);
		ReadyGolfParticipant participant = round == null ? null
			: round.findParticipant(player.getUUID()).orElse(null);
		MinecraftGolf.LOGGER.warn(
			"Rejected {} for player {} ({}) reason={} roundPhase={} participantStatus={} holeSession={}",
			command, player.getName().getString(), player.getUUID(), message,
			round == null ? "NONE" : round.phase(),
			participant == null ? "NONE" : participant.status(),
			lifecycle.session(player.getUUID()).map(session -> session.state().isComplete() ? "COMPLETE" : "ACTIVE")
				.orElse("NONE"));
		return new StartResult(false, message);
	}

	private String cumulativeStatus(UUID playerId, PlayerHoleState current) {
		return cumulativeStatus(courseState(playerId), current);
	}

	static String cumulativeStatus(PlayerCourseState state, PlayerHoleState current) {
		return HoleScoringDisplay.from(current, state).courseText();
	}

	private HoleStatePayload withCourseTotals(UUID playerId, HoleStatePayload payload, PlayerHoleState current) {
		PlayerCourseState state = courseState(playerId);
		if (state == null) {
			return payload;
		}
		HoleScoringDisplay display = HoleScoringDisplay.from(current, state);
		return payload.withCourseTotals(display.courseStrokes(), display.coursePar(),
			display.hasCourseScore() ? display.courseTotalPar() : 0);
	}

	/**
	 * M10.3 S3: sends the authoritative snapshot to a modded client, and mirrors it into
	 * the vanilla action bar for a client-light one. The payload channel itself is
	 * unchanged; the fallback is additive and gated inside the presentation service.
	 */
	private void sendSnapshot(ServerPlayer player, HoleStatePayload payload) {
		HoleStateNetworking.send(player, payload);
		ClientLightPresentation.onSnapshot(player, payload);
	}

	private void sendActiveSnapshot(ServerPlayer player, PlayerHoleState state, Vec3 ballPosition) {
		sendActiveSnapshot(player, state, ballPosition, 0, true);
	}

	private void sendActiveSnapshot(ServerPlayer player, PlayerHoleState state, Vec3 ballPosition,
			int shotDistanceBlocks, boolean notifyTapIn) {
		boolean eligible = assignedBall(player, lifecycle.session(player.getUUID())
			.map(PlayerHoleSession::ballUuid).orElse(null))
			.filter(GolfBallEntity.class::isInstance)
			.map(GolfBallEntity.class::cast)
			.filter(GolfBallEntity::isResting)
			.map(ball -> TapInRules.withinTapInRadius(ballPosition, state.hole().cup()))
			.orElse(false);
		HoleStatePayload payload = withCourseTotals(player.getUUID(),
			HoleStatePayload.active(state, ballPosition, shotDistanceBlocks)
				.withTapInAvailable(eligible), state);
		sendSnapshot(player, payload);
		if (notifyTapIn && payload.tapInAvailable()) {
			Component action = Component.literal("[Tap in (+1 stroke)]")
				.withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
					.withClickEvent(new ClickEvent.RunCommand("/golf tapin")));
			player.sendSystemMessage(Component.literal("[golf] near the cup — ").append(action));
		}
	}

	private void sendShotProgress(GolfBallEntity ball, PlayerHoleSession session, Vec3 position) {
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(ball.owner());
		if (player != null) {
			sendActiveSnapshot(player, session.state(), position, ball.shotDistanceBlocks(), false);
		}
	}

	private void sendPracticeShotProgress(GolfBallEntity ball) {
		ServerPlayer player = ball.level().getServer().getPlayerList().getPlayer(ball.owner());
		if (player != null) {
			sendSnapshot(player,
				HoleStatePayload.noCourse().withShotDistanceBlocks(ball.shotDistanceBlocks()));
		}
	}

	private void sendMissingSnapshot(ServerPlayer player, PlayerHoleState state) {
		sendSnapshot(player, withCourseTotals(player.getUUID(),
			HoleStatePayload.missingBall(state), state));
	}

	private void sendCompleteSnapshot(ServerPlayer player, PlayerHoleState state) {
		HoleStatePayload payload = withCourseTotals(player.getUUID(),
			HoleStatePayload.complete(state), state);
		if (isRoundComplete(player.getUUID())) {
			payload = payload.asRoundComplete();
		} else if (roundAdvanceAvailable(player.getUUID(), state)
				|| soloAdvanceAvailable(player.getUUID(), state)) {
			payload = payload.withRoundAdvanceAvailable(true);
		}
		sendSnapshot(player, payload);
		if (payload.phase() != HoleStatePayload.Phase.ROUND_COMPLETE
				&& courseState(player.getUUID()) != null && courseState(player.getUUID()).isComplete()) {
			sendSoloScorecard(player, courseState(player.getUUID()));
		}
	}

	private boolean isRoundComplete(UUID playerId) {
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		return round != null && round.phase() == RoundPhase.COMPLETE
			&& round.findParticipant(playerId)
				.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
				.orElse(false);
	}

	private boolean roundAdvanceAvailable(UUID playerId, PlayerHoleState state) {
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		if (!state.isComplete() || round == null
				|| round.phase() != RoundPhase.PLAYING
				|| round.currentHoleIndex() >= round.course().holes().size() - 1
				|| !round.allActiveTerminal()) {
			return false;
		}
		return round.findParticipant(playerId)
			.map(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.orElse(false);
	}

	private boolean soloAdvanceAvailable(UUID playerId, PlayerHoleState state) {
		PlayerCourseState solo = soloCourseStates.get(playerId);
		return rounds.findByPlayer(playerId).isEmpty() && solo != null && state.isComplete()
			&& !solo.isComplete()
			&& solo.currentHoleIndex() < solo.course().holes().size() - 1;
	}

	private void sendFinalScorecard(ServerPlayer player, CourseScorecard scorecard) {
		// M10.3 S3: client-light clients get the RoundScorecardNetworking chat fallback
		// (the full multi-player card) instead, so skip this per-player duplicate.
		if (ClientLightPresentation.isClientLight(player)) {
			return;
		}
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

	private void sendSoloScorecard(ServerPlayer player, PlayerCourseState courseState) {
		CourseScorecard scorecard = courseState.finalScorecard();
		List<RoundScorecardPayload.HoleColumn> holes = RoundScorecardPayload.holes(courseState.course());
		RoundScorecardPayload payload = new RoundScorecardPayload(
			scorecard.courseId(), holes,
			List.of(RoundScorecardPayload.player(player.getName().getString(), scorecard, holes)));
		RoundScorecardNetworking.send(player, payload);
		MinecraftGolf.LOGGER.info("Sent solo final scorecard to {} for course {}",
			player.getName().getString(), scorecard.courseId());
	}

	private void sendRoundScorecard(MinecraftServer server, ReadyGolfRound round) {
		List<RoundScorecardPayload.HoleColumn> holes = RoundScorecardPayload.holes(round.course());
		List<RoundScorecardPayload.PlayerRow> rows = round.participants().stream()
			.filter(participant -> participant.status() == ParticipantStatus.ACTIVE)
			.map(participant -> RoundScorecardPayload.player(playerLabel(server, participant.playerId()),
				participant.courseState().finalScorecard(), holes))
			.toList();
		RoundScorecardPayload payload = new RoundScorecardPayload(round.course().id(),
			holes, rows);
		for (ReadyGolfParticipant participant : round.participants()) {
			if (participant.status() == ParticipantStatus.ACTIVE) {
				ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId());
				if (player != null) RoundScorecardNetworking.send(player, payload);
			}
		}
	}

	private void notifyTerminalBarrierForPlayer(MinecraftServer server, UUID playerId) {
		ReadyGolfRound round = rounds.findByPlayer(playerId).orElse(null);
		notifyTerminalBarrier(server, round == null ? null : round.roundId(),
			round == null ? playerId : null);
	}

	private void notifyTerminalBarrier(MinecraftServer server, UUID roundId, UUID soloPlayerId) {
		broadcastLobbyState(server);
		if (roundId != null) {
			ReadyGolfRound round = rounds.find(roundId).orElse(null);
			if (round == null) return;
			if (round.phase() == RoundPhase.COMPLETE) {
				for (ReadyGolfParticipant participant : round.participants()) {
					if (participant.status() != ParticipantStatus.ACTIVE) continue;
					ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
					if (recipient != null) sendCompleteSnapshot(recipient,
						participant.courseState().completedHoles().getLast());
				}
				sendMultiplayerFinalResults(server, round);
				return;
			}
			if (round.phase() != RoundPhase.PLAYING) return;
			for (ReadyGolfParticipant participant : round.participants()) {
				if (participant.status() != ParticipantStatus.ACTIVE) {
					continue;
				}
				ServerPlayer recipient = server.getPlayerList().getPlayer(participant.playerId());
				if (recipient != null) {
					if (round.allActiveTerminal()
							&& participant.courseState().currentHole().isComplete()) {
						sendCompleteSnapshot(recipient, participant.courseState().currentHole());
					}
					if (participant.courseState().currentHole().isComplete()) sendBarrierMessage(recipient, round);
				}
			}
			return;
		}
		if (soloPlayerId != null) {
			PlayerCourseState solo = soloCourseStates.get(soloPlayerId);
			if (solo == null || solo.isComplete() || !solo.currentHole().isComplete()) return;
			ServerPlayer player = server.getPlayerList().getPlayer(soloPlayerId);
			if (player != null) sendSoloBarrierMessage(player);
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
		sendRoundScorecard(server, round);
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

	private String waitingMessage(ReadyGolfRound round) {
		return "[golf] waiting for golfers: " + round.terminalActiveParticipantCount()
			+ "/" + round.activeParticipantCount() + " complete";
	}

	private void sendBarrierMessage(ServerPlayer player, ReadyGolfRound round) {
		if (!round.allActiveTerminal()) {
			player.sendSystemMessage(Component.literal(waitingMessage(round)));
			return;
		}
		Component action = Component.literal("[Go to next hole]")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/golf nexthole"))
				.withHoverEvent(new HoverEvent.ShowText(
					Component.literal("Advance every active golfer"))));
		player.sendSystemMessage(Component.literal("[golf] all golfers complete — ").append(action));
	}

	private void sendSoloBarrierMessage(ServerPlayer player) {
		Component action = Component.literal("[Go to next hole]")
			.withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/golf nexthole"))
				.withHoverEvent(new HoverEvent.ShowText(
					Component.literal("Advance to the next hole"))));
		player.sendSystemMessage(Component.literal("[golf] hole complete — ").append(action));
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

	private record PreparedRestart(
		ServerPlayer player,
		PlayerHoleSession oldSession,
		GolfBallEntity ball
	) {
	}

	private record PreparedReplacement(
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
