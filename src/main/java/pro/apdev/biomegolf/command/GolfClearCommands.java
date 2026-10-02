package pro.apdev.biomegolf.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import pro.apdev.biomegolf.clear.ClearGeometry;
import pro.apdev.biomegolf.clear.ClearGeometry.Axis;
import pro.apdev.biomegolf.course.ProtectionVerdict;
import pro.apdev.biomegolf.server.ClearItemsService;
import pro.apdev.biomegolf.server.ClearItemsService.ResolvedGroup;
import pro.apdev.biomegolf.server.CourseBlockBreakGuard;
import pro.apdev.biomegolf.server.PlayerModeService;

/**
 * Builder-mode bulk block clearing.
 *
 * <p>Implements the {@code /golf clear} tree (registered by
 * {@link GolfDevCommands}, op-gated, Builder-mode-only). A run clears a bounded
 * corridor of blocks that match the named target group: depth blocks ahead on
 * the nearest cardinal axis from the player's yaw, an odd width centered on the
 * player, and a signed height from feet level. Matching blocks become air;
 * everything else is untouched. Tee/cup vicinities and locked course landscapes
 * are always preserved; Skipped matches are counted and reported, never
 * silently dropped, and active golf play blocks the whole operation.</p>
 */
public final class GolfClearCommands {

	private static final int DEFAULT_DEPTH = 10;
	private static final int DEFAULT_WIDTH = 9;
	private static final int DEFAULT_HEIGHT = 32;
	private static final String DEFAULT_ITEM = "trees";

	/** Per-player session defaults; memory only, reset on disconnect and server restart. */
	private record Session(int depth, int width, int height, String itemName) {
		Session() {
			this(DEFAULT_DEPTH, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_ITEM);
		}

		Session withDepth(int depth) {
			return new Session(depth, width, height, itemName);
		}

		Session withWidth(int width) {
			return new Session(depth, width, height, itemName);
		}

		Session withHeight(int height) {
			return new Session(depth, width, height, itemName);
		}

		Session withItemName(String itemName) {
			return new Session(depth, width, height, itemName);
		}
	}

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

	private GolfClearCommands() {
	}

	private static Session session(UUID playerId) {
		return SESSIONS.computeIfAbsent(playerId, ignored -> new Session());
	}

	private static ServerPlayer playerOrNone(CommandContext<CommandSourceStack> ctx) {
		return ctx.getSource().getEntity() instanceof ServerPlayer player ? player : null;
	}

	private static String usageText() {
		return "[golf] /golf clear <group> [depth] [width] — clear a bounded corridor of a target "
			+ "group (trees, logs, leaves, ground). Settings: /golf clear depth|width|height [n], "
			+ "item <name>, status, reset, reload. Builder mode only.";
	}

	static int usage(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.literal(usageText()), false);
		return 1;
	}

	static int status(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		Session current = player == null ? new Session() : session(player.getUUID());
		String groups = ClearItemsService.instance().describe();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear status: item '"
			+ current.itemName() + "', depth " + current.depth() + ", width " + current.width()
			+ ", height " + (current.height() > 0 ? "+" : "") + current.height()
			+ "; groups: " + groups + (player == null
				? " (console defaults shown)"
				: " (session-local; /golf clear reset restores defaults)")), false);
		return 1;
	}

	static int reset(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] /golf clear reset must be run by a player"));
			return 0;
		}
		SESSIONS.remove(player.getUUID());
		ctx.getSource().sendSuccess(() -> Component.literal(
			"[golf] clear settings reset to defaults: item 'trees', depth 10, width 9, height +32"), false);
		return 1;
	}

	static int reload(CommandContext<CommandSourceStack> ctx) {
		ClearItemsService.ReloadResult result = ClearItemsService.instance().reload();
		if (result.success()) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] " + result.message()), true);
			return 1;
		}
		ctx.getSource().sendFailure(Component.literal("[golf] " + result.message()));
		return 0;
	}

	static int depthShow(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		Session current = player == null ? new Session() : session(player.getUUID());
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear depth is " + current.depth()
			+ " (1.." + ClearGeometry.MAX_DEPTH + ")"), false);
		return 1;
	}

	static int widthShow(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		Session current = player == null ? new Session() : session(player.getUUID());
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear width is " + current.width()
			+ " (odd, 1.." + ClearGeometry.MAX_WIDTH + "; even input rounds up)"), false);
		return 1;
	}

	static int heightShow(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		Session current = player == null ? new Session() : session(player.getUUID());
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear height is "
			+ (current.height() > 0 ? "+" : "") + current.height()
			+ " (signed, " + ClearGeometry.MIN_HEIGHT + ".." + ClearGeometry.MAX_HEIGHT + ", nonzero)"), false);
		return 1;
	}

	static int itemShow(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		Session current = player == null ? new Session() : session(player.getUUID());
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear item is '" + current.itemName()
			+ "'; groups: " + ClearItemsService.instance().describe()), false);
		return 1;
	}

	static int depthSet(CommandContext<CommandSourceStack> ctx) {
		return setSessionValue(ctx, IntegerArgumentType.getInteger(ctx, "n"),
			(depth, player) -> {
				SESSIONS.put(player.getUUID(), session(player.getUUID()).withDepth(depth));
				return "[golf] clear depth set to " + depth + " for this session";
			});
	}

	static int widthSet(CommandContext<CommandSourceStack> ctx) {
		return setSessionValue(ctx, IntegerArgumentType.getInteger(ctx, "n"),
			(width, player) -> {
				int odd = ClearGeometry.oddWidth(width);
				SESSIONS.put(player.getUUID(), session(player.getUUID()).withWidth(odd));
				String note = odd == width ? "" : " (rounded up from " + width + " to an odd width)";
				return "[golf] clear width set to " + odd + note + " for this session";
			});
	}

	static int heightSet(CommandContext<CommandSourceStack> ctx) {
		int height = IntegerArgumentType.getInteger(ctx, "h");
		if (height == 0) {
			ctx.getSource().sendFailure(Component.literal("[golf] clear height must be nonzero"));
			return 0;
		}
		return setSessionValue(ctx, height, (value, player) -> {
			SESSIONS.put(player.getUUID(), session(player.getUUID()).withHeight(value));
			return "[golf] clear height set to " + (value > 0 ? "+" : "") + value + " for this session";
		});
	}

	static int itemSet(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNone(ctx);
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] /golf clear item must be run by a player"));
			return 0;
		}
		String name = StringArgumentType.getString(ctx, "name");
		ResolvedGroup group = ClearItemsService.instance().resolve(name);
		if (group == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] unknown clear group '" + name
				+ "'; available: " + ClearItemsService.instance().describe()));
			return 0;
		}
		SESSIONS.put(player.getUUID(), session(player.getUUID()).withItemName(name));
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] clear item set to '" + name + "'"), false);
		return 1;
	}

	private interface SessionSetter {
		String apply(int value, ServerPlayer player);
	}

	private static int setSessionValue(CommandContext<CommandSourceStack> ctx, int value, SessionSetter setter) {
		ServerPlayer player = playerOrNone(ctx);
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] clear settings must be run by a player"));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(setter.apply(value, player)), false);
		return 1;
	}

	private record RunTarget(ServerPlayer player, ResolvedGroup group, Session session) {
	}

	/** No explicit dimensions: run with the session defaults. */
	static int runWithGroup(CommandContext<CommandSourceStack> ctx) {
		RunTarget target = resolveTarget(ctx);
		if (target == null) {
			return 0;
		}
		return executeClear(ctx, target.group(), target.session().depth(), target.session().width(),
			target.session().height(), target.player());
	}

	/** Explicit depth only: width and height come from the session. */
	static int runWithGroupDepth(CommandContext<CommandSourceStack> ctx) {
		RunTarget target = resolveTarget(ctx);
		if (target == null) {
			return 0;
		}
		return executeClear(ctx, target.group(), IntegerArgumentType.getInteger(ctx, "depth"),
			target.session().width(), target.session().height(), target.player());
	}

	/** Explicit depth and width: height comes from the session. */
	static int runWithGroupDepthWidth(CommandContext<CommandSourceStack> ctx) {
		RunTarget target = resolveTarget(ctx);
		if (target == null) {
			return 0;
		}
		return executeClear(ctx, target.group(), IntegerArgumentType.getInteger(ctx, "depth"),
			IntegerArgumentType.getInteger(ctx, "width"), target.session().height(), target.player());
	}

	/** Common gates for a clear run; sends the failure and returns null on rejection. */
	private static RunTarget resolveTarget(CommandContext<CommandSourceStack> ctx) {
		if (GolfDevCommands.activePlayBlocksWorldMutation(ctx)) {
			return null;
		}
		ServerPlayer player = playerOrNone(ctx);
		if (player == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] /golf clear must be run by a player"));
			return null;
		}
		if (!PlayerModeService.instance().isBuilder(player.getUUID())) {
			ctx.getSource().sendFailure(Component.literal(
				"[golf] /golf clear is a Builder-mode tool; use /golf mode build first"));
			return null;
		}
		String groupName = StringArgumentType.getString(ctx, "group");
		ResolvedGroup group = ClearItemsService.instance().resolve(groupName);
		if (group == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] unknown clear group '" + groupName
				+ "'; available: " + ClearItemsService.instance().describe()));
			return null;
		}
		return new RunTarget(player, group, session(player.getUUID()));
	}

	private static int executeClear(CommandContext<CommandSourceStack> ctx, ResolvedGroup group,
			int depth, int width, int height, ServerPlayer player) {
		CommandSourceStack source = ctx.getSource();
		ServerLevel level = source.getLevel();
		if (level.isClientSide()) {
			source.sendFailure(Component.literal("[golf] clear must run on the server"));
			return 0;
		}
		net.minecraft.world.phys.Vec3 position = source.getPosition();
		BlockPos feet = BlockPos.containing(position.x, position.y, position.z);
		Axis axis = ClearGeometry.axisForYaw(source.getRotation().y);
		// Even widths from a per-run argument round up to odd, matching the session setter.
		int effectiveWidth = ClearGeometry.oddWidth(width);
		ClearGeometry.ClearBox box = ClearGeometry.box(
			feet.getX(), feet.getY(), feet.getZ(), axis, depth, effectiveWidth, height);

		int cleared = 0;
		int skippedProtected = 0;
		int skippedLocked = 0;
		int skippedUnloaded = 0;
		int skippedBounds = 0;
		for (BlockPos pos : BlockPos.betweenClosed(
			box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			if (level.isOutsideBuildHeight(pos)) {
				skippedBounds++;
				continue;
			}
			if (!level.hasChunkAt(pos.getX(), pos.getZ())) {
				skippedUnloaded++;
				continue;
			}
			ProtectionVerdict verdict = CourseBlockBreakGuard.bulkVerdict(level, pos);
			if (verdict == ProtectionVerdict.DENY_ALL) {
				skippedLocked++;
				continue;
			}
			if (verdict == ProtectionVerdict.DENY_NON_OP) {
				skippedProtected++;
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!group.matches(state)) {
				continue;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			cleared++;
		}

		StringBuilder summary = new StringBuilder("[golf] cleared ")
			.append(cleared).append(" '").append(group.name()).append("' block(s) ")
			.append("in a ").append(depth).append("x").append(height).append("x")
			.append(effectiveWidth).append(" (DxHxW) corridor on ").append(axis)
			.append(" (").append(level.dimension().identifier().toString()).append(")");
		if (skippedProtected > 0 || skippedLocked > 0) {
			summary.append("; protected: ").append(skippedProtected)
				.append(", locked: ").append(skippedLocked);
		}
		if (skippedUnloaded > 0) {
			summary.append(", unloaded: ").append(skippedUnloaded);
		}
		if (skippedBounds > 0) {
			summary.append(", outside build height: ").append(skippedBounds);
		}
		String message = summary.toString();
		source.sendSuccess(() -> Component.literal(message), true);
		return 1;
	}
}