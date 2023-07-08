package one.devos.nautical.succ;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;

public enum StateChangeType {
	ADD_OR_REPLACE {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			ClimbingState state = new ClimbingState(buf);
			mc.execute(() -> {
				GlobalClimbingManager.putState(state, true);
				LocalPlayer player = Minecraft.getInstance().player;
				if (state.isClimbing() && player != null && player.getUUID().equals(state.playerUuid)) {
					LocalClimbingManager.INSTANCE = new LocalClimbingManager(player, state);
				}
			});
		}
	},
	CLIMB_STATUS {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			UUID playerId = buf.readUUID();
			int ordinal = buf.readVarInt();
			Direction facing = ordinal == -1 ? null : Direction.values()[ordinal];
			mc.execute(() -> {
				ClimbingState state = GlobalClimbingManager.getState(playerId, true);
				state.facing = facing;
				LocalPlayer player = mc.player;
				if (player != null && player.getUUID().equals(playerId)) {
					LocalClimbingManager.INSTANCE = state.isClimbing() ? new LocalClimbingManager(player, state) : null;
				}
			});
		}
	},
	REMOVE {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			UUID toRemove = buf.readUUID();
			mc.execute(() -> {
				GlobalClimbingManager.putState(toRemove, null, true);
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null && player.getUUID().equals(toRemove)) {
					LocalClimbingManager.INSTANCE = null;
				}
			});
		}
	};

	public abstract void handle(Minecraft mc, FriendlyByteBuf buf);
}
