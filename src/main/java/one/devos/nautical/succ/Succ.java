package one.devos.nautical.succ;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.item.group.api.QuiltItemGroup;
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Succ implements ModInitializer {
	public static final String ID = "succ";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static TwisterChampionTrigger TWISTER_CHAMPION = CriteriaTriggers.register(new TwisterChampionTrigger());
	public static final QuiltItemGroup SUCC_ITEM_GROUP = QuiltItemGroup.builder(id("succ")).build();

	public static Item SUCTION_CUP = new SuctionCupItem(new QuiltItemSettings().equipmentSlot(EquipmentSlot.HEAD).group(SUCC_ITEM_GROUP));
	public static Item SUCTION_CUP_BOOTS = new Item(new QuiltItemSettings().equipmentSlot(EquipmentSlot.FEET).group(SUCC_ITEM_GROUP));
	@SuppressWarnings("deprecation") // entity constructor deprecated to prevent misuse, should only be used here
	public static EntityType<ClimbingSuctionCupEntity> SUCTION_CUP_ENTITY_TYPE = FabricEntityTypeBuilder
			.<ClimbingSuctionCupEntity>create(MobCategory.MISC, ClimbingSuctionCupEntity::new)
			.dimensions(EntityDimensions.fixed(0.5f, 0.5f))
			.disableSummon()
			.disableSaving()
			.fireImmune()
			.build();

	@Override
	public void onInitialize(ModContainer mod) {
		Registry.register(Registry.ITEM, id("suction_cup"), SUCTION_CUP);
		Registry.register(Registry.ITEM, id("suction_cup_boots"), SUCTION_CUP_BOOTS);

		Registry.register(Registry.ENTITY_TYPE, id("suction_cup"), SUCTION_CUP_ENTITY_TYPE);

		SUCC_ITEM_GROUP.setIcon(SUCTION_CUP);

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
