package com.prillcode.minecraftgolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import org.junit.jupiter.api.Test;

import com.prillcode.minecraftgolf.club.ShotType;

class ShotRequestPayloadTest {
	@Test
	void wireSchemaIsVersionedAndShotTypeDefaultsDefensively() {
		assertEquals("minecraft_golf:shot_request_v2", ShotRequestPayload.TYPE.id().toString());
		assertEquals(ShotType.FLOP, ShotType.fromWire(ShotType.FLOP.ordinal()));
		assertEquals(ShotType.STANDARD, ShotType.fromWire(255));
	}

	@Test
	void shotTypeSurvivesPayloadRoundTrip() {
		ShotRequestPayload original = new ShotRequestPayload(42, 12.5f, -4.0f, 0.8f, 0.5f, ShotType.FLOP);
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		ShotRequestPayload.STREAM_CODEC.encode(buffer, original);
		assertEquals(original, ShotRequestPayload.STREAM_CODEC.decode(buffer));
	}
}
