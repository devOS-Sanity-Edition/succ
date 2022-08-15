package one.devos.nautical.succ;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import one.devos.nautical.succ.model.DepressedSuctionCupModel;
import one.devos.nautical.succ.model.SuctionCupModel;

public class SuctionCupEntityRenderer extends EntityRenderer<ClimbingSuctionCupEntity> {
	private final SuctionCupModel cupModel;
	private final DepressedSuctionCupModel depressedModel;

	protected SuctionCupEntityRenderer(Context context) {
		super(context);
		this.cupModel = new SuctionCupModel(context);
		this.depressedModel = new DepressedSuctionCupModel(context);
	}

	@Override
	public void render(ClimbingSuctionCupEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		if (entity.isInvisible())
			return;
		matrices.pushPose();

		matrices.scale(0.75f, 0.75f, 0.75f);
		matrices.mulPose(Vector3f.XN.rotationDegrees(90));
		matrices.translate(0, -1.23, 0.35);

		VertexConsumer consumer = vertexConsumers.getBuffer(cupModel.renderType(SuctionCupModel.TEXTURE));
		this.cupModel.renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

		matrices.popPose();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(ClimbingSuctionCupEntity entity) {
		return SuctionCupModel.TEXTURE;
	}
}
