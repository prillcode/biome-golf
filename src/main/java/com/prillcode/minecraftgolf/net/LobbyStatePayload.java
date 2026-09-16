package com.prillcode.minecraftgolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Authoritative lobby display state; NONE clears stale client lobby UI. */
public record LobbyStatePayload(Phase phase, String courseId, String courseName,
		int participantCount, int maximumParticipants, boolean participating,
		boolean coordinator, boolean startable) implements CustomPacketPayload {
	public enum Phase { NONE, LOBBY, PLAYING }
	public static final Type<LobbyStatePayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "lobby_state"));
	public static final StreamCodec<FriendlyByteBuf, LobbyStatePayload> STREAM_CODEC =
		StreamCodec.of(LobbyStatePayload::encode, LobbyStatePayload::decode);
	public LobbyStatePayload {
		if (phase == null) throw new NullPointerException("phase");
		if (participantCount < 0 || maximumParticipants < 0) throw new IllegalArgumentException("invalid participant count");
	}
	public static LobbyStatePayload none() { return new LobbyStatePayload(Phase.NONE, "", "", 0, 0, false, false, false); }
	private static void encode(FriendlyByteBuf buf, LobbyStatePayload p) {
		buf.writeByte(p.phase.ordinal()); buf.writeUtf(p.courseId, 128); buf.writeUtf(p.courseName, 256);
		buf.writeVarInt(p.participantCount); buf.writeVarInt(p.maximumParticipants);
		buf.writeBoolean(p.participating); buf.writeBoolean(p.coordinator); buf.writeBoolean(p.startable);
	}
	private static LobbyStatePayload decode(FriendlyByteBuf buf) {
		return new LobbyStatePayload(Phase.values()[buf.readByte()], buf.readUtf(128), buf.readUtf(256),
			buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
