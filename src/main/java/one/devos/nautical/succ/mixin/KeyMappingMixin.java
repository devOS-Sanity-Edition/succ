package one.devos.nautical.succ.mixin;

import com.mojang.blaze3d.platform.InputConstants.Key;

import net.minecraft.client.KeyMapping;

import one.devos.nautical.succ.SuccKeybinds;
import one.devos.nautical.succ.WrappedKeyMappingIterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {
	@ModifyVariable(method = "resetMapping", at = @At("STORE"))
	private static Iterator<KeyMapping> succ$keepClimbBindsOutOfMap(Iterator<KeyMapping> value) {
		return SuccKeybinds.keepClimbBindsOutOfMap(value);
	}

	@Inject(
			method = "click",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/KeyMapping;clickCount:I",
					shift = Shift.BEFORE,
					ordinal = 0
			),
			locals = LocalCapture.CAPTURE_FAILHARD,
			cancellable = true
	)
	private static void succ$onClick(Key key, CallbackInfo ci, KeyMapping keybind) {
		if (SuccKeybinds.onClick(keybind)) {
			ci.cancel();
		}
	}
}
