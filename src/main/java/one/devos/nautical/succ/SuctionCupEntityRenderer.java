package one.devos.nautical.succ;

import static one.devos.nautical.succ.SuccUtils.axisChoose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import one.devos.nautical.succ.model.DepressedSuctionCupModel;
import one.devos.nautical.succ.model.SuctionCupModel;

public class SuctionCupEntityRenderer extends EntityRenderer<ClimbingSuctionCupEntity> {
	public static final float LIMB_LENGTH = 13; // found through trial and error, the length of a limb in ModelPart space

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
		renderEntity(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		renderPlayerLimb(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	public void renderEntity(ClimbingSuctionCupEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		if (entity.isInvisible())
			return;

		matrices.pushPose();

		matrices.scale(0.75f, 0.75f, 0.75f);
		matrices.mulPose(com.mojang.math.Axis.XN.rotationDegrees(90));
		matrices.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(entity.facing.toYRot() + 180));
		if (entity.limb.hand) {
			matrices.mulPose(com.mojang.math.Axis.YN.rotationDegrees(90));
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
		if (owner.isInvisible())
			return;
		EntityRenderer<? super LocalPlayer> playerRenderer = mc.getEntityRenderDispatcher().getRenderer(owner);
		if (!(playerRenderer instanceof PlayerRenderer renderer))
			return;
		PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();

		SuctionCupLimb limb = entity.limb;
		Vec3 handlePos = entity.getHandlePos(tickDelta);

		ModelState state = prepareModel(owner, playerModel, limb);
		ModelPart limbPart = state.limbPart;
		ModelPart overlay = state.overlay;

		Direction facing = entity.facing;
		Axis facingAxis = facing.getAxis();
		Direction right = facing.getClockWise();
		Axis rightAxis = right.getAxis();

		int mult = facing.getAxisDirection().getStep();

		Vec3 playerPos = owner.getPosition(tickDelta);
		Vec3 limbConnection = limb.offsetFromPlayer.multiply(mult, 1, mult);
		if (facingAxis == Axis.X) {
			limbConnection = SuccUtils.rotateVec(limbConnection.multiply(-1, 1, 1).add(0.1, 0, 0), 90);
		}

		Vec3 handleRelative = handlePos.subtract(playerPos);

		// FIXME this code might be an info-hazard

		double dX = axisChoose(rightAxis, limbConnection) - axisChoose(rightAxis, handleRelative);
		double dY = limbConnection.y - handleRelative.y;
		double dZ = axisChoose(facingAxis, limbConnection) - axisChoose(facingAxis, handleRelative);
		float distance = Mth.sqrt((float) (dX * dX + dY * dY + dZ * dZ));

		float yaw, pitch, roll, x, y, z;

		if (facingAxis == Axis.Z) {
			yaw = (float) Math.atan2(dX, dZ) * mult * -mult;
			pitch = (float) (Math.atan2(dY, dZ) * -mult) + Mth.HALF_PI * mult;
			roll = Mth.PI;
			x = -(Mth.sin(yaw) * LIMB_LENGTH);
			y = (Mth.cos(pitch) * LIMB_LENGTH) + 3;
			z = (Mth.cos(yaw) * distance) - 7 * mult;
		} else {
			pitch = Mth.HALF_PI;
			roll = (float) (Math.atan2(dY, dZ) * mult) + Mth.HALF_PI * mult - Mth.HALF_PI; // actually pitch
			if (facing.getAxisDirection() == AxisDirection.NEGATIVE) {
				roll += Mth.PI;
				roll *= mult;
			}
			yaw = (float) Math.atan2(dZ, dX) * -mult + Mth.PI;
			z = -(Mth.cos(yaw) * LIMB_LENGTH) - 2; // actually x
			y = (Mth.sin(roll) * LIMB_LENGTH) + 3;
			x = (Mth.sin(yaw) * distance) * mult - 9 * mult; // actually z
		}

		limbPart.setRotation(pitch, yaw, roll);
		limbPart.setPos(x, y, z);
		overlay.copyFrom(limbPart);

		VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(owner.getSkinTextureLocation()));

		matrices.pushPose();

		matrices.pushPose();
		limbPart.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
		matrices.popPose();
		matrices.pushPose();
		overlay.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
		matrices.popPose();

		matrices.popPose();

		state.restore();
	}

	@Override
	public ResourceLocation getTextureLocation(ClimbingSuctionCupEntity entity) {
		return SuctionCupModel.TEXTURE;
	}

	public static ModelState prepareModel(AbstractClientPlayer owner, PlayerModel<AbstractClientPlayer> model, SuctionCupLimb limb) {
		ModelPart limbPart = getLimb(model, limb);
		ModelPart overlay = getOverlay(model, limb);
		PlayerModelPart part = limb.playerPart;
		ModelState state = new ModelState(
				limbPart, limbPart.visible, overlay, overlay.visible,
				limbPart.x, limbPart.y, limbPart.z, limbPart.xRot, limbPart.yRot, limbPart.zRot
		);
		limbPart.setPos(0, 0, 0);
		limbPart.setRotation(0, 0, 0);
		overlay.copyFrom(limbPart);
		limbPart.visible = true;
		overlay.visible = owner.isModelPartShown(part);
		return state;
	}

	public static ModelPart getLimb(PlayerModel<AbstractClientPlayer> model, SuctionCupLimb limb) {
		return switch (limb) {
			case LEFT_HAND -> model.leftArm;
			case RIGHT_HAND -> model.rightArm;
			case LEFT_FOOT -> model.leftLeg;
			case RIGHT_FOOT -> model.rightLeg;
		};
	}

	public static ModelPart getOverlay(PlayerModel<AbstractClientPlayer> model, SuctionCupLimb limb) {
		return switch (limb) {
			case LEFT_HAND -> model.leftSleeve;
			case RIGHT_HAND -> model.rightSleeve;
			case LEFT_FOOT -> model.leftPants;
			case RIGHT_FOOT -> model.rightPants;
		};
	}

	public record ModelState(ModelPart limbPart, boolean limbVisible, ModelPart overlay, boolean overlayVisible,
							 float x, float y, float z, float pitch, float yaw, float roll) {
		public void restore() {
			limbPart.setPos(x, y, z);
			limbPart.setRotation(pitch, yaw, roll);
			overlay.copyFrom(limbPart);
			limbPart.visible = limbVisible;
			overlay.visible = overlayVisible;
		}
	}
}
