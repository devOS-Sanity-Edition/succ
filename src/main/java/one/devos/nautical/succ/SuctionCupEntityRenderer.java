package one.devos.nautical.succ;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import one.devos.nautical.succ.model.DepressedSuctionCupModel;
import one.devos.nautical.succ.model.SuctionCupModel;

public class SuctionCupEntityRenderer extends EntityRenderer<ClimbingSuctionCupEntity> {
	private final Minecraft mc;
	private final SuctionCupModel cupModel;
	private final DepressedSuctionCupModel depressedModel;

	protected SuctionCupEntityRenderer(Context context) {
		super(context);
		this.mc = Minecraft.getInstance();
		this.cupModel = new SuctionCupModel(context);
		this.depressedModel = new DepressedSuctionCupModel(context);
	}

	@Override
	public void render(ClimbingSuctionCupEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		if (!entity.isInvisible()) {
			renderEntity(entity, yaw, tickDelta, matrices, vertexConsumers, light);
			renderPlayerLimb(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		}
	}

	public void renderEntity(ClimbingSuctionCupEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		matrices.pushPose();

		matrices.scale(0.75f, 0.75f, 0.75f);
		matrices.mulPose(Vector3f.XN.rotationDegrees(90));
		matrices.mulPose(Vector3f.ZN.rotationDegrees(entity.facing.toYRot() + 180));
		if (entity.limb.hand) {
			matrices.mulPose(Vector3f.YN.rotationDegrees(90));
			matrices.translate(0.3, -1.23, 0);
		} else {
			matrices.translate(-0.01, -1.23, 0.3);
		}

		Model model = entity.getSuction() && !entity.isMoving() ? depressedModel : cupModel;
		VertexConsumer consumer = vertexConsumers.getBuffer(model.renderType(SuctionCupModel.TEXTURE));
		model.renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

		matrices.popPose();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	public void renderPlayerLimb(ClimbingSuctionCupEntity entity, float entityYaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		if (!(entity.getOwner() instanceof AbstractClientPlayer owner))
			return;
		EntityRenderer<? super LocalPlayer> playerRenderer = mc.getEntityRenderDispatcher().getRenderer(owner);
		if (!(playerRenderer instanceof PlayerRenderer renderer))
			return;
		PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();
		SuctionCupLimb limb = entity.limb;
		ModelPart limbPart = switch (limb) {
			case LEFT_HAND -> playerModel.leftArm;
			case RIGHT_HAND -> playerModel.rightArm;
			case LEFT_FOOT -> playerModel.leftLeg;
			case RIGHT_FOOT -> playerModel.rightLeg;
		};
		boolean oldLimbVisibility = limbPart.visible;
		limbPart.visible = true;
		ModelPart overlay = switch (limb) {
			case LEFT_HAND -> playerModel.leftSleeve;
			case RIGHT_HAND -> playerModel.rightSleeve;
			case LEFT_FOOT -> playerModel.leftPants;
			case RIGHT_FOOT -> playerModel.rightPants;
		};
		boolean oldOverlayVisibility = overlay.visible;
		PlayerModelPart part = switch (limb) {
			case LEFT_HAND -> PlayerModelPart.LEFT_SLEEVE;
			case RIGHT_HAND -> PlayerModelPart.RIGHT_SLEEVE;
			case LEFT_FOOT -> PlayerModelPart.LEFT_PANTS_LEG;
			case RIGHT_FOOT -> PlayerModelPart.RIGHT_PANTS_LEG;
		};
		overlay.visible = owner.isModelPartShown(part);
		Vec3 playerPos = owner.getPosition(tickDelta);
		Vec3 limbConnection = playerPos.add(SuccUtils.rotateVec(limb.offsetFromPlayer, entity.facing.toYRot()));
		Vec3 handlePos = entity.getHandlePos(tickDelta);
		boolean depressed = entity.getSuction() && !entity.isMoving();

		double dX = limbConnection.x - handlePos.x;
		double dY = limbConnection.y - handlePos.y;
		double dZ = limbConnection.z - handlePos.z;

		float yaw = (float) -Math.atan2(dX, dZ);
		float pitch = (float) Math.atan2(dY, dZ) - Mth.HALF_PI;
		limbPart.setRotation(pitch, yaw, Mth.PI);

		float limbXOld = limbPart.x;
		float limbYOld = limbPart.y;
		float limbZOld = limbPart.z;
		limbPart.x = Mth.sin(yaw) * -10;
		limbPart.y = Mth.cos(pitch) * 11 + 3;
		limbPart.z = Mth.cos(yaw) * 11;
		if (depressed) {
			limbPart.z -= 2.5;
		}

		overlay.copyFrom(limbPart);

		VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(owner.getSkinTextureLocation()));

		matrices.pushPose();

		limbPart.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
		overlay.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY);

		matrices.popPose();

		limbPart.x = limbXOld;
		limbPart.y = limbYOld;
		limbPart.z = limbZOld;
		limbPart.visible = oldLimbVisibility;
		overlay.x = limbXOld;
		overlay.y = limbYOld;
		overlay.z = limbZOld;
		overlay.visible = oldOverlayVisibility;
	}

	@Override
	public ResourceLocation getTextureLocation(ClimbingSuctionCupEntity entity) {
		return SuctionCupModel.TEXTURE;
	}
}
