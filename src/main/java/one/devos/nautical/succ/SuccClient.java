package one.devos.nautical.succ;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

import net.minecraft.client.Minecraft;
import one.devos.nautical.succ.model.DepressedSuctionCupModel;
import one.devos.nautical.succ.model.SuctionCupModel;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayConnectionEvents;

public class SuccClient implements ClientModInitializer {
	@Override
	public void onInitializeClient(ModContainer mod) {
		SuccKeybinds.init();
		GlobalClimbingManager.clientInit();

		EntityModelLayerRegistry.registerModelLayer(SuctionCupModel.LAYER, SuctionCupModel::getLayerDefinition);
		EntityModelLayerRegistry.registerModelLayer(DepressedSuctionCupModel.LAYER, DepressedSuctionCupModel::getLayerDefinition);

		ClientTickEvents.START.register(SuccClient::onClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register(LocalClimbingManager::onDisconnect);
	}

	private static void onClientTick(Minecraft mc) {
		SuccKeybinds.tick(mc);
		LocalClimbingManager.tick(mc);
	}
}
