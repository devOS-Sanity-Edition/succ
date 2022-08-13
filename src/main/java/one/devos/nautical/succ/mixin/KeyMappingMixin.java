package one.devos.nautical.succ.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;

import net.minecraft.client.KeyMapping;

import one.devos.nautical.succ.SuccKeybinds;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {
	@Inject(method = "click", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private static void succ$onClick(Key key, CallbackInfo ci, KeyMapping keybind) {
		if (keybind != null) {
			SuccKeybinds.fixStatuses(keybind);
		}
	}
}
