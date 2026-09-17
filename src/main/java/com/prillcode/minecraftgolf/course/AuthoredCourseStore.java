package com.prillcode.minecraftgolf.course;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import com.prillcode.minecraftgolf.golf.Vec3;
import com.prillcode.minecraftgolf.hole.HoleBoundary;
import com.prillcode.minecraftgolf.hole.HoleDefinition;

/**
 * Minecraft-free in-memory store of authored course drafts and finalized courses.
 *
 * <p>Courses are keyed by a normalized id: the trimmed, lowercased input, which
 * must be non-blank, contain no whitespace, and use only
 * {@code [a-z0-9_.:/-]} characters after normalization. Dimensions must use the
 * Minecraft {@code namespace:path} form (lowercase letters, digits, {@code _},
 * {@code .}, {@code -}, {@code /}).</p>
 *
 * <p>All mutating operations fail with {@link IllegalArgumentException} for
 * invalid input and {@link IllegalStateException} for invalid workflow state,
 * with precise messages, mirroring {@link HoleDefinition} and
 * {@link CourseDefinition}.</p>
 */
public final class AuthoredCourseStore {

	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_.:/-]+");
	private static final Pattern DIMENSION_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private final Map<String, CourseDraft> drafts = new LinkedHashMap<>();
	private final Map<String, CourseDefinition> finalized = new LinkedHashMap<>();
	private String defaultCourseId;

	/** Creates a new empty draft. Returns the normalized course id. */
	public String createCourse(String id, String displayName, String dimension) {
		String normalizedId = normalizeId(id);
		requireNonBlank(displayName, "displayName");
		String normalizedDimension = normalizeDimension(dimension);
		if (drafts.containsKey(normalizedId) || finalized.containsKey(normalizedId)) {
			throw new IllegalStateException("a course with id '" + normalizedId + "' already exists");
		}
		drafts.put(normalizedId, new CourseDraft(normalizedId, displayName, normalizedDimension));
		return normalizedId;
	}

	/** Renames a draft or finalized course's display name. */
	public void renameCourse(String courseId, String displayName) {
		requireNonBlank(displayName, "displayName");
		String id = lookupId(courseId);
		CourseDraft draft = drafts.get(id);
		if (draft != null) {
			draft.displayName = displayName;
			return;
		}
		CourseDefinition course = finalized.get(id);
		finalized.put(id, new CourseDefinition(course.id(), displayName, course.dimension(),
			course.generatedLayout(), course.holes()));
	}

	/** Copies a draft or finalized course into a new, independent draft. */
	public String cloneCourse(String sourceCourseId, String newCourseId, String displayName) {
		String sourceId = normalizeId(sourceCourseId);
		String destinationId = normalizeId(newCourseId);
		String sourceDisplayName;
		String dimension;
		List<HoleSnapshot> holes;
		CourseDraft sourceDraft = drafts.get(sourceId);
		if (sourceDraft != null) {
			sourceDisplayName = sourceDraft.displayName;
			dimension = sourceDraft.dimension;
			holes = draftSnapshot(sourceId).holes();
		} else {
			CourseDefinition source = finalized.get(sourceId);
			if (source == null) {
				throw new IllegalArgumentException("no course with id '" + sourceId + "'");
			}
			sourceDisplayName = source.displayName();
			dimension = source.dimension();
			holes = source.holes().stream()
				.map(hole -> new HoleSnapshot(hole.number(), hole.tee(), hole.cup(), hole.par(),
					hole.boundary(), hole.transition()))
				.toList();
		}
		String destinationDisplayName = displayName == null ? sourceDisplayName + " Copy" : displayName;
		createCourse(destinationId, destinationDisplayName, dimension);
		for (HoleSnapshot hole : holes) {
			if (hole.tee() != null) {
				setHoleTee(destinationId, hole.number(), hole.tee());
			}
			if (hole.cup() != null) {
				setHoleCup(destinationId, hole.number(), hole.cup());
			}
			if (hole.par() != null) {
				setHolePar(destinationId, hole.number(), hole.par());
			}
			if (hole.boundary() != null) {
				setHoleBounds(destinationId, hole.number(), hole.boundary());
			}
			if (hole.transition() != null) {
				setHoleTransition(destinationId, hole.number(), hole.transition());
			}
		}
		return destinationId;
	}

	public void setHoleTee(String courseId, int number, Vec3 tee) {
		holeDraft(courseId, number).tee = Objects.requireNonNull(tee, "tee");
	}

	public void setHoleCup(String courseId, int number, Vec3 cup) {
		holeDraft(courseId, number).cup = Objects.requireNonNull(cup, "cup");
	}

	public void setHolePar(String courseId, int number, int par) {
		if (par <= 0) {
			throw new IllegalArgumentException("par must be positive");
		}
		holeDraft(courseId, number).par = par;
	}

	public void setHoleBounds(String courseId, int number, HoleBoundary boundary) {
		holeDraft(courseId, number).boundary = Objects.requireNonNull(boundary, "boundary");
	}

	public void setHoleTransition(String courseId, int number, HoleTransition transition) {
		holeDraft(courseId, number).transition = Objects.requireNonNull(transition, "transition");
	}

	/** Removes a hole draft entirely; later holes keep their numbers. */
	public void removeHole(String courseId, int number) {
		draft(courseId).holes.remove(requireHoleNumber(number));
	}

	/** Removes a draft or finalized course entirely. */
	public void removeCourse(String courseId) {
		String id = lookupId(courseId);
		if (id.equals(defaultCourseId)) {
			throw new IllegalStateException("course '" + id
				+ "' is the server default; clear or change the default before deleting it");
		}
		if (drafts.remove(id) == null && finalized.remove(id) == null) {
			throw new IllegalArgumentException("no course with id '" + id + "'");
		}
	}

	/**
	 * Validates the draft and promotes it to an immutable, playable
	 * {@link CourseDefinition}; the draft is consumed.
	 */
	public CourseDefinition finalize(String courseId) {
		CourseDraft draft = draft(courseId);
		if (draft.holes.isEmpty()) {
			throw new IllegalStateException("course '" + draft.id + "' must contain at least one hole");
		}
		int count = draft.holes.size();
		List<HoleDefinition> holes = new ArrayList<>(count);
		for (int number = 1; number <= count; number++) {
			HoleDraft hole = draft.holes.get(number);
			if (hole == null) {
				throw new IllegalStateException("course '" + draft.id + "' is missing hole " + number
					+ "; hole numbers must be 1.." + count + " without gaps");
			}
			holes.add(hole.toDefinition(draft.id, number, draft.dimension));
		}
		CourseDefinition definition = new CourseDefinition(draft.id, draft.displayName, draft.dimension,
			new GeneratedLayoutIdentity("authored:" + draft.id, 1), holes);
		drafts.remove(draft.id);
		finalized.put(draft.id, definition);
		return definition;
	}

	public boolean isDraft(String courseId) {
		return drafts.containsKey(normalizeId(courseId));
	}

	public boolean isFinalized(String courseId) {
		return finalized.containsKey(normalizeId(courseId));
	}

	/** Finalized courses are playable/inspectable; drafts are never exposed this way. */
	public CourseDefinition finalizedCourse(String courseId) {
		String id = normalizeId(courseId);
		CourseDefinition definition = finalized.get(id);
		if (definition == null) {
			throw new IllegalArgumentException("no finalized course with id '" + id + "'");
		}
		return definition;
	}

	/** Immutable snapshot of one draft for inspection/persistence. */
	public DraftSnapshot draftSnapshot(String courseId) {
		CourseDraft draft = draft(courseId);
		List<HoleSnapshot> holes = new ArrayList<>();
		for (Map.Entry<Integer, HoleDraft> entry : draft.holes.entrySet()) {
			HoleDraft hole = entry.getValue();
			holes.add(new HoleSnapshot(entry.getKey(), hole.tee, hole.cup, hole.par, hole.boundary,
				hole.transition));
		}
		return new DraftSnapshot(draft.id, draft.displayName, draft.dimension, holes);
	}

	public List<DraftSnapshot> draftSnapshots() {
		List<DraftSnapshot> result = new ArrayList<>();
		for (String id : drafts.keySet()) {
			result.add(draftSnapshot(id));
		}
		return List.copyOf(result);
	}

	public List<CourseDefinition> finalizedCourses() {
		return List.copyOf(finalized.values());
	}

	/** Selects a finalized course as this world's persistent default. */
	public void setDefaultCourse(String courseId) {
		String id = normalizeId(courseId);
		if (!finalized.containsKey(id)) {
			throw new IllegalArgumentException("no finalized course with id '" + id + "'");
		}
		defaultCourseId = id;
	}

	public Optional<String> defaultCourseId() {
		return Optional.ofNullable(defaultCourseId);
	}

	public void clearDefaultCourse() {
		defaultCourseId = null;
	}

	/** Normalizes and validates a course id per the documented rule. */
	public static String normalizeId(String id) {
		Objects.requireNonNull(id, "id");
		String normalized = id.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("course id must not be blank");
		}
		if (normalized.length() > CourseDefinition.MAX_ID_LENGTH) {
			throw new IllegalArgumentException("course id must be at most "
				+ CourseDefinition.MAX_ID_LENGTH + " characters");
		}
		if (!ID_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("course id '" + id + "' must contain only lowercase"
				+ " letters, digits, '_', '.', ':', '/', '-' and no whitespace");
		}
		return normalized;
	}

	private static String normalizeDimension(String dimension) {
		requireNonBlank(dimension, "dimension");
		String normalized = dimension.trim().toLowerCase(Locale.ROOT);
		if (!DIMENSION_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("dimension '" + dimension
				+ "' must use the 'namespace:path' form");
		}
		return normalized;
	}

	private String lookupId(String courseId) {
		String id = normalizeId(courseId);
		if (!drafts.containsKey(id) && !finalized.containsKey(id)) {
			throw new IllegalArgumentException("no course with id '" + id + "'");
		}
		return id;
	}

	private CourseDraft draft(String courseId) {
		String id = normalizeId(courseId);
		CourseDraft draft = drafts.get(id);
		if (draft == null) {
			throw new IllegalArgumentException("no draft course with id '" + id + "'"
				+ (finalized.containsKey(id) ? "; the course is already finalized" : ""));
		}
		return draft;
	}

	private HoleDraft holeDraft(String courseId, int number) {
		return draft(courseId).holes.computeIfAbsent(requireHoleNumber(number), key -> new HoleDraft());
	}

	private static int requireHoleNumber(int number) {
		if (number <= 0) {
			throw new IllegalArgumentException("hole number must be positive");
		}
		return number;
	}

	private static void requireNonBlank(String value, String name) {
		Objects.requireNonNull(value, name);
		if (value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		if (name.equals("displayName") && value.length() > CourseDefinition.MAX_DISPLAY_NAME_LENGTH) {
			throw new IllegalArgumentException("displayName must be at most "
				+ CourseDefinition.MAX_DISPLAY_NAME_LENGTH + " characters");
		}
	}

	private static final class CourseDraft {
		private final String id;
		private String displayName;
		private final String dimension;
		private final Map<Integer, HoleDraft> holes = new LinkedHashMap<>();

		private CourseDraft(String id, String displayName, String dimension) {
			this.id = id;
			this.displayName = displayName;
			this.dimension = dimension;
		}
	}

	/** Mutable per-hole authoring state; may be incomplete while drafting. */
	private static final class HoleDraft {
		private Vec3 tee;
		private Vec3 cup;
		private Integer par;
		private HoleBoundary boundary;
		private HoleTransition transition;

		private HoleDefinition toDefinition(String courseId, int number, String dimension) {
			List<String> missing = new ArrayList<>();
			if (tee == null) {
				missing.add("tee");
			}
			if (cup == null) {
				missing.add("cup");
			}
			if (par == null) {
				missing.add("par");
			}
			if (!missing.isEmpty()) {
				throw new IllegalStateException("course '" + courseId + "' hole " + number
					+ " is incomplete; missing " + String.join(", ", missing));
			}
			String holeId = holeId(courseId, number);
			return new HoleDefinition(holeId, number, dimension, tee, cup, par,
				boundary != null ? boundary : HoleBoundary.unbounded(),
				new GeneratedLayoutIdentity("authored:" + holeId, 1),
				transition != null ? transition : HoleTransition.at(tee));
		}
	}

	/** Stable hole id derived from the normalized course id and hole number. */
	public static String holeId(String courseId, int number) {
		return normalizeId(courseId) + ":hole_" + requireHoleNumber(number);
	}

	/** Immutable view of one draft course. */
	public record DraftSnapshot(
		String id, String displayName, String dimension, List<HoleSnapshot> holes
	) {
		public DraftSnapshot {
			holes = List.copyOf(holes);
		}
	}

	/** Immutable view of one possibly incomplete hole draft. */
	public record HoleSnapshot(
		int number, Vec3 tee, Vec3 cup, Integer par, HoleBoundary boundary, HoleTransition transition
	) {
	}
}
