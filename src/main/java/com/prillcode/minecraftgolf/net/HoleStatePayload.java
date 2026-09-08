package com.prillcode.minecraftgolf.net;

import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.hole.GolfScoreTerm;
import com.prillcode.minecraftgolf.hole.HoleCompletionReason;
import com.prillcode.minecraftgolf.hole.HoleDefinition;
import com.prillcode.minecraftgolf.hole.PlayerHoleState;

/**
 * Server → client typed snapshot of a player's hole display state (S03, ARCH §20).
 *
 * <p>Carries only display-ready values authorised by the server; the client must
 * not derive scoring outcomes from this data. {@code completionReason} and
 * {@code scoreTerm} are null outside a {@link Phase#COMPLETE} snapshot.</p>
 */
public record HoleStatePayload(
		Phase phase,
		int holeNumber,
		int par,
		int strokeLimit,
		int strokes,
		int penaltyCount,
		int scoreToPar,
		HoleCompletionReason completionReason,
		GolfScoreTerm scoreTerm
) implements CustomPacketPayload {

	/** Four display phases covering the full player-hole lifecycle. */
	public enum Phase { PRACTICE, ACTIVE, MISSING_BALL, COMPLETE }

	public HoleStatePayload {
		Objects.requireNonNull(phase, "phase");
	}

	public static final Type<HoleStatePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "hole_state"));

	public static final StreamCodec<FriendlyByteBuf, HoleStatePayload> STREAM_CODEC =
			StreamCodec.of(HoleStatePayload::encode, HoleStatePayload::decode);

	// ── Factory methods ──────────────────────────────────────────────────────

	/** PRACTICE snapshot: no active session. */
	public static HoleStatePayload practice(HoleDefinition hole) {
		return new HoleStatePayload(Phase.PRACTICE,
				hole.number(), hole.par(), hole.strokeLimit(),
				0, 0, 0, null, null);
	}

	/** ACTIVE snapshot: hole is in progress. */
	public static HoleStatePayload active(PlayerHoleState state) {
		return new HoleStatePayload(Phase.ACTIVE,
				state.hole().number(), state.hole().par(), state.hole().strokeLimit(),
				state.strokes(), state.penaltyStrokes(),
				state.strokes() > 0 ? state.scoreToPar() : 0,
				null, null);
	}

	/** MISSING_BALL snapshot: session exists but assigned ball entity is gone. */
	public static HoleStatePayload missingBall(PlayerHoleState state) {
		return new HoleStatePayload(Phase.MISSING_BALL,
				state.hole().number(), state.hole().par(), state.hole().strokeLimit(),
				state.strokes(), state.penaltyStrokes(),
				state.strokes() > 0 ? state.scoreToPar() : 0,
				null, null);
	}

	/** COMPLETE snapshot: hole finished by any means. */
	public static HoleStatePayload complete(PlayerHoleState state) {
		return new HoleStatePayload(Phase.COMPLETE,
				state.hole().number(), state.hole().par(), state.hole().strokeLimit(),
				state.strokes(), state.penaltyStrokes(),
				state.strokes() > 0 ? state.scoreToPar() : 0,
				state.completionReason(),
				state.strokes() > 0 ? state.scoreTerm() : null);
	}

	// ── Codec ────────────────────────────────────────────────────────────────

	private static void encode(FriendlyByteBuf buf, HoleStatePayload p) {
		buf.writeByte(p.phase().ordinal());
		buf.writeInt(p.holeNumber());
		buf.writeInt(p.par());
		buf.writeInt(p.strokeLimit());
		buf.writeInt(p.strokes());
		buf.writeInt(p.penaltyCount());
		buf.writeInt(p.scoreToPar());
		boolean hasReason = p.completionReason() != null;
		buf.writeBoolean(hasReason);
		if (hasReason) {
			buf.writeByte(p.completionReason().ordinal());
		}
		boolean hasTerm = p.scoreTerm() != null;
		buf.writeBoolean(hasTerm);
		if (hasTerm) {
			buf.writeByte(p.scoreTerm().ordinal());
		}
	}

	private static HoleStatePayload decode(FriendlyByteBuf buf) {
		Phase phase = Phase.values()[buf.readByte()];
		int holeNumber = buf.readInt();
		int par = buf.readInt();
		int strokeLimit = buf.readInt();
		int strokes = buf.readInt();
		int penaltyCount = buf.readInt();
		int scoreToPar = buf.readInt();
		HoleCompletionReason completionReason = buf.readBoolean()
				? HoleCompletionReason.values()[buf.readByte()]
				: null;
		GolfScoreTerm scoreTerm = buf.readBoolean()
				? GolfScoreTerm.values()[buf.readByte()]
				: null;
		return new HoleStatePayload(phase, holeNumber, par, strokeLimit, strokes,
				penaltyCount, scoreToPar, completionReason, scoreTerm);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
