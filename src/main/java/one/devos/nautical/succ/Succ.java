package one.devos.nautical.succ;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Succ implements ModInitializer {
	public static final String ID = "succ";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static Item SUCTION_CUP = new SuctionCupItem(new QuiltItemSettings().equipmentSlot(EquipmentSlot.HEAD));
	public static Item SUCTION_CUP_BOOTS = new Item(new QuiltItemSettings().equipmentSlot(EquipmentSlot.FEET));

	@Override
	public void onInitialize(ModContainer mod) {
		Registry.register(Registry.ITEM, id("suction_cup"), SUCTION_CUP);
		Registry.register(Registry.ITEM, id("suction_cup_boots"), SUCTION_CUP_BOOTS);

		ServerPlayConnectionEvents.JOIN.register(GlobalClimbingManager::sendDataToJoiningPlayer);
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}
}
