package com.prillcode.minecraftgolf.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import com.prillcode.minecraftgolf.MinecraftGolf;
import com.prillcode.minecraftgolf.entity.GolfBallEntity;

/**
 * Client-only presentation for the server-authoritative golf ball entity.
 *
 * <p>The 26.2 renderer pipeline collects geometry during {@link #submit}; this
 * renderer submits one camera-facing, shaded circle whose world dimensions
 * match the entity's 0.25-block radius. It reads render state only and never
 * advances or predicts physics.</p>
 */
public final class GolfBallEntityRenderer extends EntityRenderer<GolfBallEntity, GolfBallRenderState> {

	private static final float RADIUS = (float) GolfBallEntity.BALL_RADIUS;
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(
			Identifier.fromNamespaceAndPath(MinecraftGolf.MOD_ID, "textures/entity/golf_ball.png"));

	public GolfBallEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = RADIUS;
	}

	@Override
	public GolfBallRenderState createRenderState() {
		return new GolfBallRenderState();
	}

	@Override
	public void submit(
			GolfBallRenderState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraState) {
		poseStack.pushPose();
		poseStack.mulPose(cameraState.orientation);
		submitNodeCollector.submitCustomGeometry(
				poseStack,
				RENDER_TYPE,
				(pose, vertices) -> submitBallQuad(pose, vertices, state.lightCoords));
		poseStack.popPose();

		super.submit(state, poseStack, submitNodeCollector, cameraState);
	}

	private static void submitBallQuad(PoseStack.Pose pose, VertexConsumer vertices, int packedLight) {
		vertex(vertices, pose, -RADIUS, 0.0F, 0.0F, 1.0F, packedLight);
		vertex(vertices, pose, RADIUS, 0.0F, 1.0F, 1.0F, packedLight);
		vertex(vertices, pose, RADIUS, RADIUS * 2.0F, 1.0F, 0.0F, packedLight);
		vertex(vertices, pose, -RADIUS, RADIUS * 2.0F, 0.0F, 0.0F, packedLight);
	}

	private static void vertex(
			VertexConsumer vertices,
			PoseStack.Pose pose,
			float x,
			float y,
			float u,
			float v,
			int packedLight) {
		vertices.addVertex(pose, x, y, 0.0F)
				.setColor(0xFFFFFFFF)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(packedLight)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
