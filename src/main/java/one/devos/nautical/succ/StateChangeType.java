package one.devos.nautical.succ;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public enum StateChangeType {
	ADD_OR_REPLACE {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			ClimbingState state = new ClimbingState(buf);
			mc.execute(() -> {
				GlobalClimbingManager.CLIMBING_STATES.put(state.playerUuid, state);
				LocalPlayer player = Minecraft.getInstance().player;
				if (state.climbing && player != null && player.getUUID().equals(state.playerUuid)) {
					LocalClimbingManager.INSTANCE = new LocalClimbingManager(mc);
				}
			});
		}
	},
	CLIMB_STATUS {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			UUID playerId = buf.readUUID();
			boolean climbing = buf.readBoolean();
			mc.execute(() -> {
				GlobalClimbingManager.CLIMBING_STATES.get(playerId).climbing = climbing;
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null && player.getUUID().equals(playerId)) {
					LocalClimbingManager.INSTANCE = climbing ? new LocalClimbingManager(mc) : null;
				}
			});
		}
	},
	REMOVE {
		@Override
		public void handle(Minecraft mc, FriendlyByteBuf buf) {
			UUID toRemove = buf.readUUID();
			mc.execute(() -> {
				GlobalClimbingManager.CLIMBING_STATES.remove(toRemove);
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null && player.getUUID().equals(toRemove)) {
					LocalClimbingManager.INSTANCE = null;
				}
			});
		}
	};

	public abstract void handle(Minecraft mc, FriendlyByteBuf buf);
}
