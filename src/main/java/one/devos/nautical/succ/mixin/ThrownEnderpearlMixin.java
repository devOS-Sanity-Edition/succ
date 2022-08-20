package one.devos.nautical.succ.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import one.devos.nautical.succ.GlobalClimbingManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin extends ThrowableItemProjectile {
	public ThrownEnderpearlMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;resetFallDistance()V"))
	private void succ$onTeleport(HitResult hitResult, CallbackInfo ci) {
		if (getOwner() instanceof ServerPlayer player) {
			GlobalClimbingManager.onTeleport(player);
		}
	}
}
