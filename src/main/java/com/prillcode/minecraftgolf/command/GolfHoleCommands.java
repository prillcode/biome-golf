package com.prillcode.minecraftgolf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.server.ActiveHoleService;
import com.prillcode.minecraftgolf.server.ActiveHoleService.StartResult;

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
			.then(Commands.literal("round")
				.then(Commands.literal("create").executes(GolfHoleCommands::createRound))
				.then(Commands.literal("join").executes(GolfHoleCommands::joinRound))
				.then(Commands.literal("start").executes(GolfHoleCommands::startRound))
				.then(Commands.literal("leave").executes(GolfHoleCommands::leaveRound))
				.then(Commands.literal("status").executes(GolfHoleCommands::roundStatus)))
			.then(Commands.literal("hole")
				.then(Commands.literal("start").executes(GolfHoleCommands::start))
				.then(Commands.literal("restart").executes(GolfHoleCommands::restart))
				.then(Commands.literal("abandon").executes(GolfHoleCommands::abandon))
				.then(Commands.literal("status").executes(GolfHoleCommands::status)))
			.then(Commands.literal("practiceball").executes(GolfHoleCommands::dropPracticeBall))
			.then(Commands.literal("pickup").executes(GolfHoleCommands::pickUp))
			.then(Commands.literal("nexthole").executes(GolfHoleCommands::nextHole)));
		MinecraftGolf.LOGGER.info(
			"Registered Ready Golf round, hole lifecycle, practice ball, Pick Up, and next-hole commands");
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

	private static int createRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().createRound(
			context.getSource().getPlayerOrException()));
	}

	private static int joinRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().joinRound(
			context.getSource().getPlayerOrException()));
	}

	private static int startRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().startRound(
			context.getSource().getPlayerOrException()));
	}

	private static int leaveRound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return sendResult(context, ActiveHoleService.instance().leaveRound(
			context.getSource().getPlayerOrException()));
	}

	private static int roundStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		context.getSource().sendSuccess(
			() -> Component.literal(ActiveHoleService.instance().roundStatus(player)), false);
		return 1;
	}

	private static int restart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().restart(player));
	}

	private static int abandon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().abandon(player));
	}

	private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		context.getSource().sendSuccess(
			() -> Component.literal(ActiveHoleService.instance().status(player)), false);
		return 1;
	}

	private static int dropPracticeBall(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().dropPracticeBall(player));
	}

	private static int pickUp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return sendResult(context, ActiveHoleService.instance().pickUp(player));
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
