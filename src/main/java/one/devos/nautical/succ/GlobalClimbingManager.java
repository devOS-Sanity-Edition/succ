package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.Vec3;

import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks climbing state of all players and syncs it to everyone.
 */
public class GlobalClimbingManager {
	private static final Map<UUID, ClimbingState> CLIMBING_STATES = new HashMap<>();
	public static final ResourceLocation STATE_CHANGE_PACKET = Succ.id("state_change");
	public static final double START_CLIMB_POS_OFFSET = 0.30;

	public static void startClimbing(ServerPlayer player, Vec3 clickPos, Direction blockFace) {
		moveNewClimber(player, clickPos, blockFace.getOpposite());
		changeClimbingState(player, true);
	}

	public static boolean isClimbing(Player player) {
		ClimbingState state = CLIMBING_STATES.get(player.getUUID());
		return state != null && state.climbing;
	}

	public static void stopClimbing(ServerPlayer player) {
		changeClimbingState(player, false);
	}

	public static void reset() {
		CLIMBING_STATES.clear();
	}

	private static void moveNewClimber(ServerPlayer player, Vec3 clickPos, Direction facing) {
		player.stopRiding();
		Vec3i facingNormal = facing.getNormal();
		Vec3 offset = new Vec3(facingNormal.getX(), facingNormal.getY(), facingNormal.getZ());
		double posOffset = START_CLIMB_POS_OFFSET;
		if (facing.getAxisDirection() == AxisDirection.POSITIVE) {
			posOffset *= 3.7; // weird magic number to make positioning match, I don't understand why it's different
		}
		offset = offset.scale(posOffset);
		Vec3 climberPos = clickPos.subtract(offset);
		player.connection.teleport(climberPos.x, climberPos.y - player.getEyeHeight(), climberPos.z, facing.toYRot(), 0);
	}

	private static void changeClimbingState(ServerPlayer player, boolean climbing) {
		UUID uuid = player.getUUID();
		ClimbingState state = getOrCreate(uuid);
		state.climbing = climbing;
		sendUpdatedStateToAll(state, player.server);
	}

	private static ClimbingState getOrCreate(UUID uuid) {
		ClimbingState state = CLIMBING_STATES.get(uuid);
		if (state == null) {
			state = new ClimbingState(uuid);
			CLIMBING_STATES.put(uuid, state);
		}
		return state;
	}

	private static void sendUpdatedStateToAll(ClimbingState state, MinecraftServer server) {
		ServerPlayNetworking.send(PlayerLookup.all(server), STATE_CHANGE_PACKET, state.toNetwork());
	}

	public static void sendDataToJoiningPlayer(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		CLIMBING_STATES.forEach((uuid, state) -> sender.sendPacket(STATE_CHANGE_PACKET, state.toNetwork()));
	}

	@Environment(EnvType.CLIENT)
	public static void clientInit() {
		ClientPlayNetworking.registerGlobalReceiver(STATE_CHANGE_PACKET, GlobalClimbingManager::handleChangeReceived);
	}

	@Environment(EnvType.CLIENT)
	private static void handleChangeReceived(Minecraft mc, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ClimbingState state = new ClimbingState(buf);
		mc.execute(() -> {
			CLIMBING_STATES.put(state.playerUuid, state);
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && player.getUUID().equals(state.playerUuid)) {
				LocalClimbingManager.INSTANCE = state.climbing ? new LocalClimbingManager(mc) : null;
			}
		});
	}
}
