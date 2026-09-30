package pro.apdev.biomegolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Clientbound presentation intent: toggle both golf HUDs from their local visibility. */
public record ToggleHudPayload() implements CustomPacketPayload {

	public static final ToggleHudPayload INSTANCE = new ToggleHudPayload();
	public static final Type<ToggleHudPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "toggle_hud_v1"));
	public static final StreamCodec<FriendlyByteBuf, ToggleHudPayload> STREAM_CODEC =
		StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
