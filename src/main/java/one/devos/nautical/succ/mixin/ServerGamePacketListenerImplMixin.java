package one.devos.nautical.succ.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import one.devos.nautical.succ.GlobalClimbingManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Shadow
	private int aboveGroundTickCount;

	@Inject(method = "tick", at = @At("HEAD"))
	private void succ$climbingIsNotFlying(CallbackInfo ci) {
		if (GlobalClimbingManager.isClimbing(player)) {
			aboveGroundTickCount = 0;
		}
	}
}
