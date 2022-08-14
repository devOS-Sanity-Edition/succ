package one.devos.nautical.succ.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import one.devos.nautical.succ.Succ;

@Environment(EnvType.CLIENT)
public class SuctionCupModel extends EntityModel<Player> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Succ.id("suction_cup"), "main");
	public static final ResourceLocation TEXTURE = Succ.id("textures/suction_cup.png");

	private final ModelPart bone;

	public SuctionCupModel(Context ctx) {
		ModelPart root = ctx.bakeLayer(LAYER);
		this.bone = root.getChild("bone");
	}

	public static LayerDefinition getLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild(
				"bone",
				CubeListBuilder.create()
						.texOffs(0, 14).addBox(-13.075F, -2.0F, 2.875F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
						.texOffs(0, 0).addBox(-12.575F, -7.0F, 3.35F, 9.0F, 5.0F, 9.0F, new CubeDeformation(0.0F))
						.texOffs(27, 0).addBox(-10.625F, -9.075F, 6.975F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(Player player, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
		bone.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
