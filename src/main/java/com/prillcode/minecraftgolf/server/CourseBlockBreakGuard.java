package com.prillcode.minecraftgolf.server;

import java.util.List;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseProtection;
import com.prillcode.minecraftgolf.course.CourseProtectionConfig;
import com.prillcode.minecraftgolf.course.ProtectedZone;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/**
 * M7 S1 server-side block-break guard protecting the authored course from
 * creative-mode destruction (MILESTONES.md M7 playtest finding).
 *
 * <p>Uses the Fabric {@link PlayerBlockBreakEvents#BEFORE} hook (no Mixin):
 * breaks of blocks inside a hole's tee/cup vicinity zones are cancelled, and
 * the cup/flag block itself is always protected. Operator/dev-level players
 * (the same permission gate as {@code /golf dev preparecourse}) are exempt so
 * course repair remains possible. Everything outside the zones stays breakable,
 * preserving the fun of clearing in-the-way trees/rocks.</p>
 */
public final class CourseBlockBreakGuard {

	private static boolean registered;

	private CourseBlockBreakGuard() {
	}

	/** Installs the block-break guard; safe on dedicated and integrated servers. */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		PlayerBlockBreakEvents.BEFORE.register(CourseBlockBreakGuard::onBeforeBlockBreak);
		MinecraftGolf.LOGGER.info(
			"Registered course block-break guard (tee/cup vicinity protection, {} block radius)",
			CourseProtectionConfig.DEFAULT.vicinityRadius());
	}

	/** Fabric BEFORE handler: returns true to allow the break, false to cancel it. */
	private static boolean onBeforeBlockBreak(Level world, Player player, BlockPos pos,
			BlockState state, BlockEntity blockEntity) {
		// Zone membership from authored metadata; guard applies only in the course dimension.
		ActiveHoleService service = ActiveHoleService.instance();
		CourseDefinition course = service.configuredCourseOrNull();
		String dimension;
		List<ProtectedZone> zones;
		if (course != null) {
			dimension = course.dimension();
			zones = CourseProtection.zonesFor(course, CourseProtectionConfig.DEFAULT);
		} else {
			HoleDefinition hole = service.configuredHoleOrNull();
			if (hole == null) {
				return true;
			}
			dimension = hole.dimension();
			zones = CourseProtection.zonesFor(hole, CourseProtectionConfig.DEFAULT);
		}
		if (!world.dimension().identifier().toString().equals(dimension)) {
			return true;
		}
		boolean hasDevPermission = player instanceof ServerPlayer serverPlayer
			&& Commands.LEVEL_GAMEMASTERS.check(serverPlayer.permissions());
		boolean cupBlock = state.getBlock() == GolfBlocks.GOLF_CUP;
		if (!CourseProtection.mayBreak(zones, pos.getX(), pos.getY(), pos.getZ(),
				cupBlock, hasDevPermission)) {
			MinecraftGolf.LOGGER.info("[golf] blocked {} from breaking a protected course block at ({}, {}, {})",
				player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
			return false;
		}
		return true;
	}
}
