package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import one.devos.nautical.succ.GlobalClimbingManager;

@Mixin(PlayerItemInHandLayer.class)
public class PlayerItemInHandLayerMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void succ$hideHeldItemsWhenClimbing(LivingEntity entity, ItemStack stack, ItemDisplayContext transformationMode, HumanoidArm arm,
												PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
		if (entity instanceof Player player && GlobalClimbingManager.isClimbing(player)) {
			ci.cancel();
		}
	}
}
