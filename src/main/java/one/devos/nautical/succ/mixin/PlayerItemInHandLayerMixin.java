package one.devos.nautical.succ.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import one.devos.nautical.succ.LocalClimbingManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void succ$hideHeldItemsWhenClimbing(LivingEntity entity, ItemStack stack, TransformType transformationMode,
												HumanoidArm arm, PoseStack matrices, MultiBufferSource vertexConsumers,
												int light, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null) {
			ci.cancel();
		}
	}
}
