package pro.apdev.biomegolf.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Presentation-only snapshot consumed by {@link GolfBallEntityRenderer}.
 *
 * <p>The base state carries the interpolated entity position, lighting, and
 * outline data needed by the 26.2 submit pipeline. Gameplay state remains on
 * the server-owned entity.</p>
 */
public final class GolfBallRenderState extends EntityRenderState {
}
