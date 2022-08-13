package one.devos.nautical.succ;

import net.minecraft.resources.ResourceLocation;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Succ implements ModInitializer {
	public static final String ID = "succ";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("hello from " + this.getClass().getName());
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(ID, path);
	}
}
