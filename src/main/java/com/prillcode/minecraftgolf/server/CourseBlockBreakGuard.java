package com.prillcode.minecraftgolf.server;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.block.GolfBlocks;
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseLandscape;
import com.prillcode.minecraftgolf.course.CourseProtection;
import com.prillcode.minecraftgolf.course.CourseProtectionConfig;
import com.prillcode.minecraftgolf.course.CourseProtectionIndex;
import com.prillcode.minecraftgolf.course.ProtectionVerdict;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/**
 * Server-side course protection guard for authored courses.
 *
 * <p>Originally the M7 S1 block-break guard; M8.10 extends it to the
 * {@link ProtectionVerdict} model and whole-course landscape perimeters. It
 * protects, using only public Fabric events (no Mixin):</p>
 *
 * <ul>
 *   <li>block breaks ({@link PlayerBlockBreakEvents#BEFORE}) inside tee/cup
 *       vicinity or a landscape perimeter,</li>
 *   <li>block placement ({@link BlockEvents#USE_ITEM_ON}) when the clicked or
 *       adjacent placement position is protected (M8.10 S2).</li>
 * </ul>
 *
 * <p>The cup/flag block is always non-operator protected. Operators (the same
 * gamemaster permission gate as {@code /golf dev preparecourse}) are exempt
 * under {@link ProtectionVerdict#DENY_NON_OP}, but a locked landscape perimeter
 * ({@link ProtectionVerdict#DENY_ALL}) denies every player so the lock removes
 * the operator exemption. Everything outside the protected regions stays fully
 * editable.</p>
 */
public final class CourseBlockBreakGuard {

	private static boolean registered;
	private static final CourseProtectionIndex INDEX = new CourseProtectionIndex();

	private CourseBlockBreakGuard() {
	}

	public static void replaceAuthoredCourses(java.util.List<CourseDefinition> courses,
			java.util.List<CourseLandscape> landscapes) {
		INDEX.replaceAuthoredCourses(courses, landscapes);
	}

	public static void replaceConfiguredCourse(CourseDefinition course) {
		INDEX.replaceConfiguredCourse(course);
	}

	public static void replaceConfiguredHole(HoleDefinition hole) {
		INDEX.replaceConfiguredHole(hole);
	}

	public static void clearConfigured() { INDEX.clearConfigured(); }
	public static void clear() { INDEX.clear(); }

	/** Installs the guard; safe on dedicated and integrated servers. */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		PlayerBlockBreakEvents.BEFORE.register(CourseBlockBreakGuard::onBeforeBlockBreak);
		BlockEvents.USE_ITEM_ON.register(CourseBlockBreakGuard::onUseItemOn);
		MinecraftGolf.LOGGER.info(
			"Registered course protection guard (breaks and placements; tee/cup vicinity"
				+ " radius {} plus landscape perimeters)",
			CourseProtectionConfig.DEFAULT.vicinityRadius());
	}

	/** Fabric BEFORE handler: returns true to allow the break, false to cancel it. */
	private static boolean onBeforeBlockBreak(Level world, Player player, BlockPos pos,
			BlockState state, BlockEntity blockEntity) {
		var zones = INDEX.zones(dimension(world));
		var landscapes = INDEX.landscapes(dimension(world));
		if (zones.isEmpty() && landscapes.isEmpty()) return true;
		boolean cupBlock = state.getBlock() == GolfBlocks.GOLF_CUP;
		if (!CourseProtection.mayBreak(zones, landscapes, pos.getX(), pos.getY(), pos.getZ(),
				cupBlock, hasOperatorPermission(player))) {
			MinecraftGolf.LOGGER.info("[golf] blocked {} from breaking a protected course block at ({}, {}, {})",
				player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
			return false;
		}
		return true;
	}

	/**
	 * M8.10 S2 Fabric handler: returns {@code null} to let vanilla handle the
	 * interaction, or {@link InteractionResult#CONSUME} to deny a protected
	 * placement.
	 *
	 * <p>Only held {@link BlockItem}s are considered, so ordinary interactions
	 * (doors, buttons, levers, containers) still work inside a perimeter. The
	 * clicked position and the adjacent placement position are both checked, so
	 * placing against a protected block from outside the perimeter is denied as
	 * well. {@code CONSUME} carries {@code consumesAction() == true}, which is
	 * what prevents {@code ServerPlayerGameMode} from falling through to
	 * {@code ItemStack#useOn}; {@code FAIL} would still place the block.</p>
	 */
	private static InteractionResult onUseItemOn(ItemStack stack, BlockState state, Level world,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		// Server is authoritative for world mutation; client prediction corrects itself.
		if (world.isClientSide()) return null;
		if (!(stack.getItem() instanceof BlockItem)) return null;

		String dimension = dimension(world);
		ProtectionVerdict clicked = INDEX.verdict(dimension, pos.getX(), pos.getY(), pos.getZ());
		BlockPos placementPos = pos.relative(hitResult.getDirection());
		ProtectionVerdict placement = INDEX.verdict(dimension,
			placementPos.getX(), placementPos.getY(), placementPos.getZ());
		ProtectionVerdict verdict = clicked.mostRestrictive(placement);
		if (!verdict.denies(hasOperatorPermission(player))) {
			return null;
		}
		MinecraftGolf.LOGGER.info("[golf] blocked {} from placing a protected course block at ({}, {}, {})",
			player.getName().getString(), placementPos.getX(), placementPos.getY(), placementPos.getZ());
		return InteractionResult.CONSUME;
	}

	private static String dimension(Level world) {
		return world.dimension().identifier().toString();
	}

	private static boolean hasOperatorPermission(Player player) {
		return player instanceof ServerPlayer serverPlayer
			&& Commands.LEVEL_GAMEMASTERS.check(serverPlayer.permissions());
	}
}
