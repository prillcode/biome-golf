package com.prillcode.minecraftgolf.client.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.prillcode.minecraftgolf.entity.GolfBallEntity;

/**
 * Placeholder client renderer for the golf ball (M001-S03 interim; M001-S04
 * will replace this with the real visual ball renderer).
 *
 * <p>Minecraft 26.2 hard-crashes the client when a tracked entity type has no
 * registered renderer (the render dispatcher NPEs on a null renderer instead
 * of skipping the entity). This empty renderer exists solely so a spawned ball
 * is safe to have in the world while S03's server-side dev commands are
 * exercised on runClient — it draws nothing.</p>
 *
 * <p>Client-only: lives in {@code src/client}, never referenced from common or
 * server code. The ball remains server-authoritative; rendering is purely
 * presentation and does not affect physics.</p>
 */
public class GolfBallEntityRenderer extends EntityRenderer<GolfBallEntity, EntityRenderState> {

	public GolfBallEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	// Base extractRenderState fills entityType/position from the entity, and the
	// default submit() renders nothing (no model, no texture) — the intended S03
	// placeholder behavior. S04 adds the real render here.
}
