package pro.apdev.biomegolf.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import pro.apdev.biomegolf.MinecraftGolf;

/** Bounded client intent from the contextual Golf Menu. */
public record GolfMenuActionPayload(Action action) implements CustomPacketPayload {
	public enum Action { OPEN_COURSE_BROWSER, START_ROUND, LEAVE_ROUND, REPLAY_HOLE, REPLAY_ROUND }
	public static final Type<GolfMenuActionPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "golf_menu_action_v1"));
	public static final StreamCodec<FriendlyByteBuf, GolfMenuActionPayload> STREAM_CODEC =
		StreamCodec.of((buf, payload) -> buf.writeByte(payload.action().ordinal()),
			buf -> new GolfMenuActionPayload(Action.values()[buf.readByte()]));
	public GolfMenuActionPayload {
		if (action == null) throw new NullPointerException("action");
	}
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
