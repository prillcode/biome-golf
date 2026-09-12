package com.prillcode.minecraftgolf.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.prillcode.minecraftgolf.MinecraftGolf;

/**
 * Registry holder for the golf ball entity type.
 *
 * <p>Registration happens explicitly from {@link MinecraftGolf#onInitialize}
 * so the mod logs its presence and the entity is summonable
 * ({@code /summon minecraft_golf:golf_ball}).</p>
 */
public final class GolfBallEntities {

	public static final ResourceKey<EntityType<?>> GOLF_BALL_KEY = ResourceKey.create(
			Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "golf_ball"));

	public static final EntityType<GolfBallEntity> GOLF_BALL = registerGolfBall();

	private GolfBallEntities() {
	}

	@SuppressWarnings("unchecked")
	private static EntityType<GolfBallEntity> registerGolfBall() {
		// build() already returns EntityType<GolfBallEntity>; register() needs the
		// registry's EntityType<?> view, so widen then narrow back.
		EntityType<?> type = Registry.register(
				BuiltInRegistries.ENTITY_TYPE,
				GOLF_BALL_KEY,
				EntityType.Builder.of(GolfBallEntity::new, MobCategory.MISC)
						.sized((float) (GolfBallEntity.BALL_RADIUS * 2.0), (float) (GolfBallEntity.BALL_RADIUS * 2.0))
						// Shots can travel beyond the player's local 10-chunk view while the
						// post-shot camera follows the server-authoritative ball.
						.clientTrackingRange(16)
						.updateInterval(1)
						.build(GOLF_BALL_KEY));
		return (EntityType<GolfBallEntity>) type;
	}
}
