package one.devos.nautical.succ.commands;

import static org.quiltmc.qsl.command.api.client.ClientCommandManager.literal;

import one.devos.nautical.succ.ClimbingState;
import one.devos.nautical.succ.GlobalClimbingManager;
import net.minecraft.network.chat.Component;

import org.quiltmc.qsl.command.api.client.ClientCommandRegistrationCallback;
import org.quiltmc.qsl.command.api.client.QuiltClientCommandSource;

public class SuccClientCommands {
	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx, env) -> dispatcher.register(
				literal("succ")
						.then(literal("climbinfo")
								.then(literal("client")
										.executes(context -> {
											QuiltClientCommandSource source = context.getSource();
											ClimbingState state = GlobalClimbingManager.getState(source.getPlayer());

											source.sendFeedback(Component.literal("climbing: " + state.isClimbing()));
											source.sendFeedback(Component.literal("entities: " + state.entities.size()));
											source.sendFeedback(Component.literal("facing: " + state.facing));
											return 1;
										})
								)
						)
		));
	}
}
