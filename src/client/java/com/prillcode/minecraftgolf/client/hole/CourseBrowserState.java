package com.prillcode.minecraftgolf.client.hole;

import com.prillcode.minecraftgolf.net.CourseListPayload;

/** Client cache for the latest server-provided finalized-course list. */
public final class CourseBrowserState {
	private static CourseListPayload current;
	private CourseBrowserState() {}
	public static void update(CourseListPayload payload) { current = payload; }
	public static CourseListPayload get() { return current; }
	public static void clear() { current = null; }
}
