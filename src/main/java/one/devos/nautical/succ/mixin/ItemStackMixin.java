package one.devos.nautical.succ.mixin;

import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import one.devos.nautical.succ.Succ;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract boolean is(Item item);

	@Inject(method = "hasAdventureModePlaceTagForBlock", at = @At("HEAD"), cancellable = true)
	private void succ$allowAdventureClimbing(Registry<Block> blockRegistry, BlockInWorld pos, CallbackInfoReturnable<Boolean> cir) {
		if (is(Succ.SUCTION_CUP)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
