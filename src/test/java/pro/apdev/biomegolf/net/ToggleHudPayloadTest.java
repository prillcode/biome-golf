package pro.apdev.biomegolf.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ToggleHudPayloadTest {

	@Test
	void payloadHasItsOwnVersionedClientboundId() {
		assertEquals("minecraft_golf:toggle_hud_v1", ToggleHudPayload.TYPE.id().toString());
		assertSame(ToggleHudPayload.TYPE, ToggleHudPayload.INSTANCE.type());
	}

	@Test
	void toggleIntentRoundTripsWithoutSendingVisibilityState() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		try {
			ToggleHudPayload.STREAM_CODEC.encode(buffer, ToggleHudPayload.INSTANCE);
			assertEquals(0, buffer.readableBytes());
			assertSame(ToggleHudPayload.INSTANCE, ToggleHudPayload.STREAM_CODEC.decode(buffer));
		} finally {
			buffer.release();
		}
	}
}
