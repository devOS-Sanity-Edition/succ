package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class SuctionCupItem extends Item {
	public static final EquipmentSlot[] CUP_SLOTS = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET };
	public static final TagKey<Item> CLIMBING_CUPS = TagKey.create(Registry.ITEM_REGISTRY, Succ.id("climbing_cups"));

	public SuctionCupItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null && context.getLevel().isClientSide() && Succ.ENV == EnvType.CLIENT) {
			// only try on client side. manager is client only and will send a packet.
			tryStartClimbing(player);
		}
		return InteractionResult.PASS;
	}

	public void tryStartClimbing(Player player) {
		if (ClimbingManager.isClimbing()) {
			return;
		}
		for (EquipmentSlot slot : CUP_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!stack.is(CLIMBING_CUPS))
				return;
		}
		// all 4 cups equipped
		ClimbingManager.startClimbing();
	}
}
