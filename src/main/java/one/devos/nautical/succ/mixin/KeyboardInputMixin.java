package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import one.devos.nautical.succ.LocalClimbingManager;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void succ$dontMoveWhenClimbing(boolean slowDown, float f, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null) {
			up = false;
			right = false;
			down = false;
			left = false;
			forwardImpulse = 0;
			leftImpulse = 0;
		}
	}
}
