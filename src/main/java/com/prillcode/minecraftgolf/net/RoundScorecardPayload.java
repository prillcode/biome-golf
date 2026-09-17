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

/** Server-authoritative final scorecard for the completed round. */
public record RoundScorecardPayload(String courseId, List<Integer> pars, List<PlayerRow> players)
		implements CustomPacketPayload {
	public static final int HOLE_COUNT = 18;
	public static final Type<RoundScorecardPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_scorecard_v1"));
	public static final StreamCodec<FriendlyByteBuf, RoundScorecardPayload> STREAM_CODEC =
		StreamCodec.of(RoundScorecardPayload::encode, RoundScorecardPayload::decode);

	public record PlayerRow(String name, List<Integer> strokes) {
		public PlayerRow {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(strokes, "strokes");
			strokes = List.copyOf(strokes);
			if (name.isBlank() || strokes.size() != HOLE_COUNT) {
				throw new IllegalArgumentException("scorecard rows must have a name and 18 holes");
			}
			if (strokes.stream().anyMatch(value -> value < -1)) {
				throw new IllegalArgumentException("unplayed holes must be -1");
			}
		}
	}

	public RoundScorecardPayload {
		Objects.requireNonNull(courseId, "courseId");
		Objects.requireNonNull(pars, "pars");
		Objects.requireNonNull(players, "players");
		pars = List.copyOf(pars);
		players = List.copyOf(players);
		if (courseId.isBlank() || pars.size() != HOLE_COUNT || players.isEmpty() || players.size() > 4) {
			throw new IllegalArgumentException("scorecard must contain 18 pars and 1-4 players");
		}
		if (pars.stream().anyMatch(value -> value < 0)) {
			throw new IllegalArgumentException("missing hole pars must be zero");
		}
	}

	public static List<Integer> pars(CourseDefinition course) {
		return java.util.stream.IntStream.range(0, HOLE_COUNT)
			.mapToObj(index -> index < course.holes().size() ? course.holes().get(index).par() : 0)
			.toList();
	}

	public static PlayerRow player(String name, CourseScorecard scorecard) {
		List<Integer> strokes = java.util.stream.IntStream.range(0, HOLE_COUNT)
			.mapToObj(index -> scorecard.holes().stream()
				.filter(score -> score.holeNumber() == index + 1)
				.map(score -> score.strokes()).findFirst().orElse(-1))
			.toList();
		return new PlayerRow(name, strokes);
	}

	private static void encode(FriendlyByteBuf buf, RoundScorecardPayload payload) {
		buf.writeUtf(payload.courseId(), 128);
		payload.pars().forEach(buf::writeByte);
		buf.writeByte(payload.players().size());
		for (PlayerRow player : payload.players()) {
			buf.writeUtf(player.name(), 64);
			player.strokes().forEach(buf::writeByte);
		}
	}

	private static RoundScorecardPayload decode(FriendlyByteBuf buf) {
		String courseId = buf.readUtf(128);
		List<Integer> pars = java.util.stream.IntStream.range(0, HOLE_COUNT)
			.mapToObj(index -> (int) buf.readByte()).toList();
		int playerCount = buf.readUnsignedByte();
		List<PlayerRow> players = java.util.stream.IntStream.range(0, playerCount).mapToObj(index ->
			new PlayerRow(buf.readUtf(64), java.util.stream.IntStream.range(0, HOLE_COUNT)
				.mapToObj(hole -> (int) buf.readByte()).toList())).toList();
		return new RoundScorecardPayload(courseId, pars, players);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
