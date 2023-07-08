package one.devos.nautical.succ.commands;

import static net.minecraft.commands.Commands.literal;

import one.devos.nautical.succ.ClimbingState;
import one.devos.nautical.succ.GlobalClimbingManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

public class SuccCommands {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, ctx, env) -> dispatcher.register(
				literal("succ")
						.then(literal("climbinfo")
								.then(literal("server")
										.executes(context -> {
											CommandSourceStack source = context.getSource();
											ClimbingState state = GlobalClimbingManager.getState(source.getPlayerOrException());

											source.sendSystemMessage(Component.literal("climbing: " + state.isClimbing()));
											source.sendSystemMessage(Component.literal("entities: " + state.entities.size()));
											source.sendSystemMessage(Component.literal("facing: " + state.facing));
											return 1;
										})
								)
						)
		));
	}
}
