package com.prillcode.minecraftgolf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.UUID;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.server.ActiveHoleService.StartResult;
import com.prillcode.minecraftgolf.server.PracticeRangeService;
import com.prillcode.minecraftgolf.server.PracticeRangeService.Location;
import com.prillcode.minecraftgolf.server.VisitorService;
import com.prillcode.minecraftgolf.block.GolfBlocks;
import net.minecraft.world.level.block.Blocks;
import com.prillcode.minecraftgolf.club.ShotType;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.net.ShotOutcome;
import com.prillcode.minecraftgolf.net.ShotService;
import com.prillcode.minecraftgolf.round.RoundLobbyProjection;

/** Player-facing lifecycle commands for the server-authoritative M5 course. */
public final class GolfHoleCommands {

	private static boolean registered;

	private GolfHoleCommands() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		CommandRegistrationCallback.EVENT.register(GolfHoleCommands::onRegisterCommands);
	}

	private static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess, CommandSelection environment) {
			dispatcher.register(Commands.literal("golf")
			.then(Commands.literal("help").executes(GolfHoleCommands::help))
			.then(Commands.literal("spectator")
				.executes(GolfHoleCommands::spectatorView)
				.then(Commands.literal("leave").executes(GolfHoleCommands::leaveSpectator)))
			.then(Commands.literal("visitor")
				.then(Commands.literal("status").executes(GolfHoleCommands::visitorStatus))
				.then(Commands.literal("address").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("address", StringArgumentType.greedyString())
						.executes(GolfHoleCommands::visitorAddress)))
				.then(Commands.literal("link").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("link", StringArgumentType.greedyString())
						.executes(GolfHoleCommands::visitorLink)))
				.then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("set").executes(GolfHoleCommands::visitorSpawnSet))
					.then(Commands.literal("clear").executes(GolfHoleCommands::visitorSpawnClear))))
			.then(Commands.literal("round")
				.then(Commands.literal("create")
					.executes(GolfHoleCommands::createRound)
					.then(Commands.argument("courseId", StringArgumentType.word())
						.executes(GolfHoleCommands::createRoundForCourse)))
				.then(Commands.literal("list").executes(GolfHoleCommands::listRounds))
				.then(Commands.literal("join")
					.executes(GolfHoleCommands::joinRound)
					.then(Commands.argument("roundId", StringArgumentType.word())
						.executes(GolfHoleCommands::joinSpecificRound)))
				.then(Commands.literal("start").executes(GolfHoleCommands::startRound))
				.then(Commands.literal("leave").executes(GolfHoleCommands::leaveRound))
				.then(Commands.literal("restart").executes(GolfHoleCommands::restartRound))
				.then(Commands.literal("status").executes(GolfHoleCommands::roundStatus)))
			.then(Commands.literal("browse").executes(GolfHoleCommands::browseCourses))
			.then(Commands.literal("hole")
				.then(Commands.literal("start")
					.executes(GolfHoleCommands::start)
					.then(Commands.argument("hole", IntegerArgumentType.integer(1))
						.executes(GolfHoleCommands::startAtHole)))
				.then(Commands.literal("restart").executes(GolfHoleCommands::restart))
				.then(Commands.literal("status").executes(GolfHoleCommands::status)))
			.then(Commands.literal("clubs")
				.then(Commands.literal("equip").executes(GolfHoleCommands::equipClubs)))
			.then(Commands.literal("practice")
				.then(Commands.literal("ball").executes(GolfHoleCommands::dropPracticeBall))
				.then(Commands.literal("clear").executes(GolfHoleCommands::clearPracticeBalls))
				.then(Commands.literal("tee")
					.executes(GolfHoleCommands::goToPracticeTee)
					.then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(GolfHoleCommands::setPracticeTee)))
				.then(Commands.literal("target")
					.then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", IntegerArgumentType.integer(1, PracticeRangeService.MAX_TARGETS))
							.executes(GolfHoleCommands::setPracticeTarget)))
					.then(Commands.literal("clear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("target", IntegerArgumentType.integer(1, PracticeRangeService.MAX_TARGETS))
							.executes(GolfHoleCommands::clearPracticeTarget)))
					.then(Commands.literal("list").executes(GolfHoleCommands::listPracticeTargets)))
				)
			.then(Commands.literal("pickup").executes(GolfHoleCommands::pickUp))
			.then(Commands.literal("tapin").executes(GolfHoleCommands::tapIn))
			.then(Commands.literal("swing")
				.executes(GolfHoleCommands::swing)
				.then(Commands.argument("power", FloatArgumentType.floatArg(0.0f, 1.0f))
					.executes(GolfHoleCommands::swing)
					.then(Commands.argument("accuracy", FloatArgumentType.floatArg(0.0f, 1.0f))
						.executes(GolfHoleCommands::swing)
						.then(Commands.argument("type", StringArgumentType.word())
							.executes(GolfHoleCommands::swing)))))
			.then(Commands.literal("nexthole").executes(GolfHoleCommands::nextHole)));
		MinecraftGolf.LOGGER.info(
			"Registered Ready Golf round, hole lifecycle, practice ball, practice clear, Pick Up, swing, and next-hole commands");
	}

	private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		StartResult result = ActiveHoleService.instance().start(player);
		if (!result.success()) {
			context.getSource().sendFailure(Component.literal(result.message()));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(result.message()), false);
		return 1;
	}

	private static int help(CommandContext<CommandSourceStack> context) {
		context.getSource().sendSuccess(() -> Component.literal("[golf] Minecraft Golf commands:")
				.withStyle(ChatFormatting.GOLD), false);
		context.getSource().sendSuccess(() -> Component.literal("Getting started: /golf round list | /golf round join"), false);
		context.getSource().sendSuccess(() -> Component.literal("Rounds: /golf round create <courseId> | /golf round start | /golf round status | /golf round leave"), false);
		context.getSource().sendSuccess(() -> Component.literal("Playing: /golf hole status | /golf hole restart | /golf pickup | /golf nexthole"), false);
		context.getSource().sendSuccess(() -> Component.literal("Swing (no client mod needed): /golf swing [power] [accuracy] [shotType]"), false);
		context.getSource().sendSuccess(() -> Component.literal("Practice: /golf practice ball | /golf practice tee | /golf practice target list"), false);
		context.getSource().sendSuccess(() -> Component.literal("Solo: /golf course play [courseId] [hole] | /golf hole start [hole]"), false);
		context.getSource().sendSuccess(() -> Component.literal("Visitors (Bedrock/vanilla): /golf spectator | /golf spectator leave"), false);
		return 1;
	}

	private static int startAtHole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().start(
			player, IntegerArgumentType.getInteger(context, "hole")));
	}

	private static int createRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().createRound(player));
	}

	private static int createRoundForCourse(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().createRound(
			StringArgumentType.getString(context, "courseId"), player));
	}

	private static int joinRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().joinRound(player));
	}

	private static int joinSpecificRound(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		try {
			return sendResult(context, ActiveHoleService.instance().joinRound(
				UUID.fromString(StringArgumentType.getString(context, "roundId")), player));
		} catch (IllegalArgumentException exception) {
			context.getSource().sendFailure(Component.literal("[golf] round id must be a full server-issued UUID"));
			return 0;
		}
	}

	private static int listRounds(CommandContext<CommandSourceStack> context) {
		RoundLobbyProjection projection = ActiveHoleService.instance()
			.roundProjection(context.getSource().getServer());
		context.getSource().sendSuccess(() -> Component.literal("[golf] Open Ready Golf lobbies:")
			.withStyle(ChatFormatting.GOLD), false);
		if (projection.lobbies().isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal(
				"None. Create one with /golf round create <courseId>"), false);
		}
		for (RoundLobbyProjection.LobbyEntry lobby : projection.lobbies()) {
			String command = "/golf round join " + lobby.roundId();
			context.getSource().sendSuccess(() -> Component.literal(lobby.courseName() + " | "
				+ lobby.coordinatorName() + " | " + lobby.participantCount() + "/" + lobby.capacity() + " | ")
				.append(Component.literal("[Join]").withStyle(style -> style.withColor(ChatFormatting.GREEN)
					.withClickEvent(new ClickEvent.RunCommand(command))))
				.append(Component.literal(" " + command)), false);
		}
		context.getSource().sendSuccess(() -> Component.literal("[golf] Finalized courses:"), false);
		for (RoundLobbyProjection.CourseEntry course : projection.courses()) {
			context.getSource().sendSuccess(() -> Component.literal(course.id() + " | " + course.displayName()
				+ " | " + course.holeCount() + " holes, par " + course.totalPar()
				+ " | create: /golf round create " + course.id()), false);
		}
		return 1;
	}

	private static int startRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().startRound(player));
	}

	private static int leaveRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().leaveRound(
			context.getSource().getPlayerOrException()));
	}

	private static int restartRound(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().restartRound(player));
	}

	private static int roundStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		context.getSource().sendSuccess(
			() -> Component.literal(ActiveHoleService.instance().roundStatus(player)), false);
		return 1;
	}

	private static int browseCourses(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().openCourseBrowser(player));
	}

	private static int restart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().restart(player));
	}

	private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		context.getSource().sendSuccess(
			() -> Component.literal(ActiveHoleService.instance().status(player)), false);
		return 1;
	}

	private static int equipClubs(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().equipClubs(player));
	}

	private static int dropPracticeBall(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().dropPracticeBall(player));
	}

	private static int clearPracticeBalls(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().clearPracticeBalls(
			context.getSource().getPlayerOrException()));
	}

	private static int setPracticeTee(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Vec3 position = standingPoint(player);
		PracticeRangeService.instance().setTee(player.level().dimension().identifier().toString(), position);
		context.getSource().sendSuccess(() -> Component.literal("[golf] practice tee set to " + position), true);
		return 1;
	}

	private static int goToPracticeTee(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Location tee = PracticeRangeService.instance().tee();
		if (tee == null) return fail(context, "no practice tee is set; use /golf practice tee set");
		if (!player.level().dimension().identifier().toString().equals(tee.dimension())) {
			return fail(context, "practice tee is in dimension " + tee.dimension());
		}
		player.teleportTo(tee.position().x(), tee.position().y(), tee.position().z());
		context.getSource().sendSuccess(() -> Component.literal("[golf] moved to practice tee"), false);
		return 1;
	}

	private static int setPracticeTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int number = IntegerArgumentType.getInteger(context, "target");
		Vec3 position = standingPoint(player);
		PracticeRangeService service = PracticeRangeService.instance();
		Location previous = service.target(number);
		BlockPos cup = BlockPos.containing(position.x(), position.y() - GolfBallEntity.BALL_RADIUS, position.z());
		ServerLevel level = player.level();
		boolean placed = (level.getBlockState(cup).canBeReplaced()
			|| level.getBlockState(cup).getBlock() == GolfBlocks.GOLF_CUP)
			&& level.setBlockAndUpdate(cup, GolfBlocks.GOLF_CUP.defaultBlockState());
		if (placed) {
			GolfCourseCommands.placeFlag(level, cup);
			if (previous != null && previous.dimension().equals(level.dimension().identifier().toString())) {
				BlockPos previousCup = BlockPos.containing(previous.position().x(),
					previous.position().y() - GolfBallEntity.BALL_RADIUS, previous.position().z());
				if (!previousCup.equals(cup)) GolfCourseCommands.clearMarker(level, previousCup);
			}
			service.setTarget(number, level.dimension().identifier().toString(), position);
		}
		context.getSource().sendSuccess(() -> Component.literal("[golf] practice target " + number + " set at "
			+ position + (placed ? "; cup and flag placed" : "; cup block could not be placed here")), true);
		return placed ? 1 : 0;
	}

	private static int clearPracticeTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		int number = IntegerArgumentType.getInteger(context, "target");
		Location target = PracticeRangeService.instance().target(number);
		if (target == null) return fail(context, "practice target " + number + " is not set");
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (!player.level().dimension().identifier().toString().equals(target.dimension())) {
			return fail(context, "practice target is in dimension " + target.dimension());
		}
		BlockPos cup = BlockPos.containing(target.position().x(), target.position().y() - GolfBallEntity.BALL_RADIUS,
			target.position().z());
		for (BlockPos marker : new BlockPos[] {cup, cup.above(), cup.above(2)}) {
			if (player.level().getBlockState(marker).getBlock() == GolfBlocks.GOLF_CUP
				|| player.level().getBlockState(marker).getBlock() == GolfBlocks.GOLF_FLAG
				|| player.level().getBlockState(marker).getBlock() == GolfBlocks.GOLF_FLAG_TOP) {
				player.level().setBlockAndUpdate(marker, Blocks.AIR.defaultBlockState());
			}
		}
		PracticeRangeService.instance().clearTarget(number);
		context.getSource().sendSuccess(() -> Component.literal("[golf] practice target " + number + " cleared"), true);
		return 1;
	}

	private static int listPracticeTargets(CommandContext<CommandSourceStack> context) {
		PracticeRangeService service = PracticeRangeService.instance();
		context.getSource().sendSuccess(() -> Component.literal("[golf] practice tee: "
			+ (service.tee() == null ? "not set" : service.tee().position())), false);
		for (int number = 1; number <= PracticeRangeService.MAX_TARGETS; number++) {
			final int targetNumber = number;
			Location target = service.target(number);
			context.getSource().sendSuccess(() -> Component.literal("[golf] target " + targetNumber + ": "
				+ (target == null ? "not set" : target.position())), false);
		}
		return 1;
	}

	private static Vec3 standingPoint(ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		return new Vec3(feet.getX() + 0.5, feet.getY() + GolfBallEntity.BALL_RADIUS, feet.getZ() + 0.5);
	}

	private static int fail(CommandContext<CommandSourceStack> context, String message) {
		context.getSource().sendFailure(Component.literal("[golf] " + message));
		return 0;
	}

	private static int pickUp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().pickUp(player));
	}

	private static int tapIn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().tapIn(player));
	}

	private static int nextHole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		return sendResult(context, ActiveHoleService.instance().nextHole(player));
	}

	/**
	 * M10.1 server-only shot path: a vanilla client (or Bedrock via Geyser) can
	 * strike its nearest resting ball through chat, aiming along its look direction.
	 * The optional arguments default to full power, a perfect accuracy lane, and the
	 * standard trajectory, so {@code /golf swing} alone is a legal shot.
	 */
	private static int swing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (visitorBlocked(context, player)) return 0;
		float power = optionalFloat(context, "power", 1.0f);
		float accuracy = optionalFloat(context, "accuracy", 0.5f);
		ShotType shotType;
		try {
			shotType = optionalShotType(context);
		} catch (IllegalArgumentException exception) {
			context.getSource().sendFailure(Component.literal(exception.getMessage()));
			return 0;
		}
		ShotOutcome outcome = ShotService.attemptNearest(player, power, accuracy, shotType);
		if (outcome != ShotOutcome.SUCCESS) {
			context.getSource().sendFailure(Component.literal(outcome.description()));
			return 0;
		}
		return 1;
	}

	private static float optionalFloat(CommandContext<CommandSourceStack> context, String name,
			float fallback) {
		try {
			return FloatArgumentType.getFloat(context, name);
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private static ShotType optionalShotType(CommandContext<CommandSourceStack> context) {
		String raw;
		try {
			raw = StringArgumentType.getString(context, "type");
		} catch (IllegalArgumentException exception) {
			return ShotType.STANDARD;
		}
		for (ShotType candidate : ShotType.values()) {
			if (candidate.name().equalsIgnoreCase(raw) || candidate.displayName().equalsIgnoreCase(raw)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("[golf] unknown shot type '" + raw
			+ "'; use standard, chip, stinger, or flop");
	}

	/** Visitors (Bedrock/vanilla) can watch but not play; reject play commands with a clear message. */
	private static boolean visitorBlocked(CommandContext<CommandSourceStack> context, ServerPlayer player) {
		if (VisitorService.isVisitor(player)) {
			context.getSource().sendFailure(Component.literal(VisitorService.playRejection()));
			return true;
		}
		return false;
	}

	private static int spectatorView(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		VisitorService.instance().setMode(context.getSource().getPlayerOrException(),
			VisitorService.Mode.SPECTATOR);
		return 1;
	}

	private static int leaveSpectator(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		VisitorService.instance().setMode(context.getSource().getPlayerOrException(),
			VisitorService.Mode.SURVIVAL);
		return 1;
	}

	private static int visitorStatus(CommandContext<CommandSourceStack> context) {
		VisitorService service = VisitorService.instance();
		String spawn = service.hasViewpoint()
			? service.viewpoint().dimension() + " " + service.viewpoint().position()
			: "not set";
		context.getSource().sendSuccess(() -> Component.literal("[golf] visitor config: address="
			+ display(service.javaAddress()) + " | link=" + display(service.modLink())
			+ " | spawn=" + spawn), false);
		return 1;
	}

	private static int visitorAddress(CommandContext<CommandSourceStack> context) {
		String address = StringArgumentType.getString(context, "address");
		VisitorService.instance().setJavaAddress(address);
		context.getSource().sendSuccess(() -> Component.literal("[golf] Java address set to " + address), false);
		return 1;
	}

	private static int visitorLink(CommandContext<CommandSourceStack> context) {
		String link = StringArgumentType.getString(context, "link");
		VisitorService.instance().setModLink(link);
		context.getSource().sendSuccess(() -> Component.literal("[golf] mod link set to " + link), false);
		return 1;
	}

	private static int visitorSpawnSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		VisitorService.instance().setViewpoint(player.level().dimension().identifier().toString(),
			new Vec3(player.getX(), player.getY(), player.getZ()));
		context.getSource().sendSuccess(() -> Component.literal("[golf] visitor spawn set here"), true);
		return 1;
	}

	private static int visitorSpawnClear(CommandContext<CommandSourceStack> context) {
		VisitorService.instance().clearViewpoint();
		context.getSource().sendSuccess(() -> Component.literal("[golf] visitor spawn cleared"), false);
		return 1;
	}

	private static String display(String value) {
		return value == null || value.isBlank() ? "(unset)" : value;
	}

	private static int sendResult(CommandContext<CommandSourceStack> context, StartResult result) {
		if (!result.success()) {
			context.getSource().sendFailure(Component.literal(result.message()));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(result.message()), false);
		return 1;
	}
}
