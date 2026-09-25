package pro.apdev.biomegolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Client intent to invoke the existing server-authoritative next-hole barrier. */
public record NextHoleRequestPayload() implements CustomPacketPayload {

	public static final NextHoleRequestPayload INSTANCE = new NextHoleRequestPayload();
	public static final Type<NextHoleRequestPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "next_hole_request"));
	public static final StreamCodec<FriendlyByteBuf, NextHoleRequestPayload> STREAM_CODEC =
		StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
