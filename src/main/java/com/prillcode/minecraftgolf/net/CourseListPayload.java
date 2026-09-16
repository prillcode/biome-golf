package com.prillcode.minecraftgolf.net;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;

/** Server-authoritative, display-only list of finalized courses. */
public record CourseListPayload(List<CourseEntry> courses) implements CustomPacketPayload {

	public record CourseEntry(String id, String displayName, int holeCount, int totalPar) {
	}

	public static final Type<CourseListPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "course_list"));
	public static final StreamCodec<FriendlyByteBuf, CourseListPayload> STREAM_CODEC =
		StreamCodec.of(CourseListPayload::encode, CourseListPayload::decode);

	public CourseListPayload {
		courses = List.copyOf(courses);
		if (courses.size() > 64) throw new IllegalArgumentException("too many courses");
	}

	private static void encode(FriendlyByteBuf buf, CourseListPayload payload) {
		buf.writeVarInt(payload.courses.size());
		for (CourseEntry course : payload.courses) {
			buf.writeUtf(course.id, 128);
			buf.writeUtf(course.displayName, 256);
			buf.writeVarInt(course.holeCount);
			buf.writeVarInt(course.totalPar);
		}
	}

	private static CourseListPayload decode(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		if (count < 0 || count > 64) throw new IllegalArgumentException("invalid course count");
		List<CourseEntry> courses = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			courses.add(new CourseEntry(buf.readUtf(128), buf.readUtf(256), buf.readVarInt(), buf.readVarInt()));
		}
		return new CourseListPayload(courses);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
