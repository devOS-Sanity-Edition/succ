package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(Entity.class) // note: this is a client-only mixin despite entity not being client only
public class EntityMixin {
	@Inject(method = "setXRot", at = @At("HEAD"), cancellable = true)
	private void succ$restrictPitchAngleWhileClimbing(float pitch, CallbackInfo ci) {
		LocalClimbingManager manager = LocalClimbingManager.INSTANCE;
		if (manager != null && (Object) this instanceof LocalPlayer player) {
			if (pitch < manager.minPitch || pitch > manager.maxPitch) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "setYRot", at = @At("HEAD"), cancellable = true)
	private void succ$restrictRotationAngleWhileClimbing(float yaw, CallbackInfo ci) {
		LocalClimbingManager manager = LocalClimbingManager.INSTANCE;
		if (manager != null && (Object) this instanceof LocalPlayer player) {
			if (yaw < manager.minYaw || yaw > manager.maxYaw) {
				ci.cancel();
			}
		}
	}
}
