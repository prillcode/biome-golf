package pro.apdev.biomegolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Client intent for an action on a completed Ready Golf round. */
public record RoundActionPayload(Action action) implements CustomPacketPayload {
	public enum Action { REPLAY, LEAVE }
	public static final Type<RoundActionPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_action_v1"));
	public static final StreamCodec<FriendlyByteBuf, RoundActionPayload> STREAM_CODEC =
		StreamCodec.of((buf, payload) -> buf.writeByte(payload.action().ordinal()),
			buf -> new RoundActionPayload(Action.values()[buf.readByte()]));

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
