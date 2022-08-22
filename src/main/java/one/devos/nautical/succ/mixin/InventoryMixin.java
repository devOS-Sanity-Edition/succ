package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Inventory;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(Inventory.class) // note: this is a client-only mixin despite entity not being client only
public class InventoryMixin {
	@Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
	private void succ$preventItemSwapping(double scrollAmount, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null) {
			ci.cancel();
		}
	}
}
