package one.devos.nautical.succ_test;

import net.minecraft.resources.ResourceLocation;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuccTest implements ModInitializer {
	public static final String ID = "succ_test";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize(ModContainer mod) {
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}
}
