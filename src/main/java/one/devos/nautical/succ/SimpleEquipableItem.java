package one.devos.nautical.succ;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

/**
 * Workaround for QSL bug: <a href="https://github.com/QuiltMC/quilt-standard-libraries/issues/320">QSL#320</a>
 */
public class SimpleEquipableItem extends Item implements Equipable {
	private final EquipmentSlot slot;

	public SimpleEquipableItem(Properties settings, EquipmentSlot slot) {
		super(settings);
		this.slot = Objects.requireNonNull(slot);
	}

	@Override
	@NotNull
	public EquipmentSlot getEquipmentSlot() {
		return slot;
	}
}
