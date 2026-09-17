package com.prillcode.minecraftgolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Client intent to open the server's finalized-course browser. */
public record CourseListRequestPayload() implements CustomPacketPayload {
	public static final CourseListRequestPayload INSTANCE = new CourseListRequestPayload();
	public static final Type<CourseListRequestPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "round_browser_request_v2"));
	public static final StreamCodec<FriendlyByteBuf, CourseListRequestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
