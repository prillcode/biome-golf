package pro.apdev.biomegolf.net;

import java.util.List;
import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;
import pro.apdev.biomegolf.round.RoundLobbyProjection;

/** Server-authoritative, display-only projection of finalized courses and open lobbies. */
public record CourseListPayload(List<CourseEntry> courses, List<LobbyEntry> lobbies) implements CustomPacketPayload {

	public record CourseEntry(String id, String displayName, int holeCount, int totalPar) {
	}
	public record LobbyEntry(UUID roundId, String courseId, String courseName,
		String coordinatorName, int participantCount, int capacity) {
	}

	public static final Type<CourseListPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_browser_v2"));
	public static final StreamCodec<FriendlyByteBuf, CourseListPayload> STREAM_CODEC =
		StreamCodec.of(CourseListPayload::encode, CourseListPayload::decode);

	public CourseListPayload {
		courses = List.copyOf(courses);
		lobbies = List.copyOf(lobbies);
	}

	public static CourseListPayload from(RoundLobbyProjection projection) {
		return new CourseListPayload(projection.courses().stream().map(course -> new CourseEntry(
			course.id(), course.displayName(), course.holeCount(), course.totalPar())).toList(),
			projection.lobbies().stream().map(lobby -> new LobbyEntry(lobby.roundId(), lobby.courseId(),
				lobby.courseName(), lobby.coordinatorName(), lobby.participantCount(), lobby.capacity())).toList());
	}

	private static void encode(FriendlyByteBuf buf, CourseListPayload payload) {
		buf.writeVarInt(payload.courses.size());
		for (CourseEntry course : payload.courses) {
			buf.writeUtf(course.id, 128);
			buf.writeUtf(course.displayName, 256);
			buf.writeVarInt(course.holeCount);
			buf.writeVarInt(course.totalPar);
		}
		buf.writeVarInt(payload.lobbies.size());
		for (LobbyEntry lobby : payload.lobbies) {
			buf.writeUUID(lobby.roundId); buf.writeUtf(lobby.courseId, 128); buf.writeUtf(lobby.courseName, 256);
			buf.writeUtf(lobby.coordinatorName, 64); buf.writeVarInt(lobby.participantCount); buf.writeVarInt(lobby.capacity);
		}
	}

	private static CourseListPayload decode(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		if (count < 0) throw new IllegalArgumentException("invalid course count");
		List<CourseEntry> courses = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			courses.add(new CourseEntry(buf.readUtf(128), buf.readUtf(256), buf.readVarInt(), buf.readVarInt()));
		}
		int lobbyCount = buf.readVarInt();
		if (lobbyCount < 0) throw new IllegalArgumentException("invalid lobby count");
		List<LobbyEntry> lobbies = new java.util.ArrayList<>();
		for (int i = 0; i < lobbyCount; i++) {
			lobbies.add(new LobbyEntry(buf.readUUID(), buf.readUtf(128), buf.readUtf(256),
				buf.readUtf(64), buf.readVarInt(), buf.readVarInt()));
		}
		return new CourseListPayload(courses, lobbies);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
