package com.prillcode.minecraftgolf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.entity.EntityTypeTest;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.ball.BallState;
import com.prillcode.minecraftgolf.dev.DevelopmentHoleBuilder;
import com.prillcode.minecraftgolf.dev.DevelopmentHoleBuilder.Layout;
import com.prillcode.minecraftgolf.entity.GolfBallEntities;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;
import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.server.ActiveHoleService;

/**
 * Server-side developer launch and test controls (MILESTONES.md M1, ROADMAP
 * M001-S03).
 *
 * <p>A small op-gated {@code /golf} command group that drives the S02
 * {@link GolfBallEntity} through its existing server-authoritative API:
 *
 * <pre>
 * /golf spawn [x y z]                    summon a ball (drops and settles)
 * /golf launch &lt;forward&gt; &lt;up&gt; [id]   launch nearest (or given) ball
 * /golf inspect [id]                     print BallState of nearest (or given) ball
 * /golf clear                            remove every loaded golf ball
 * /golf dev preparehole                  explicitly prepare the configured flat test hole
 * </pre>
 *
 * <p>Commands mutate only server-authoritative state (ARCHITECTURE.md §2.1):
 * {@code launch} calls {@link GolfBallEntity#launch}, which applies the same
 * {@code BallPhysics.clampLaunch} ceiling as any future shot execution, and
 * {@code inspect} reads the authoritative {@link BallState} back out. There is
 * no client code here — this class lives in {@code src/main} and must stay
 * dedicated-server-safe.</p>
 *
 * <p>Registration is a normal Fabric command-api-v2 callback; the permission
 * gate mirrors vanilla {@code /summon} (game-master level) so the commands work
 * in a cheats-on single-player world and for operators on a server.</p>
 */
public final class GolfDevCommands {

	private static final double SPAWN_FORWARD = 2.0;
	private static final double SPAWN_UP = 1.0;
	private static final double MAX_LAUNCH_COMPONENT = 8.0;

	private static boolean registered;

	private GolfDevCommands() {
	}

	/**
	 * Installs the {@code /golf} command registration callback. Called from
	 * {@link MinecraftGolf#onInitialize}; safe on dedicated servers and on the
	 * integrated server of a single-player session.
	 */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		CommandRegistrationCallback.EVENT.register(GolfDevCommands::onRegisterCommands);
	}

	private static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess, CommandSelection environment) {
		dispatcher.register(Commands.literal("golf")
				.then(Commands.literal("spawn")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(GolfDevCommands::spawnAtExecutor)
						.then(Commands.argument("x", DoubleArgumentType.doubleArg())
								.then(Commands.argument("y", DoubleArgumentType.doubleArg())
										.then(Commands.argument("z", DoubleArgumentType.doubleArg())
												.executes(GolfDevCommands::spawnAtExplicit)))))
				.then(Commands.literal("launch")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("forward", DoubleArgumentType.doubleArg())
								.then(Commands.argument("up", DoubleArgumentType.doubleArg())
										.executes(GolfDevCommands::launchNearest)
										.then(Commands.argument("id", IntegerArgumentType.integer())
												.executes(GolfDevCommands::launchById)))))
				.then(Commands.literal("inspect")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(GolfDevCommands::inspectNearest)
						.then(Commands.argument("id", IntegerArgumentType.integer())
								.executes(GolfDevCommands::inspectById)))
				.then(Commands.literal("clear")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(GolfDevCommands::clearAll))
				.then(Commands.literal("dev")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("preparehole")
								.executes(GolfDevCommands::prepareHole))));
		MinecraftGolf.LOGGER.info(
			"Registered /golf developer commands (spawn, launch, inspect, clear, dev preparehole)");
	}

	// ------------------------------------------------------------------
	// /golf dev preparehole
	// ------------------------------------------------------------------

	private static int prepareHole(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		HoleDefinition hole = ActiveHoleService.instance().configuredHole();
		String currentDimension = level.dimension().identifier().toString();
		if (!hole.dimension().equals(currentDimension)) {
			source.sendFailure(Component.literal("[golf] configured hole is in " + hole.dimension()
				+ "; command source is in " + currentDimension));
			return 0;
		}
		try {
			Layout layout = DevelopmentHoleBuilder.prepare(level, hole);
			source.sendSuccess(() -> Component.literal(
				"[golf] prepared development hole: X[" + layout.minX() + ".." + layout.maxX()
					+ "] Y=" + layout.floorY() + " Z[" + layout.minZ() + ".." + layout.maxZ()
					+ "]"), true);
			return 1;
		} catch (IllegalArgumentException exception) {
			source.sendFailure(Component.literal("[golf] cannot prepare development hole: "
				+ exception.getMessage()));
			return 0;
		}
	}

	// ------------------------------------------------------------------
	// /golf spawn
	// ------------------------------------------------------------------

	private static int spawnAtExecutor(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		double yaw = Math.toRadians(source.getRotation().y);
		double dx = -Math.sin(yaw);
		double dz = Math.cos(yaw);
		net.minecraft.world.phys.Vec3 pos = source.getPosition();
		return spawn(ctx, pos.x + dx * SPAWN_FORWARD, pos.y + SPAWN_UP, pos.z + dz * SPAWN_FORWARD);
	}

	private static int spawnAtExplicit(CommandContext<CommandSourceStack> ctx) {
		return spawn(ctx,
				DoubleArgumentType.getDouble(ctx, "x"),
				DoubleArgumentType.getDouble(ctx, "y"),
				DoubleArgumentType.getDouble(ctx, "z"));
	}

	private static int spawn(CommandContext<CommandSourceStack> ctx, double x, double y, double z) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		if (level.isClientSide()) {
			source.sendFailure(Component.literal("golf: spawn must run on the server"));
			return 0;
		}
		GolfBallEntity ball = GolfBallEntities.GOLF_BALL.create(level, EntitySpawnReason.COMMAND);
		if (ball == null) {
			source.sendFailure(Component.literal("golf: failed to create golf ball entity"));
			return 0;
		}
		ball.setPos(x, y, z);
		if (!level.addFreshEntity(ball)) {
			source.sendFailure(Component.literal("golf: failed to add golf ball to the level"));
			return 0;
		}
		String msg = "golf: spawned ball #" + ball.getId() + " at " + fmt(x, y, z)
				+ " (drops and settles under gravity)";
		MinecraftGolf.LOGGER.info(msg);
		source.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// /golf launch <forward> <up> [id]
	// ------------------------------------------------------------------

	private static int launchNearest(CommandContext<CommandSourceStack> ctx) {
		GolfBallEntity target = nearestBall(ctx.getSource());
		if (target == null) {
			ctx.getSource().sendFailure(Component.literal(
					"golf: no golf ball nearby — spawn one with /golf spawn, or pass an entity id"));
			return 0;
		}
		return launch(ctx, target.getId());
	}

	private static int launchById(CommandContext<CommandSourceStack> ctx) {
		return launch(ctx, IntegerArgumentType.getInteger(ctx, "id"));
	}

	private static int launch(CommandContext<CommandSourceStack> ctx, int targetId) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		GolfBallEntity ball = findBall(level, targetId);
		if (ball == null) {
			source.sendFailure(Component.literal("golf: no golf ball with id " + targetId + " in this level"));
			return 0;
		}

		// Forward is horizontal speed along the executor's facing; up is the
		// vertical component. The entity clamps the total via clampLaunch.
		double forward = DoubleArgumentType.getDouble(ctx, "forward");
		double up = DoubleArgumentType.getDouble(ctx, "up");
		if (Double.isNaN(forward) || Double.isNaN(up)
				|| Math.abs(forward) > MAX_LAUNCH_COMPONENT || Math.abs(up) > MAX_LAUNCH_COMPONENT) {
			source.sendFailure(Component.literal(
					"golf: launch components must be finite and within \u00b1" + MAX_LAUNCH_COMPONENT
							+ " blocks/tick"));
			return 0;
		}

		double yaw = Math.toRadians(source.getRotation().y);
		Vec3 velocity = Vec3.of(-Math.sin(yaw) * forward, up, Math.cos(yaw) * forward);
		ball.launch(velocity);
		String msg = "golf: launched ball #" + ball.getId() + " with velocity " + fmt(velocity)
				+ " blocks/tick (clamped to config max)";
		MinecraftGolf.LOGGER.info(msg);
		source.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// /golf inspect [id]
	// ------------------------------------------------------------------

	private static int inspectNearest(CommandContext<CommandSourceStack> ctx) {
		GolfBallEntity target = nearestBall(ctx.getSource());
		if (target == null) {
			ctx.getSource().sendFailure(Component.literal(
					"golf: no golf ball loaded in this level — spawn one with /golf spawn, or pass an entity id"));
			return 0;
		}
		return inspect(ctx, target);
	}

	private static int inspectById(CommandContext<CommandSourceStack> ctx) {
		int id = IntegerArgumentType.getInteger(ctx, "id");
		GolfBallEntity ball = findBall(ctx.getSource().getLevel(), id);
		if (ball == null) {
			ctx.getSource().sendFailure(Component.literal("golf: no golf ball with id " + id + " in this level"));
			return 0;
		}
		return inspect(ctx, ball);
	}

	private static int inspect(CommandContext<CommandSourceStack> ctx, GolfBallEntity ball) {
		CommandSourceStack source = ctx.getSource();
		BallState state = ball.ballState();
		String msg;
		if (state == null) {
			msg = "golf: ball #" + ball.getId() + " at " + fmt(ball.position().x, ball.position().y, ball.position().z)
					+ " — no physics state yet (waits for the first server tick)";
		} else {
			double speedBlocksPerSecond = state.velocity().length() * 20.0;
			String motion = state.resting() ? "RESTING" : (state.grounded() ? "MOVING (rolling)" : "MOVING (airborne)");
			String owner = ball.owner() == null ? "unowned" : shortUuid(ball.owner());
			msg = "golf: ball #" + ball.getId() + " " + motion
					+ " | owner " + owner
					+ " | pos " + fmt(state.position())
					+ " | vel " + fmt(state.velocity()) + " blocks/tick"
					+ " | speed " + String.format("%.2f", speedBlocksPerSecond) + " blocks/s"
					+ " | grounded=" + state.grounded() + " resting=" + state.resting();
		}
		MinecraftGolf.LOGGER.info(msg);
		source.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// /golf clear
	// ------------------------------------------------------------------

	private static int clearAll(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		if (level.isClientSide()) {
			source.sendFailure(Component.literal("golf: clear must run on the server"));
			return 0;
		}
		java.util.List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		int removed = 0;
		for (GolfBallEntity ball : balls) {
			ball.discard();
			removed++;
		}
		String msg = "golf: cleared " + removed + " golf ball" + (removed == 1 ? "" : "s") + " from "
				+ level.dimension().identifier();
		MinecraftGolf.LOGGER.info(msg);
		source.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// Target resolution helpers
	// ------------------------------------------------------------------

	/**
	 * Nearest loaded golf ball to the executor, or {@code null} when the level
	 * has none loaded.
	 */
	private static GolfBallEntity nearestBall(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		java.util.List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		net.minecraft.world.phys.Vec3 origin = source.getPosition();
		GolfBallEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (GolfBallEntity ball : balls) {
			double d = ball.distanceToSqr(origin.x, origin.y, origin.z);
			if (d < bestDist) {
				bestDist = d;
				best = ball;
			}
		}
		return best;
	}

	private static GolfBallEntity findBall(ServerLevel level, int id) {
		java.util.List<? extends GolfBallEntity> balls =
				level.getEntities(EntityTypeTest.forClass(GolfBallEntity.class), ignored -> true);
		for (GolfBallEntity ball : balls) {
			if (ball.getId() == id) {
				return ball;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Formatting helpers
	// ------------------------------------------------------------------

	private static String fmt(Vec3 v) {
		return fmt(v.x(), v.y(), v.z());
	}

	private static String fmt(double x, double y, double z) {
		return String.format("(%.2f, %.2f, %.2f)", x, y, z);
	}

	private static String shortUuid(java.util.UUID uuid) {
		String s = uuid.toString();
		return s.substring(0, 8);
	}
}
