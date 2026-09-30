package pro.apdev.biomegolf.command;

import java.util.List;

/** Canonical concise player and operator help text, also consumed by coverage tests. */
public final class GolfHelpPages {

	public static final List<String> PLAYER = List.of(
		"[golf] Player help — /golf help admin shows operator commands",
		"Courses: /golf browse | /golf round list | /golf round join [id]",
		"Rounds: /golf round create [courseId] | start | status | leave | restart",
		"Solo: /golf course play [courseId] [hole] | /golf hole start [hole] | restart | status",
		"Play: /golf swing [power] [accuracy] [shotType] | /golf pickup | /golf tapin | /golf nexthole",
		"Controls: right-click with a club; C shot type, Y distance units, H toggle both HUDs",
		"Modes: /golf mode status | golf | world (switch your play style)",
		"HUD fallback: /golf hud toggles both HUDs; /golf clubs equip restores clubs",
		"Practice: /golf practice ball | clear | tee | target list",
		"Visitors: /golf spectator [leave] | /golf visitor status");

	public static final List<String> ADMIN = List.of(
		"[golf] OP/GAMEMASTER ONLY — these commands are visible to everyone but require operator permission",
		"Course: /golf course create|clone|edit|select|finalize|delete|list|status",
		"Course defaults/landscape: /golf course default set|status|clear; /golf course landscape bounds|status|lock|unlock|clear",
		"Hole authoring: /golf hole tee|cup|par|bounds <number> (current draft)",
		"Practice setup: /golf practice tee set; /golf practice target set|clear <1-8>",
		"Modes/builder: /golf mode build <courseId>; /golf builder restock | palette reload",
		"Visitor setup: /golf visitor address|link <value>; /golf visitor spawn set|clear",
		"Diagnostics/dev: /golf spawn [x y z] | /golf launch <forward> <up> [id] | /golf inspect [id] | /golf dev preparehole|preparecourse|testhole",
		"Full syntax and usage: docs/COMMANDS.md");

	private GolfHelpPages() {
	}
}
