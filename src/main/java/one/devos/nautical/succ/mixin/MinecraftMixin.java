package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void succ$noInvWhileClimbing(Screen screen, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null && screen instanceof InventoryScreen) {
			ci.cancel();
		}
	}
}
