package pro.apdev.biomegolf.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.block.GolfBlocks;
import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.CourseLandscape;
import pro.apdev.biomegolf.course.CourseProtection;
import pro.apdev.biomegolf.course.CourseProtectionConfig;
import pro.apdev.biomegolf.course.CourseProtectionIndex;
import pro.apdev.biomegolf.course.ProtectionVerdict;
import pro.apdev.biomegolf.hole.HoleDefinition;

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
 *       adjacent placement position is protected (M8.10 S2),</li>
 *   <li>TNT placement/ignition (M8.10 S3), and newly spawned primed TNT
 *       ({@link ServerEntityEvents#ALLOW_LOAD}) inside a blast-margin-expanded
 *       perimeter (M8.10 S3).</li>
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
		BlockEvents.USE_WITHOUT_ITEM.register(CourseBlockBreakGuard::onUseWithoutItem);
		UseEntityCallback.EVENT.register(CourseBlockBreakGuard::onUseEntity);
		ServerEntityEvents.ALLOW_LOAD.register(CourseBlockBreakGuard::onAllowLoad);
		MinecraftGolf.LOGGER.info(
			"Registered course protection guard (breaks, placements, containers, entity use, and TNT;"
				+ " tee/cup vicinity radius {} plus landscape perimeters with a {} block TNT blast margin)",
			CourseProtectionConfig.DEFAULT.vicinityRadius(),
			CourseProtectionConfig.DEFAULT.tntBlastSafetyMargin());
	}

	/** Fabric BEFORE handler: returns true to allow the break, false to cancel it. */
	private static boolean onBeforeBlockBreak(Level world, Player player, BlockPos pos,
			BlockState state, BlockEntity blockEntity) {
		String dimension = dimension(world);
		// M8.15: an active Builder may edit only inside their selected course perimeter,
		// even when they are an operator and the landscape is locked. Everyone else keeps
		// the existing verdict rules, so one builder never relaxes protection for others.
		if (PlayerModeService.instance().isBuilder(player.getUUID())) {
			if (!PlayerModeService.instance().builderAllowsEdit(player.getUUID(), dimension,
					pos.getX(), pos.getY(), pos.getZ())) {
				MinecraftGolf.LOGGER.info("[golf] blocked builder {} from breaking outside their course at ({}, {}, {})",
					player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
				return false;
			}
			return true;
		}
		var zones = INDEX.zones(dimension);
		var landscapes = INDEX.landscapes(dimension);
		if (zones.isEmpty() && landscapes.isEmpty()) return true;
		boolean cupBlock = state.getBlock() == GolfBlocks.GOLF_CUP;
		if (!CourseProtection.mayBreak(zones, landscapes, pos.getX(), pos.getY(), pos.getZ(),
				cupBlock, isOperator(player))) {
			MinecraftGolf.LOGGER.info("[golf] blocked {} from breaking a protected course block at ({}, {}, {})",
				player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
			return false;
		}
		return true;
	}

	/**
	 * M8.10 S2/S3 Fabric handler: returns {@code null} to let vanilla handle the
	 * interaction, or {@link InteractionResult#CONSUME} to deny a protected
	 * placement or a protected TNT placement/ignition.
	 *
	 * <p>Only held {@link BlockItem}s (and TNT) are considered, so ordinary
	 * interactions (doors, buttons, levers, containers) still work inside a
	 * perimeter. For placements the clicked position and the adjacent placement
	 * position are both checked, so placing against a protected block from
	 * outside the perimeter is denied as well. {@code CONSUME} carries
	 * {@code consumesAction() == true}, which is what prevents
	 * {@code ServerPlayerGameMode} from falling through to
	 * {@code ItemStack#useOn}; {@code FAIL} would still place the block.</p>
	 */
	private static InteractionResult onUseItemOn(ItemStack stack, BlockState state, Level world,
			BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		// Server is authoritative for world mutation; client prediction corrects itself.
		if (world.isClientSide()) return null;
		String dimension = dimension(world);
		boolean operator = isOperator(player);

		// M8.15: an active Builder may place only currently-supplied palette items, and
		// only inside their selected course perimeter. This is server enforcement, not a
		// filtered Creative screen.
		if (PlayerModeService.instance().isBuilder(player.getUUID())) {
			// A Builder must not open containers, which would be an item export path.
			if (state.getMenuProvider(world, pos) != null) {
				MinecraftGolf.LOGGER.info("[golf] blocked builder {} from opening a container at ({}, {}, {})",
					player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
				return InteractionResult.CONSUME;
			}
			BlockPos placementPos = pos.relative(hitResult.getDirection());
			boolean inside = PlayerModeService.instance().builderAllowsEdit(player.getUUID(), dimension,
					pos.getX(), pos.getY(), pos.getZ())
				&& PlayerModeService.instance().builderAllowsEdit(player.getUUID(), dimension,
					placementPos.getX(), placementPos.getY(), placementPos.getZ());
			if (!inside || !BuilderPaletteService.instance().isAllowed(stack)) {
				MinecraftGolf.LOGGER.info("[golf] blocked builder {} from placing {} at ({}, {}, {})",
					player.getName().getString(), stack.getItem(), placementPos.getX(), placementPos.getY(),
					placementPos.getZ());
				return InteractionResult.CONSUME;
			}
			return null;
		}

		// TNT blocks/minecarts: use the blast-margin-expanded perimeter so a charge
		// placed just outside a boundary that could still reach in is denied too.
		if (stack.is(Items.TNT) || stack.is(Items.TNT_MINECART)) {
			BlockPos placementPos = pos.relative(hitResult.getDirection());
			ProtectionVerdict verdict = INDEX.tntVerdict(dimension, pos.getX(), pos.getY(), pos.getZ())
				.mostRestrictive(INDEX.tntVerdict(dimension,
					placementPos.getX(), placementPos.getY(), placementPos.getZ()));
			return denyOrPass(verdict, operator, player, "placing TNT at", placementPos);
		}

		// Direct ignition of an existing TNT block (flint & steel / fire charge).
		if (state.is(Blocks.TNT)
			&& (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
			return denyOrPass(INDEX.tntVerdict(dimension, pos.getX(), pos.getY(), pos.getZ()),
				operator, player, "igniting TNT at", pos);
		}

		// Ordinary block placement: exact positions, no blast margin.
		if (stack.getItem() instanceof BlockItem) {
			BlockPos placementPos = pos.relative(hitResult.getDirection());
			ProtectionVerdict verdict = INDEX.verdict(dimension, pos.getX(), pos.getY(), pos.getZ())
				.mostRestrictive(INDEX.verdict(dimension,
					placementPos.getX(), placementPos.getY(), placementPos.getZ()));
			return denyOrPass(verdict, operator, player, "placing a protected course block at", placementPos);
		}
		return null;
	}

	/**
	 * M8.10 S3 catch-all Fabric handler: cancels a newly spawned (not loaded from
	 * disk) primed TNT inside the blast-margin-expanded perimeter. Catches what
	 * the item-level checks cannot: TNT placed before the perimeter was authored,
	 * and redstone/dispenser/fire/flaming-arrow ignition.
	 *
	 * <p>Lock semantics are honoured: a locked perimeter cancels every new charge.
	 * An unlocked perimeter ({@link ProtectionVerdict#DENY_NON_OP}) cancels the
	 * charge unless the player who primed it is an operator, so operators regain
	 * TNT while non-operator ignition is still refused. Saved entities are never
	 * cancelled ({@code loadedFromDisk}), so chunk loads cannot delete charges.</p>
	 */
	private static boolean onAllowLoad(Entity entity, ServerLevel level, EntitySpawnReason reason,
			boolean loadedFromDisk) {
		if (loadedFromDisk) {
			return true;
		}
		// M8.15: cancel items dropped by a Builder so palette/non-palette items cannot be
		// exported into the world for a World player to collect.
		if (entity instanceof ItemEntity item && item.getOwner() instanceof Player owner
				&& PlayerModeService.instance().isBuilder(owner.getUUID())) {
			MinecraftGolf.LOGGER.info("[golf] cancelled an item dropped by builder {}",
				owner.getName().getString());
			return false;
		}
		if (!(entity instanceof PrimedTnt tnt)) {
			return true;
		}
		// M8.15: builders are never supplied TNT; refuse any charge they prime.
		if (tnt.getOwner() instanceof Player owner
				&& PlayerModeService.instance().isBuilder(owner.getUUID())) {
			MinecraftGolf.LOGGER.info("[golf] cancelled a primed TNT owned by builder {}",
				owner.getName().getString());
			return false;
		}
		BlockPos pos = entity.blockPosition();
		ProtectionVerdict verdict = INDEX.tntVerdict(dimension(level),
			pos.getX(), pos.getY(), pos.getZ());
		if (verdict == ProtectionVerdict.ALLOW) {
			return true;
		}
		if (verdict == ProtectionVerdict.DENY_NON_OP && tnt.getOwner() instanceof Player owner
			&& isOperator(owner)) {
			return true;
		}
		MinecraftGolf.LOGGER.info("[golf] cancelled a primed TNT at ({}, {}, {}) inside a protected course perimeter",
			pos.getX(), pos.getY(), pos.getZ());
		return false;
	}

	/**
	 * M8.15: a Builder's empty-hand right-click removes the clicked block instantly
	 * (and without drops) when it is inside their selected course perimeter,
	 * restoring the one-click terrain clearing of creative course building without
	 * granting Creative. Containers are denied first so inventory contents are never
	 * erased, and scope plus structural blocks are enforced server-side.
	 */
	private static InteractionResult onUseWithoutItem(BlockState state, Level world, BlockPos pos,
			Player player, BlockHitResult hitResult) {
		if (world.isClientSide() || !PlayerModeService.instance().isBuilder(player.getUUID())) {
			return null;
		}
		if (state.getMenuProvider(world, pos) == null
				&& canBuilderErase(state, world, pos, (ServerPlayer) player)) {
			eraseBuilderBlock(state, world, pos, (ServerPlayer) player);
			return InteractionResult.CONSUME;
		}
		if (state.getMenuProvider(world, pos) != null) {
			MinecraftGolf.LOGGER.info("[golf] blocked builder {} from opening a container at ({}, {}, {})",
				player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
			return InteractionResult.CONSUME;
		}
		return null;
	}

	/**
	 * Builder erase is scoped to the selected course perimeter and skips
	 * survival-unbreakable blocks (bedrock, barrier) and the command-managed
	 * cup/flag assembly.
	 */
	private static boolean canBuilderErase(BlockState state, Level world, BlockPos pos, ServerPlayer player) {
		if (!PlayerModeService.instance().builderAllowsEdit(player.getUUID(), dimension(world),
				pos.getX(), pos.getY(), pos.getZ())) {
			return false;
		}
		if (state.getDestroySpeed(world, pos) < 0.0F) {
			return false;
		}
		Block block = state.getBlock();
		return block != GolfBlocks.GOLF_CUP && block != GolfBlocks.GOLF_FLAG && block != GolfBlocks.GOLF_FLAG_TOP;
	}

	/** Server-side no-drop removal with break particles and sound. */
	private static void eraseBuilderBlock(BlockState state, Level world, BlockPos pos, ServerPlayer player) {
		MinecraftGolf.LOGGER.info("[golf] builder {} erased {} at ({}, {}, {})",
			player.getName().getString(), state.getBlock(), pos.getX(), pos.getY(), pos.getZ());
		world.levelEvent(2001, pos, Block.getId(state));
		world.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
		world.destroyBlock(pos, false);
	}

	/** M8.15: a Builder must not interact with entities (item frames, storage, animals, trades). */
	private static InteractionResult onUseEntity(Player player, Level world, InteractionHand hand,
			Entity entity, EntityHitResult hitResult) {
		if (world.isClientSide() || !PlayerModeService.instance().isBuilder(player.getUUID())) {
			return null;
		}
		MinecraftGolf.LOGGER.info("[golf] blocked builder {} from interacting with {}",
			player.getName().getString(), entity.getType());
		return InteractionResult.CONSUME;
	}

	private static InteractionResult denyOrPass(ProtectionVerdict verdict, boolean operator,
			Player player, String action, BlockPos pos) {
		if (!verdict.denies(operator)) {
			return null;
		}
		MinecraftGolf.LOGGER.info("[golf] blocked {} from {} ({}, {}, {})",
			player.getName().getString(), action, pos.getX(), pos.getY(), pos.getZ());
		return InteractionResult.CONSUME;
	}

	private static String dimension(Level world) {
		return world.dimension().identifier().toString();
	}

	private static boolean isOperator(Player player) {
		return player instanceof ServerPlayer serverPlayer
			&& Commands.LEVEL_GAMEMASTERS.check(serverPlayer.permissions());
	}
}
