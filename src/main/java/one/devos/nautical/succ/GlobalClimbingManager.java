package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks climbing state of all players and syncs it to everyone.
 */
public class GlobalClimbingManager {
	protected static final Map<UUID, ClimbingState> CLIMBING_STATES = new HashMap<>();
	public static final ResourceLocation STATE_CHANGE_PACKET = Succ.id("state_change");
	public static final double START_CLIMB_POS_OFFSET = 0.30;

	public static void startClimbing(ServerPlayer player, Vec3 clickPos, Direction blockFace) {
		Direction playerFacing = blockFace.getOpposite();
		moveNewClimber(player, clickPos, playerFacing);
		changeClimbingState(player, clickPos, playerFacing, true);
	}

	public static boolean isClimbing(Player player) {
		ClimbingState state = CLIMBING_STATES.get(player.getUUID());
		return state != null && state.climbing;
	}

	public static void stopClimbing(ServerPlayer player) {
		changeClimbingState(player, null, null, false);
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

	private static void changeClimbingState(ServerPlayer player, @Nullable Vec3 clickPos, @Nullable Direction facing, boolean climbing) {
		UUID uuid = player.getUUID();
		ClimbingState state = CLIMBING_STATES.get(uuid);
		state.climbing = climbing;
		if (climbing && clickPos != null && facing != null) {
			addCupEntities(player, state, clickPos, facing);
		} else {
			removeCupEntities(player, state);
		}
		sendUpdatedStateToAll(state, player.server);
	}

	private static void addCupEntities(ServerPlayer player, ClimbingState state, Vec3 clickPos, Direction facing) {
		Level level = player.level;
		if (level == null) {
			return;
		}
		SuctionCupLimb.INITIAL_POS_OFFSETS.forEach((limb, offset) -> {
			Vec3 cupPos = clickPos.add(offset);
			ClimbingSuctionCupEntity entity = new ClimbingSuctionCupEntity(level, limb, state, facing);
			entity.teleportTo(cupPos.x, cupPos.y, cupPos.z);
			level.addFreshEntity(entity);
			state.entities.put(limb, entity);
		});
	}

	private static void removeCupEntities(ServerPlayer player, ClimbingState state) {
		state.entities.forEach((limb, entity) -> entity.discard());
		state.entities.clear();
	}

	private static void sendUpdatedStateToAll(ClimbingState state, MinecraftServer server) {
		ServerPlayNetworking.send(PlayerLookup.all(server), STATE_CHANGE_PACKET, state.syncClimbStatusToNetwork());
	}

	public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		ClimbingState newState = new ClimbingState(handler.player.getUUID());
		CLIMBING_STATES.put(newState.playerUuid, newState);
		CLIMBING_STATES.forEach((uuid, state) -> sender.sendPacket(STATE_CHANGE_PACKET, state.syncAllToNetwork()));
	}

	public static void onPlayerLeave(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		CLIMBING_STATES.remove(handler.player.getUUID());
	}

	@Environment(EnvType.CLIENT)
	public static void clientInit() {
		ClientPlayNetworking.registerGlobalReceiver(STATE_CHANGE_PACKET, GlobalClimbingManager::handleChangeReceived);
	}

	@Environment(EnvType.CLIENT)
	private static void handleChangeReceived(Minecraft mc, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		StateChangeType type = buf.readEnum(StateChangeType.class);
		type.handle(mc, buf);
	}
}
