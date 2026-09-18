package com.prillcode.minecraftgolf.command;

import com.mojang.brigadier.CommandDispatcher;
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

import java.util.UUID;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.server.ActiveHoleService.StartResult;
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
				.then(Commands.literal("clear").executes(GolfHoleCommands::clearPracticeBalls)))
			.then(Commands.literal("pickup").executes(GolfHoleCommands::pickUp))
			.then(Commands.literal("tapin").executes(GolfHoleCommands::tapIn))
			.then(Commands.literal("nexthole").executes(GolfHoleCommands::nextHole)));
		MinecraftGolf.LOGGER.info(
			"Registered Ready Golf round, hole lifecycle, practice ball, practice clear, Pick Up, and next-hole commands");
	}

	private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
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
		context.getSource().sendSuccess(() -> Component.literal("Solo: /golf course play [courseId] [hole] | /golf hole start [hole]"), false);
		return 1;
	}

	private static int startAtHole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().start(
			context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "hole")));
	}

	private static int createRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().createRound(
			context.getSource().getPlayerOrException()));
	}

	private static int createRoundForCourse(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().createRound(
			StringArgumentType.getString(context, "courseId"),
			context.getSource().getPlayerOrException()));
	}

	private static int joinRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().joinRound(
			context.getSource().getPlayerOrException()));
	}

	private static int joinSpecificRound(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
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
		return sendResult(context, ActiveHoleService.instance().startRound(
			context.getSource().getPlayerOrException()));
	}

	private static int leaveRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().leaveRound(
			context.getSource().getPlayerOrException()));
	}

	private static int restartRound(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().restartRound(
			context.getSource().getPlayerOrException()));
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
		return sendResult(context, ActiveHoleService.instance().dropPracticeBall(player));
	}

	private static int clearPracticeBalls(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().clearPracticeBalls(
			context.getSource().getPlayerOrException()));
	}

	private static int pickUp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().pickUp(player));
	}

	private static int tapIn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().tapIn(
			context.getSource().getPlayerOrException()));
	}

	private static int nextHole(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().nextHole(player));
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
