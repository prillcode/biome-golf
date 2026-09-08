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

/** Player-facing lifecycle commands for the one configured M4 hole. */
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
			.then(Commands.literal("hole")
				.then(Commands.literal("start").executes(GolfHoleCommands::start))
				.then(Commands.literal("status").executes(GolfHoleCommands::status)))
			.then(Commands.literal("pickup").executes(GolfHoleCommands::pickUp)));
		MinecraftGolf.LOGGER.info("Registered /golf hole start|status and /golf pickup commands");
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

	private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		context.getSource().sendSuccess(
			() -> Component.literal(ActiveHoleService.instance().status(player.getUUID())), false);
		return 1;
	}

	private static int pickUp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		StartResult result = ActiveHoleService.instance().pickUp(player);
		if (!result.success()) {
			context.getSource().sendFailure(Component.literal(result.message()));
			return 0;
		}
		context.getSource().sendSuccess(() -> Component.literal(result.message()), false);
		return 1;
	}
}
