package pro.apdev.biomegolf.net;

import java.util.UUID;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Authoritative lobby display state; NONE clears stale client lobby UI. */
public record LobbyStatePayload(Phase phase, UUID roundId, String courseId, String courseName,
		int participantCount, int maximumParticipants, boolean participating,
		boolean coordinator, boolean startable) implements CustomPacketPayload {
	public enum Phase { NONE, LOBBY, PLAYING, COMPLETE }
	public static final Type<LobbyStatePayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "lobby_state_v2"));
	public static final StreamCodec<FriendlyByteBuf, LobbyStatePayload> STREAM_CODEC =
		StreamCodec.of(LobbyStatePayload::encode, LobbyStatePayload::decode);
	public LobbyStatePayload {
		if (phase == null) throw new NullPointerException("phase");
		if ((phase == Phase.NONE) != (roundId == null)) throw new IllegalArgumentException("round id must match lobby phase");
		if (participantCount < 0 || maximumParticipants < 0) throw new IllegalArgumentException("invalid participant count");
	}
	public static LobbyStatePayload none() { return new LobbyStatePayload(Phase.NONE, null, "", "", 0, 0, false, false, false); }
	private static void encode(FriendlyByteBuf buf, LobbyStatePayload p) {
		buf.writeByte(p.phase.ordinal());
		if (p.roundId != null) buf.writeUUID(p.roundId);
		buf.writeUtf(p.courseId, 128); buf.writeUtf(p.courseName, 256);
		buf.writeVarInt(p.participantCount); buf.writeVarInt(p.maximumParticipants);
		buf.writeBoolean(p.participating); buf.writeBoolean(p.coordinator); buf.writeBoolean(p.startable);
	}
	private static LobbyStatePayload decode(FriendlyByteBuf buf) {
		Phase phase = Phase.values()[buf.readByte()];
		return new LobbyStatePayload(phase, phase == Phase.NONE ? null : buf.readUUID(), buf.readUtf(128), buf.readUtf(256),
			buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
