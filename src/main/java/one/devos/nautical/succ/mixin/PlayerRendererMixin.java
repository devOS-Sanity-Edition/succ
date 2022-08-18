package one.devos.nautical.succ.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import one.devos.nautical.succ.LocalClimbingManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public PlayerRendererMixin(Context context, PlayerModel<AbstractClientPlayer> entityModel, float f) {
		super(context, entityModel, f);
	}

	@Inject(method = "setModelProperties", at = @At("TAIL"))
	private void succ$hideLimbsWhenClimbing(AbstractClientPlayer player, CallbackInfo ci) {
		if (LocalClimbingManager.INSTANCE != null) {
			PlayerModel<AbstractClientPlayer> model = this.getModel();
			model.leftArm.visible = false;
			model.leftSleeve.visible = false;

			model.rightArm.visible = false;
			model.rightSleeve.visible = false;

			model.leftLeg.visible = false;
			model.leftPants.visible = false;

			model.rightLeg.visible = false;
			model.rightPants.visible = false;
		}
	}
}
