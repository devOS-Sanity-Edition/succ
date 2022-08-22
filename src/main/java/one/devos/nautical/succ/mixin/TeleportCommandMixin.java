package one.devos.nautical.succ.mixin;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import one.devos.nautical.succ.GlobalClimbingManager;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
	@Inject(method = "performTeleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setYHeadRot(F)V", ordinal = 0))
	private static void succ$onTeleport(CommandSourceStack source, Entity target, ServerLevel world, double x, double y,
										double z, Set<RelativeArgument> movementFlags, float yaw, float pitch,
										@Nullable @Coerce Object facingLocation, CallbackInfo ci) {
		if (target instanceof ServerPlayer player) {
			GlobalClimbingManager.onTeleport(player);
		}
	}
}
