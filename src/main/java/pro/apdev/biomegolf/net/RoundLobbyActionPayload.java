package pro.apdev.biomegolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Bounded client intent for a Ready Golf lobby action. */
public record RoundLobbyActionPayload(Action action, String targetId) implements CustomPacketPayload {
	public enum Action { CREATE, JOIN, START, LEAVE, RESTART_HOLE, REPLAY_ROUND }
	public static final Type<RoundLobbyActionPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_lobby_action_v2"));
	public static final StreamCodec<FriendlyByteBuf, RoundLobbyActionPayload> STREAM_CODEC =
		StreamCodec.of(RoundLobbyActionPayload::encode, RoundLobbyActionPayload::decode);
	public RoundLobbyActionPayload {
		if (action == null) throw new NullPointerException("action");
		if (targetId == null || targetId.isBlank() || targetId.length() > 128) throw new IllegalArgumentException("invalid action target");
		if (action != Action.CREATE) java.util.UUID.fromString(targetId);
	}
	private static void encode(FriendlyByteBuf buf, RoundLobbyActionPayload p) {
		buf.writeByte(p.action.ordinal());
		buf.writeUtf(p.targetId, 128);
	}
	private static RoundLobbyActionPayload decode(FriendlyByteBuf buf) {
		return new RoundLobbyActionPayload(Action.values()[buf.readByte()], buf.readUtf(128));
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
