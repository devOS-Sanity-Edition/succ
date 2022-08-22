package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import one.devos.nautical.succ.GlobalClimbingManager;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow
	@Final
	protected ServerPlayer player;

	@Inject(method = "setGameModeForPlayer", at = @At("TAIL"))
	private void succ$spectatorsDontClimb(GameType gameMode, GameType previousGameMode, CallbackInfo ci) {
		if (gameMode == GameType.SPECTATOR && GlobalClimbingManager.isClimbing(player)) {
			GlobalClimbingManager.stopClimbing(player);
		}
	}
}
