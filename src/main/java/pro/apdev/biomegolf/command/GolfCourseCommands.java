package pro.apdev.biomegolf.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.block.GolfBlocks;
import pro.apdev.biomegolf.course.AuthoredCourseStore;
import pro.apdev.biomegolf.course.AuthoredCourseStore.DraftSnapshot;
import pro.apdev.biomegolf.course.AuthoredCourseStore.HoleSnapshot;
import pro.apdev.biomegolf.course.AuthoredHoleBounds;
import pro.apdev.biomegolf.course.CourseDefinition;
import pro.apdev.biomegolf.course.CourseLandscape;
import pro.apdev.biomegolf.entity.GolfBallEntity;
import pro.apdev.biomegolf.golf.Vec3;
import pro.apdev.biomegolf.hole.HoleBoundary;
import pro.apdev.biomegolf.hole.HoleDefinition;
import pro.apdev.biomegolf.server.ActiveHoleService;
import pro.apdev.biomegolf.server.AuthoredCourseService;
import pro.apdev.biomegolf.server.AuthoredCourseService.PendingCorner;
import pro.apdev.biomegolf.server.AuthoredCourseService.PendingLandscapeCorner;

/**
 * M8 S2 operator course-authoring commands (op-gated, player-position based).
 *
 * <pre>
 * /golf course create &lt;id&gt; [displayName...]   create a draft in the player's dimension
 * /golf course list                            list drafts and finalized courses
 * /golf course status &lt;id&gt;                   per-hole completeness or finalized summary
 * /golf course finalize &lt;id&gt;                 validate and promote a draft to playable
 * /golf course delete &lt;id&gt;                   remove a draft or finalized course
 * /golf course edit &lt;id&gt;                     make a draft the player's current draft
 * /golf course select &lt;id&gt;                   switch the active authored course
 * /golf course play [&lt;id&gt;] [&lt;hole&gt;]          start solo play
 * /golf course default set|status|clear        manage the persistent world default
 * /golf hole tee &lt;n&gt;                         set hole n tee at the player's position
 * /golf hole cup &lt;n&gt;                         set hole n cup and place the cup block
 * /golf hole par &lt;n&gt; &lt;par&gt;                   set hole n par
 * /golf hole bounds &lt;n&gt;                      two-corner bounds capture (Y = world height)
 * /golf course landscape bounds                two-corner whole-course perimeter capture
 * /golf course landscape clear [&lt;id&gt;]           remove the perimeter and unlock
 * /golf course landscape lock [&lt;id&gt;]            deny block mutation/TNT to everyone inside
 * /golf course landscape unlock [&lt;id&gt;]          restore the operator exemption inside
 * /golf course landscape status [&lt;id&gt;]          show perimeter bounds, dimension, and lock state
 * </pre>
 *
 * <p>All state is server-authoritative: mutations go through
 * {@link AuthoredCourseService}, which persists the store after every
 * successful change. Runtime course selection switches
 * {@link ActiveHoleService} without changing the persistent world default.</p>
 */
public final class GolfCourseCommands {

	private static boolean registered;

	private GolfCourseCommands() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		CommandRegistrationCallback.EVENT.register(GolfCourseCommands::onRegisterCommands);
	}

	private static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess, CommandSelection environment) {
		dispatcher.register(Commands.literal("golf")
			.then(Commands.literal("course")
				.then(Commands.literal("play")
					.executes(GolfCourseCommands::playDefaultCourse)
					.then(Commands.argument("courseId", StringArgumentType.word())
						.executes(GolfCourseCommands::playCourse)
						.then(Commands.argument("hole", IntegerArgumentType.integer(1))
							.executes(GolfCourseCommands::playCourseHole))))
				.then(Commands.literal("default")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("set")
						.then(Commands.argument("id", StringArgumentType.word())
							.executes(GolfCourseCommands::setDefaultCourse)))
					.then(Commands.literal("status").executes(GolfCourseCommands::defaultCourseStatus))
					.then(Commands.literal("clear").executes(GolfCourseCommands::clearDefaultCourse)))
				.then(Commands.literal("create")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::createCourse)
						.then(Commands.argument("displayName", StringArgumentType.greedyString())
							.executes(GolfCourseCommands::createCourseNamed))))
				.then(Commands.literal("clone")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("sourceId", StringArgumentType.word())
						.then(Commands.argument("newId", StringArgumentType.word())
							.executes(GolfCourseCommands::cloneCourse)
							.then(Commands.argument("displayName", StringArgumentType.greedyString())
								.executes(GolfCourseCommands::cloneCourseNamed)))))
				.then(Commands.literal("list")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(GolfCourseCommands::listCourses))
				.then(Commands.literal("status")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::courseStatus)))
				.then(Commands.literal("finalize")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::finalizeCourse)))
				.then(Commands.literal("delete")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::deleteCourse)))
				.then(Commands.literal("edit")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::editCourse)))
				.then(Commands.literal("select")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("id", StringArgumentType.word())
						.executes(GolfCourseCommands::selectCourse)))
				.then(Commands.literal("landscape")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("bounds").executes(GolfCourseCommands::landscapeBounds))
					.then(Commands.literal("clear")
						.executes(GolfCourseCommands::landscapeClear)
						.then(landscapeIdArgument(GolfCourseCommands::landscapeClear)))
					.then(Commands.literal("lock")
						.executes(GolfCourseCommands::landscapeLock)
						.then(landscapeIdArgument(GolfCourseCommands::landscapeLock)))
					.then(Commands.literal("unlock")
						.executes(GolfCourseCommands::landscapeUnlock)
						.then(landscapeIdArgument(GolfCourseCommands::landscapeUnlock)))
					.then(Commands.literal("status")
						.executes(GolfCourseCommands::landscapeStatus)
						.then(landscapeIdArgument(GolfCourseCommands::landscapeStatus)))))
			.then(Commands.literal("hole")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("tee")
					.then(Commands.argument("hole", IntegerArgumentType.integer(1))
						.executes(GolfCourseCommands::setTee)))
				.then(Commands.literal("cup")
					.then(Commands.argument("hole", IntegerArgumentType.integer(1))
						.executes(GolfCourseCommands::setCup)))
				.then(Commands.literal("par")
					.then(Commands.argument("hole", IntegerArgumentType.integer(1))
						.then(Commands.argument("par", IntegerArgumentType.integer(1))
							.executes(GolfCourseCommands::setPar))))
				.then(Commands.literal("bounds")
					.then(Commands.argument("hole", IntegerArgumentType.integer(1))
						.executes(GolfCourseCommands::captureBounds)))));
		MinecraftGolf.LOGGER.info(
			"Registered /golf course play/default and authoring commands (create, clone, list, status, finalize, delete, edit, select, landscape)"
				+ " and /golf hole metadata commands (tee, cup, par, bounds)");
	}

	// ------------------------------------------------------------------
	// /golf course ...
	// ------------------------------------------------------------------

	private static int playDefaultCourse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return sendResult(ctx, ActiveHoleService.instance().start(ctx.getSource().getPlayerOrException(), 1));
	}

	private static int playCourse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return playCourse(ctx, 1);
	}

	private static int playCourseHole(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return playCourse(ctx, IntegerArgumentType.getInteger(ctx, "hole"));
	}

	private static int playCourse(CommandContext<CommandSourceStack> ctx, int holeNumber)
			throws CommandSyntaxException {
		return sendResult(ctx, ActiveHoleService.instance().start(
			StringArgumentType.getString(ctx, "courseId"), holeNumber,
			ctx.getSource().getPlayerOrException()));
	}

	private static int setDefaultCourse(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseService service = AuthoredCourseService.instance();
		try {
			service.store().setDefaultCourse(StringArgumentType.getString(ctx, "id"));
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.save();
		String id = service.store().defaultCourseId().orElseThrow();
		ctx.getSource().sendSuccess(() -> Component.literal(
			"[golf] persistent default course is now '" + id + "'"), true);
		return 1;
	}

	private static int defaultCourseStatus(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseStore store = AuthoredCourseService.instance().store();
		String id = store.defaultCourseId().orElse(null);
		if (id == null) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] no persistent default course is set"), false);
			return 1;
		}
		CourseDefinition definition = store.finalizedCourse(id);
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] persistent default course is '"
			+ id + "' (" + definition.displayName() + ")"), false);
		return 1;
	}

	private static int clearDefaultCourse(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseService service = AuthoredCourseService.instance();
		String previous = service.store().defaultCourseId().orElse(null);
		service.store().clearDefaultCourse();
		service.save();
		ctx.getSource().sendSuccess(() -> Component.literal(previous == null
			? "[golf] no persistent default course was set"
			: "[golf] cleared persistent default course '" + previous + "'"), true);
		return 1;
	}

	private static int createCourse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return createCourse(ctx, StringArgumentType.getString(ctx, "id"));
	}

	private static int createCourseNamed(CommandContext<CommandSourceStack> ctx)
			throws CommandSyntaxException {
		return createCourse(ctx, StringArgumentType.getString(ctx, "displayName"));
	}

	private static int cloneCourse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return cloneCourse(ctx, null);
	}

	private static int cloneCourseNamed(CommandContext<CommandSourceStack> ctx)
			throws CommandSyntaxException {
		return cloneCourse(ctx, StringArgumentType.getString(ctx, "displayName"));
	}

	private static int cloneCourse(CommandContext<CommandSourceStack> ctx, String displayName)
			throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuthoredCourseService service = AuthoredCourseService.instance();
		String sourceId = StringArgumentType.getString(ctx, "sourceId");
		String newId = StringArgumentType.getString(ctx, "newId");
		String clonedId;
		try {
			clonedId = service.store().cloneCourse(sourceId, newId, displayName);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.setCurrentDraft(player.getUUID(), clonedId);
		service.save();
		AuthoredCourseStore.DraftSnapshot draft = service.store().draftSnapshot(clonedId);
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] cloned course '"
			+ AuthoredCourseStore.normalizeId(sourceId) + "' as draft '" + clonedId + "' (\""
			+ draft.displayName() + "\") — now your current draft"), true);
		return 1;
	}

	private static int createCourse(CommandContext<CommandSourceStack> ctx, String displayName)
			throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuthoredCourseService service = AuthoredCourseService.instance();
		String id = StringArgumentType.getString(ctx, "id");
		String dimension = player.level().dimension().identifier().toString();
		String normalizedId;
		try {
			normalizedId = service.store().createCourse(id, displayName, dimension);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.setCurrentDraft(player.getUUID(), normalizedId);
		service.save();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] created draft course '" + normalizedId
			+ "' (\"" + displayName + "\") in " + dimension
			+ " — now your current draft; set holes with /golf hole tee|cup|par|bounds <n>"), true);
		return 1;
	}

	private static int listCourses(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseStore store = AuthoredCourseService.instance().store();
		if (store.draftSnapshots().isEmpty() && store.finalizedCourses().isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal(
				"[golf] no authored courses; create one with /golf course create <id>"), false);
			return 1;
		}
		for (DraftSnapshot draft : store.draftSnapshots()) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] " + draft.id()
				+ " (\"" + draft.displayName() + "\") — DRAFT, " + draft.holes().size()
				+ " hole(s), " + draft.dimension()), false);
		}
		for (CourseDefinition course : store.finalizedCourses()) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] " + course.id()
				+ " (\"" + course.displayName() + "\") — finalized, " + course.holes().size()
				+ " hole(s), par " + course.totalPar() + ", " + course.dimension()), false);
		}
		return 1;
	}

	private static int courseStatus(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseStore store = AuthoredCourseService.instance().store();
		String id = StringArgumentType.getString(ctx, "id");
		if (store.isDraft(id)) {
			DraftSnapshot draft = store.draftSnapshot(id);
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] draft '" + draft.id()
				+ "' (\"" + draft.displayName() + "\") in " + draft.dimension()), false);
			if (draft.holes().isEmpty()) {
				ctx.getSource().sendSuccess(() -> Component.literal(
					"[golf] no holes yet; use /golf hole tee|cup|par|bounds <n>"), false);
			}
			for (HoleSnapshot hole : draft.holes()) {
				ctx.getSource().sendSuccess(() -> Component.literal("[golf] hole " + hole.number()
					+ ": tee " + present(hole.tee()) + " | cup " + present(hole.cup())
					+ " | par " + (hole.par() == null ? "missing" : hole.par())
					+ " | bounds " + (hole.boundary() == null ? "missing" : "set")), false);
			}
			return 1;
		}
		if (store.isFinalized(id)) {
			CourseDefinition course = store.finalizedCourse(id);
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] course '" + course.id()
				+ "' (\"" + course.displayName() + "\") — finalized, " + course.holes().size()
				+ " hole(s), par " + course.totalPar() + ", " + course.dimension()), false);
			for (HoleDefinition hole : course.holes()) {
				ctx.getSource().sendSuccess(() -> Component.literal("[golf] hole " + hole.number()
					+ " — par " + hole.par() + ", tee " + hole.tee() + ", cup " + hole.cup()), false);
			}
			return 1;
		}
		return fail(ctx, "no course with id '" + AuthoredCourseStore.normalizeId(id) + "'");
	}

	private static int finalizeCourse(CommandContext<CommandSourceStack> ctx) {
		if (activePlayBlocksMutation(ctx)) return 0;
		AuthoredCourseService service = AuthoredCourseService.instance();
		String id = StringArgumentType.getString(ctx, "id");
		CourseDefinition definition;
		try {
			definition = service.store().finalize(id);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.save();
		service.refreshProtection();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] finalized course '" + definition.id()
			+ "' (\"" + definition.displayName() + "\") — " + definition.holes().size()
			+ " hole(s), par " + definition.totalPar()
			+ "; select it with /golf course select " + definition.id()), true);
		return 1;
	}

	private static int deleteCourse(CommandContext<CommandSourceStack> ctx) {
		if (activePlayBlocksMutation(ctx)) return 0;
		AuthoredCourseService service = AuthoredCourseService.instance();
		String id;
		try {
			id = AuthoredCourseStore.normalizeId(StringArgumentType.getString(ctx, "id"));
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		try {
			service.store().removeCourse(id);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.clearCurrentDraftFor(id);
		ActiveHoleService.instance().clearConfiguredCourseIf(id);
		service.save();
		service.refreshProtection();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] deleted course '" + id + "'"), true);
		return 1;
	}

	private static int editCourse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuthoredCourseService service = AuthoredCourseService.instance();
		String id = StringArgumentType.getString(ctx, "id");
		if (service.store().isFinalized(id)) {
			return fail(ctx, "course '" + AuthoredCourseStore.normalizeId(id)
				+ "' is finalized and immutable; delete it with /golf course delete and re-create it to make changes");
		}
		if (!service.store().isDraft(id)) {
			return fail(ctx, "no draft course with id '" + AuthoredCourseStore.normalizeId(id) + "'");
		}
		String normalizedId = AuthoredCourseStore.normalizeId(id);
		service.setCurrentDraft(player.getUUID(), normalizedId);
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] '" + normalizedId
			+ "' is now your current draft; set holes with /golf hole tee|cup|par|bounds <n>"), false);
		return 1;
	}

	private static int selectCourse(CommandContext<CommandSourceStack> ctx) {
		ActiveHoleService holes = ActiveHoleService.instance();
		if (holes.hasActivePlay()) {
			return fail(ctx, "cannot switch courses while a round or hole attempt is active;"
				+ " leave/complete current play first");
		}
		String id = StringArgumentType.getString(ctx, "id");
		CourseDefinition definition;
		try {
			definition = AuthoredCourseService.instance().store().finalizedCourse(id);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		holes.initializeCourse(definition);
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] active course is now '" + definition.id()
			+ "' (" + definition.displayName() + ") — " + definition.holes().size()
			+ " hole(s), par " + definition.totalPar()
			+ "; selection is not persisted — select it again after a restart"), true);
		return 1;
	}

	private static int sendResult(CommandContext<CommandSourceStack> ctx,
			ActiveHoleService.StartResult result) {
		if (!result.success()) {
			ctx.getSource().sendFailure(Component.literal(result.message()));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(result.message()), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// /golf hole tee|cup|par|bounds <n>
	// ------------------------------------------------------------------

	private static int setTee(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String draftId = currentDraftOrFail(ctx, player);
		if (draftId == null) {
			return 0;
		}
		int number = IntegerArgumentType.getInteger(ctx, "hole");
		Vec3 tee = standingPoint(player);
		try {
			AuthoredCourseService.instance().store().setHoleTee(draftId, number, tee);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		AuthoredCourseService.instance().save();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] draft '" + draftId + "' hole "
			+ number + " tee set to " + tee), false);
		return 1;
	}

	private static int setCup(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String draftId = currentDraftOrFail(ctx, player);
		if (draftId == null) {
			return 0;
		}
		int number = IntegerArgumentType.getInteger(ctx, "hole");
		Vec3 cup = standingPoint(player);
		AuthoredCourseStore store = AuthoredCourseService.instance().store();
		Vec3 previousCup = null;
		for (HoleSnapshot hole : store.draftSnapshot(draftId).holes()) {
			if (hole.number() == number) {
				previousCup = hole.cup();
				break;
			}
		}
		try {
			store.setHoleCup(draftId, number, cup);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		AuthoredCourseService.instance().save();

		// Mirror ActiveHoleService.createAttempt: the cup block sits at
		// containing(cup.x, cup.y - BALL_RADIUS, cup.z), the block at the player's feet.
		ServerLevel level = player.level();
		BlockPos cupBlockPos = BlockPos.containing(
			cup.x(), cup.y() - GolfBallEntity.BALL_RADIUS, cup.z());
		if (previousCup != null) {
			clearMarker(level, BlockPos.containing(previousCup.x(),
				previousCup.y() - GolfBallEntity.BALL_RADIUS, previousCup.z()));
		}
		boolean placed = (level.getBlockState(cupBlockPos).canBeReplaced()
			|| level.getBlockState(cupBlockPos).getBlock() == GolfBlocks.GOLF_CUP)
			&& level.setBlockAndUpdate(cupBlockPos, GolfBlocks.GOLF_CUP.defaultBlockState());
		if (placed) {
			placeFlag(level, cupBlockPos);
		}
		String placement = placed
			? "; placed the cup block at " + cupBlockPos.toShortString()
			: "; could not place the cup block at " + cupBlockPos.toShortString()
				+ " (not replaceable) — place minecraft_golf:golf_cup there manually";
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] draft '" + draftId + "' hole "
			+ number + " cup set to " + cup + placement), false);
		return 1;
	}

	private static int setPar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String draftId = currentDraftOrFail(ctx, player);
		if (draftId == null) {
			return 0;
		}
		int number = IntegerArgumentType.getInteger(ctx, "hole");
		int par = IntegerArgumentType.getInteger(ctx, "par");
		try {
			AuthoredCourseService.instance().store().setHolePar(draftId, number, par);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		AuthoredCourseService.instance().save();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] draft '" + draftId + "' hole "
			+ number + " par set to " + par), false);
		return 1;
	}

	private static int captureBounds(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuthoredCourseService service = AuthoredCourseService.instance();
		String draftId = currentDraftOrFail(ctx, player);
		if (draftId == null) {
			return 0;
		}
		int number = IntegerArgumentType.getInteger(ctx, "hole");
		BlockPos here = player.blockPosition();
		Vec3 corner = new Vec3(here.getX(), here.getY(), here.getZ());
		PendingCorner pending = service.pendingCorner(player.getUUID());
		if (pending == null || !pending.courseId().equals(draftId) || pending.holeNumber() != number) {
			service.setPendingCorner(player.getUUID(), new PendingCorner(draftId, number, corner));
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] bounds corner A recorded at "
				+ here.toShortString() + " for draft '" + draftId + "' hole " + number
				+ "; stand at the opposite corner and run /golf hole bounds " + number + " again"), false);
			return 1;
		}
		ServerLevel level = player.level();
		HoleBoundary boundary = AuthoredHoleBounds.fromCorners(
			pending.corner(), corner, level.getMinY(), level.getMaxY());
		try {
			service.store().setHoleBounds(draftId, number, boundary);
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.clearPendingCorner(player.getUUID());
		service.save();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] draft '" + draftId + "' hole "
			+ number + " bounds set to X[" + boundary.min().x() + ".." + boundary.max().x()
			+ "] Z[" + boundary.min().z() + ".." + boundary.max().z() + "]; Y expanded to world build height ["
			+ boundary.min().y() + ".." + boundary.max().y() + "] so cliffs and elevated shots stay in-bounds"
			+ "; run again to restart capture"), false);
		return 1;
	}

	// ------------------------------------------------------------------
	// /golf course landscape bounds|clear|lock|unlock|status
	// ------------------------------------------------------------------

	/**
	 * M8.10 S4: two-corner capture of the whole-course landscape perimeter. The
	 * first invocation records corner A; the second builds the XZ box (Y expanded
	 * to world build height) and stores it, preserving any existing lock.
	 */
	private static int landscapeBounds(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String courseId = resolveLandscapeTarget(ctx);
		if (courseId == null) return 0;
		AuthoredCourseService service = AuthoredCourseService.instance();
		String courseDimension = courseDimension(service.store(), courseId);
		String playerDimension = player.level().dimension().identifier().toString();
		if (!playerDimension.equals(courseDimension)) {
			return fail(ctx, "course '" + courseId + "' is in " + courseDimension
				+ "; stand in that dimension to capture its landscape perimeter");
		}
		BlockPos here = player.blockPosition();
		Vec3 corner = new Vec3(here.getX(), here.getY(), here.getZ());
		PendingLandscapeCorner pending = service.pendingLandscapeCorner(player.getUUID());
		if (pending == null || !pending.courseId().equals(courseId)) {
			service.setPendingLandscapeCorner(player.getUUID(), new PendingLandscapeCorner(courseId, corner));
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] landscape corner A recorded at "
				+ here.toShortString() + " for course '" + courseId
				+ "'; stand at the opposite corner and run /golf course landscape bounds again"), false);
			return 1;
		}
		ServerLevel level = player.level();
		HoleBoundary bounds = AuthoredHoleBounds.fromCorners(
			pending.corner(), corner, level.getMinY(), level.getMaxY());
		boolean locked = service.store().landscape(courseId).map(CourseLandscape::locked).orElse(false);
		try {
			service.store().setLandscape(new CourseLandscape(courseId, courseDimension, bounds, locked));
		} catch (IllegalArgumentException exception) {
			return fail(ctx, exception.getMessage());
		}
		service.clearPendingLandscapeCorner(player.getUUID());
		service.save();
		service.refreshProtection();
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] landscape perimeter for '" + courseId
			+ "' set to X[" + bounds.min().x() + ".." + bounds.max().x()
			+ "] Z[" + bounds.min().z() + ".." + bounds.max().z()
			+ "]; Y expanded to world build height [" + bounds.min().y() + ".." + bounds.max().y()
			+ "]; " + (locked ? "locked" : "unlocked")
			+ "; run again to restart capture"), true);
		return 1;
	}

	/** M8.10 S4: removes the perimeter and unlocks the course. */
	private static int landscapeClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		String courseId = resolveLandscapeTarget(ctx);
		if (courseId == null) return 0;
		AuthoredCourseService service = AuthoredCourseService.instance();
		boolean existed = service.store().clearLandscape(courseId);
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player != null) service.clearPendingLandscapeCorner(player.getUUID());
		service.save();
		service.refreshProtection();
		ctx.getSource().sendSuccess(() -> Component.literal(existed
			? "[golf] cleared the landscape perimeter and lock for course '" + courseId + "'"
			: "[golf] course '" + courseId + "' had no landscape perimeter; nothing to clear"), true);
		return 1;
	}

	private static int landscapeLock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setLandscapeLock(ctx, true);
	}

	private static int landscapeUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setLandscapeLock(ctx, false);
	}

	private static int setLandscapeLock(CommandContext<CommandSourceStack> ctx, boolean locked)
			throws CommandSyntaxException {
		if (activePlayBlocksMutation(ctx)) return 0;
		String courseId = resolveLandscapeTarget(ctx);
		if (courseId == null) return 0;
		AuthoredCourseService service = AuthoredCourseService.instance();
		CourseLandscape landscape = service.store().landscape(courseId).orElse(null);
		if (landscape == null) {
			return fail(ctx, "course '" + courseId
				+ "' has no landscape perimeter yet; set one with /golf course landscape bounds first");
		}
		if (landscape.locked() == locked) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] landscape perimeter for '" + courseId
				+ "' is already " + (locked ? "locked" : "unlocked")), false);
			return 1;
		}
		service.store().setLandscape(landscape.withLocked(locked));
		service.save();
		service.refreshProtection();
		ctx.getSource().sendSuccess(() -> Component.literal(locked
			? "[golf] landscape perimeter for '" + courseId
				+ "' is now locked: no player may break, place, or detonate TNT inside it"
			: "[golf] landscape perimeter for '" + courseId
				+ "' is now unlocked: operators may repair inside it, non-operators still cannot"), true);
		return 1;
	}

	private static int landscapeStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String courseId = resolveLandscapeTarget(ctx);
		if (courseId == null) return 0;
		AuthoredCourseStore store = AuthoredCourseService.instance().store();
		String kind = store.isDraft(courseId) ? "draft" : "finalized";
		String dimension = courseDimension(store, courseId);
		CourseLandscape landscape = store.landscape(courseId).orElse(null);
		if (landscape == null) {
			ctx.getSource().sendSuccess(() -> Component.literal("[golf] landscape for " + kind + " course '"
				+ courseId + "' (" + dimension + "): no perimeter (unlocked)"), false);
			return 1;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("[golf] landscape for " + kind + " course '"
			+ courseId + "' (" + landscape.dimension() + "): "
			+ (landscape.locked() ? "LOCKED" : "unlocked") + ", X["
			+ landscape.bounds().min().x() + ".." + landscape.bounds().max().x() + "] Z["
			+ landscape.bounds().min().z() + ".." + landscape.bounds().max().z() + "] Y["
			+ landscape.bounds().min().y() + ".." + landscape.bounds().max().y() + "]"), false);
		return 1;
	}

	/**
	 * Resolves the course a landscape command targets. An explicit {@code id}
	 * argument wins (so console/RCON can always run {@code unlock}); otherwise the
	 * player's current draft, else the active selected course, else the
	 * "select a course first" failure. Stale draft selections are cleared.
	 */
	private static String resolveLandscapeTarget(CommandContext<CommandSourceStack> ctx) {
		AuthoredCourseService service = AuthoredCourseService.instance();
		String explicitId = optionalIdArgument(ctx);
		if (explicitId != null) {
			String id;
			try {
				id = AuthoredCourseStore.normalizeId(explicitId);
			} catch (IllegalArgumentException exception) {
				fail(ctx, exception.getMessage());
				return null;
			}
			if (!service.store().isDraft(id) && !service.store().isFinalized(id)) {
				fail(ctx, "no course with id '" + id + "'");
				return null;
			}
			return id;
		}
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player != null) {
			String draftId = service.currentDraft(player.getUUID());
			if (draftId != null) {
				if (service.store().isDraft(draftId)) {
					return draftId;
				}
				service.clearCurrentDraft(player.getUUID());
			}
		}
		CourseDefinition active = ActiveHoleService.instance().configuredCourseOrNull();
		if (active != null && service.store().isFinalized(active.id())) {
			return active.id();
		}
		ctx.getSource().sendFailure(Component.literal("[golf] no course selected; use /golf course select <id>"
			+ " or /golf course edit <id>"));
		return null;
	}

	/**
	 * Returns the optional {@code id} argument, or {@code null} when it is absent.
	 * Brigadier throws {@link IllegalArgumentException} for an unread argument.
	 */
	private static String optionalIdArgument(CommandContext<CommandSourceStack> ctx) {
		try {
			return StringArgumentType.getString(ctx, "id");
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static String courseDimension(AuthoredCourseStore store, String courseId) {
		return store.isDraft(courseId)
			? store.draftSnapshot(courseId).dimension()
			: store.finalizedCourse(courseId).dimension();
	}

	/** Builds the optional {@code <id>} child used by the landscape subcommands. */
	private static RequiredArgumentBuilder<CommandSourceStack, String> landscapeIdArgument(
			Command<CommandSourceStack> executor) {
		return Commands.argument("id", StringArgumentType.word()).executes(executor);
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------
	private static boolean activePlayBlocksMutation(CommandContext<CommandSourceStack> ctx) {
		if (!ActiveHoleService.instance().hasActivePlay()) return false;
		fail(ctx, "cannot modify authored courses while golf play is active; leave or complete current play first");
		return true;
	}

	static void placeFlag(ServerLevel level, BlockPos cupBlockPos) {
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

	static void clearMarker(ServerLevel level, BlockPos cupBlockPos) {
		for (BlockPos markerPos : new BlockPos[] {
			cupBlockPos, cupBlockPos.above(), cupBlockPos.above(2)
		}) {
			if (level.getBlockState(markerPos).getBlock() == GolfBlocks.GOLF_CUP
				|| level.getBlockState(markerPos).getBlock() == GolfBlocks.GOLF_FLAG
				|| level.getBlockState(markerPos).getBlock() == GolfBlocks.GOLF_FLAG_TOP) {
				level.setBlockAndUpdate(markerPos, Blocks.AIR.defaultBlockState());
			}
		}
	}

	/**
	 * Ball-rest point for the block the player stands on: centered in X/Z, at
	 * surface height + ball radius in Y (matching M5 tee/cup conventions).
	 */
	private static Vec3 standingPoint(ServerPlayer player) {
		BlockPos feet = player.blockPosition();
		return new Vec3(feet.getX() + 0.5, feet.getY() + GolfBallEntity.BALL_RADIUS, feet.getZ() + 0.5);
	}

	private static String currentDraftOrFail(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		String draftId = AuthoredCourseService.instance().currentDraft(player.getUUID());
		if (draftId == null) {
			ctx.getSource().sendFailure(Component.literal("[golf] no current draft; create one with"
				+ " /golf course create <id> or select one with /golf course edit <id>"));
			return null;
		}
		if (!AuthoredCourseService.instance().store().isDraft(draftId)) {
			AuthoredCourseService.instance().clearCurrentDraft(player.getUUID());
			ctx.getSource().sendFailure(Component.literal("[golf] draft '" + draftId
				+ "' no longer exists; create or edit a draft with /golf course create|edit <id>"));
			return null;
		}
		return draftId;
	}

	private static String present(Vec3 value) {
		return value == null ? "missing" : value.toString();
	}

	private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
		ctx.getSource().sendFailure(Component.literal("[golf] " + message));
		return 0;
	}
}
