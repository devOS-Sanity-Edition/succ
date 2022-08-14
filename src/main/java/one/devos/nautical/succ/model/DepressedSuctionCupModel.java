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
import net.minecraft.world.entity.Entity;
import one.devos.nautical.succ.Succ;

@Environment(EnvType.CLIENT)
public class DepressedSuctionCupModel extends EntityModel<Entity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Succ.id("depressed_suction_cup"), "main");

	private final ModelPart depressed_suction_cup;

	public DepressedSuctionCupModel(Context ctx) {
		ModelPart root = ctx.bakeLayer(LAYER);
		this.depressed_suction_cup = root.getChild("depressed_suction_cup");
	}

	public static LayerDefinition getLayerDefinition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition part = mesh.getRoot();
		part.addOrReplaceChild(
				"depressed_suction_cup",
				CubeListBuilder.create()
						.texOffs(0, 38).addBox(-5.0875F, 1.6792F, -5.0958F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
						.texOffs(0, 26).addBox(-4.5875F, -1.3208F, -4.6208F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
						.texOffs(27, 0).addBox(-2.6375F, -3.3958F, -0.9958F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0125F, 21.3208F, -0.0292F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
		depressed_suction_cup.render(matrices, vertices, light, overlay, red, green, blue, alpha);
	}
}
