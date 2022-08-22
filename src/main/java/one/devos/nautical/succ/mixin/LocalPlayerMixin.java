package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void succ$preventDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		if (LocalClimbingManager.INSTANCE != null) {
			cir.setReturnValue(false);
		}
	}
}
