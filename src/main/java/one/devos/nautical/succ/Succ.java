package one.devos.nautical.succ;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Succ implements ModInitializer {
	public static final String ID = "succ";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static TwisterChampionTrigger TWISTER_CHAMPION = CriteriaTriggers.register(new TwisterChampionTrigger());

	public static Item SUCTION_CUP = new SuctionCupItem(new QuiltItemSettings().equipmentSlot(EquipmentSlot.HEAD).maxCount(2));
	public static Item SUCTION_CUP_BOOTS = new Item(new QuiltItemSettings().equipmentSlot(EquipmentSlot.FEET).maxCount(1));
	@SuppressWarnings("deprecation") // entity constructor deprecated to prevent misuse, should only be used here
	public static EntityType<ClimbingSuctionCupEntity> SUCTION_CUP_ENTITY_TYPE = FabricEntityTypeBuilder
			.<ClimbingSuctionCupEntity>create(MobCategory.MISC, ClimbingSuctionCupEntity::new)
			.dimensions(EntityDimensions.fixed(0.5f, 0.5f))
			.disableSummon()
			.disableSaving()
			.fireImmune()
			.build();

	public static final CreativeModeTab SUCC_ITEM_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(SUCTION_CUP))
			.displayItems((params, output) -> {
				output.accept(SUCTION_CUP);
				output.accept(SUCTION_CUP_BOOTS);
			})
			.title(Component.translatable("itemGroup.succ"))
			.build();

	@Override
	public void onInitialize(ModContainer mod) {
		Registry.register(BuiltInRegistries.ITEM, id("suction_cup"), SUCTION_CUP);
		Registry.register(BuiltInRegistries.ITEM, id("suction_cup_boots"), SUCTION_CUP_BOOTS);

		Registry.register(BuiltInRegistries.ENTITY_TYPE, id("suction_cup"), SUCTION_CUP_ENTITY_TYPE);

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("item_group"), SUCC_ITEM_GROUP);

		ServerPlayConnectionEvents.JOIN.register(GlobalClimbingManager::onPlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(GlobalClimbingManager::onPlayerLeave);
		ServerPlayerEvents.AFTER_RESPAWN.register(GlobalClimbingManager::onRespawn);
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(GlobalClimbingManager::onChangeWorld);
		ClimbingSuctionCupEntity.networkingInit();
		GlobalClimbingManager.networkingInit();
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}
}
