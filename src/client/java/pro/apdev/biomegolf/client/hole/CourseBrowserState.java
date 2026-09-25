package pro.apdev.biomegolf.client.hole;

import pro.apdev.biomegolf.net.CourseListPayload;

/** Client cache for the latest server-provided finalized-course list. */
public final class CourseBrowserState {
	private static CourseListPayload current;
	private CourseBrowserState() {}
	public static void update(CourseListPayload payload) { current = payload; }
	public static CourseListPayload get() { return current; }
	public static void clear() { current = null; }
}
