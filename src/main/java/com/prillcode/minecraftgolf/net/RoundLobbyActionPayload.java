package com.prillcode.minecraftgolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Bounded client intent for a Ready Golf lobby action. */
public record RoundLobbyActionPayload(Action action, String courseId) implements CustomPacketPayload {
	public enum Action { CREATE, JOIN, START, LEAVE }
	public static final Type<RoundLobbyActionPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_lobby_action"));
	public static final StreamCodec<FriendlyByteBuf, RoundLobbyActionPayload> STREAM_CODEC =
		StreamCodec.of(RoundLobbyActionPayload::encode, RoundLobbyActionPayload::decode);
	public RoundLobbyActionPayload {
		if (action == null) throw new NullPointerException("action");
		if (courseId == null || courseId.length() > 128) throw new IllegalArgumentException("invalid course id");
	}
	private static void encode(FriendlyByteBuf buf, RoundLobbyActionPayload p) {
		buf.writeByte(p.action.ordinal());
		buf.writeUtf(p.courseId, 128);
	}
	private static RoundLobbyActionPayload decode(FriendlyByteBuf buf) {
		return new RoundLobbyActionPayload(Action.values()[buf.readByte()], buf.readUtf(128));
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
