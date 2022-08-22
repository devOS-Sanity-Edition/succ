package one.devos.nautical.succ.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	void succ$clickCount(int clickCount);

	@Accessor("clickCount")
	int succ$clickCount();
}
