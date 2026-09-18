package com.prillcode.minecraftgolf.net;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.network.FriendlyByteBuf;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.club.ShotType;

/**
 * Client → server finalized shot intent (M3, ARCH §8.1/§20).
 *
 * <p>The client locally times the three-click meter and mails this to the server,
 * which re-validates and resolves the launch. Carries no outcome — the server is
 * authoritative for the resulting flight (D009).</p>
 *
 * @param ballId      entity id of the golf ball the player intends to strike
 * @param aimYawDeg   Minecraft yaw of the aim line
 * @param aimPitchDeg Minecraft pitch (0 level; positive down)
 * @param power       0..1 fraction of full club power
 * @param accuracy    0..1 on-target lane; 0.5 is a perfect hit
 * @param shotType    server-validated trajectory intent
 */
public record ShotRequestPayload(
		int ballId, float aimYawDeg, float aimPitchDeg, float power, float accuracy, ShotType shotType)
		implements CustomPacketPayload {

	public static final Type<ShotRequestPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "shot_request_v2"));

	public static final StreamCodec<FriendlyByteBuf, ShotRequestPayload> STREAM_CODEC =
			StreamCodec.of(ShotRequestPayload::encode, ShotRequestPayload::decode);

	private static void encode(FriendlyByteBuf buf, ShotRequestPayload p) {
		buf.writeInt(p.ballId());
		buf.writeFloat(p.aimYawDeg());
		buf.writeFloat(p.aimPitchDeg());
		buf.writeFloat(p.power());
		buf.writeFloat(p.accuracy());
		buf.writeByte(p.shotType().ordinal());
	}

	private static ShotRequestPayload decode(FriendlyByteBuf buf) {
		return new ShotRequestPayload(
				buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				ShotType.fromWire(buf.readUnsignedByte()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
