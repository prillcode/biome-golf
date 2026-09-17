package com.prillcode.minecraftgolf.net;

import java.util.List;
import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.course.CourseDefinition;
import com.prillcode.minecraftgolf.course.CourseScorecard;

/** Server-authoritative final scorecard with explicit authored hole numbers. */
public record RoundScorecardPayload(String courseId, List<HoleColumn> holes, List<PlayerRow> players)
		implements CustomPacketPayload {
	public static final Type<RoundScorecardPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_scorecard_v2"));
	public static final StreamCodec<FriendlyByteBuf, RoundScorecardPayload> STREAM_CODEC =
		StreamCodec.of(RoundScorecardPayload::encode, RoundScorecardPayload::decode);

	public record HoleColumn(int number, int par) {
		public HoleColumn {
			if (number < 1 || par < 1) throw new IllegalArgumentException("invalid scorecard hole");
		}
	}

	public record PlayerRow(String name, List<Integer> strokes) {
		public PlayerRow {
			Objects.requireNonNull(name, "name");
			strokes = List.copyOf(Objects.requireNonNull(strokes, "strokes"));
			if (name.isBlank() || strokes.stream().anyMatch(value -> value < -1)) {
				throw new IllegalArgumentException("invalid scorecard player row");
			}
		}
	}

	public RoundScorecardPayload {
		Objects.requireNonNull(courseId, "courseId");
		holes = List.copyOf(Objects.requireNonNull(holes, "holes"));
		players = List.copyOf(Objects.requireNonNull(players, "players"));
		int holeCount = holes.size();
		if (courseId.isBlank() || holes.isEmpty() || players.isEmpty() || players.size() > 4) {
			throw new IllegalArgumentException("scorecard must contain holes and 1-4 players");
		}
		if (holes.stream().map(HoleColumn::number).distinct().count() != holes.size()
				|| players.stream().anyMatch(player -> player.strokes().size() != holeCount)) {
			throw new IllegalArgumentException("scorecard rows must match unique authored holes");
		}
	}

	public static List<HoleColumn> holes(CourseDefinition course) {
		return course.holes().stream().map(hole -> new HoleColumn(hole.number(), hole.par())).toList();
	}

	public static PlayerRow player(String name, CourseScorecard scorecard, List<HoleColumn> holes) {
		return new PlayerRow(name, holes.stream().map(hole -> scorecard.holes().stream()
			.filter(score -> score.holeNumber() == hole.number()).map(score -> score.strokes())
			.findFirst().orElse(-1)).toList());
	}

	private static void encode(FriendlyByteBuf buf, RoundScorecardPayload payload) {
		buf.writeUtf(payload.courseId(), 128);
		buf.writeVarInt(payload.holes().size());
		for (HoleColumn hole : payload.holes()) {
			buf.writeVarInt(hole.number());
			buf.writeVarInt(hole.par());
		}
		buf.writeVarInt(payload.players().size());
		for (PlayerRow player : payload.players()) {
			buf.writeUtf(player.name(), 64);
			for (int strokes : player.strokes()) buf.writeVarInt(strokes + 1);
		}
	}

	private static RoundScorecardPayload decode(FriendlyByteBuf buf) {
		String courseId = buf.readUtf(128);
		int holeCount = buf.readVarInt();
		if (holeCount < 1) throw new IllegalArgumentException("invalid scorecard hole count");
		List<HoleColumn> holes = java.util.stream.IntStream.range(0, holeCount)
			.mapToObj(index -> new HoleColumn(buf.readVarInt(), buf.readVarInt())).toList();
		int playerCount = buf.readVarInt();
		if (playerCount < 1 || playerCount > 4) throw new IllegalArgumentException("invalid scorecard player count");
		List<PlayerRow> players = java.util.stream.IntStream.range(0, playerCount).mapToObj(index ->
			new PlayerRow(buf.readUtf(64), java.util.stream.IntStream.range(0, holeCount)
				.mapToObj(hole -> buf.readVarInt() - 1).toList())).toList();
		return new RoundScorecardPayload(courseId, holes, players);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
