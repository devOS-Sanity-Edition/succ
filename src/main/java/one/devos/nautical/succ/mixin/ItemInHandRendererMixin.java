package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void succ$hideHandsWhenClimbing(float tickDelta, PoseStack matrices, BufferSource vertexConsumers, LocalPlayer player, int light, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null) {
			ci.cancel();
		}
	}
}
