package one.devos.nautical.succ;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.PlayerLookup;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks climbing state of all players and syncs it to everyone.
 */
public class GlobalClimbingManager {
	public static final ResourceLocation STATE_CHANGE_PACKET = Succ.id("state_change");
	public static final ResourceLocation REQUEST_STOP = Succ.id("request_stop");
	public static final double START_CLIMB_POS_OFFSET = 0.30;

	// we need a different manager on each side to prevent leaking state in singleplayer and lan
	// the environment is the logical side here, not the physical side
	private static final GlobalClimbingManager clientManager = new GlobalClimbingManager(EnvType.CLIENT);
	private static final GlobalClimbingManager serverManager = new GlobalClimbingManager(EnvType.SERVER);

	private final EnvType env;
	private final Map<UUID, ClimbingState> states = new HashMap<>();

	private GlobalClimbingManager(EnvType env) {
		this.env = env;
	}

	public static GlobalClimbingManager get(Player player) {
		return get(player.level);
	}

	public static GlobalClimbingManager get(Level level) {
		return get(level.isClientSide());
	}

	public static GlobalClimbingManager get(boolean client) {
		return client ? clientManager : serverManager;
	}

	public static void startClimbing(ServerPlayer player, Vec3 clickPos, Direction blockFace) {
		Direction playerFacing = blockFace.getOpposite();
		moveNewClimber(player, clickPos, playerFacing);
		changeClimbingState(player, clickPos, playerFacing, true);
	}

	public static boolean isClimbing(Player player) {
		ClimbingState state = getState(player);
		return state != null && state.isClimbing();
	}

	public static void stopClimbing(ServerPlayer player) {
		changeClimbingState(player, null, null, false);
	}

	public static ClimbingState getState(Player player) {
		return getState(player.getUUID(), get(player));
	}

	public static ClimbingState getState(UUID id, boolean client) {
		return getState(id, get(client));
	}

	private static ClimbingState getState(UUID id, GlobalClimbingManager manager) {
		return manager.states.get(id);
	}

	protected static void putState(ClimbingState state, boolean client) {
		putState(state.playerUuid, state, client);
	}

	protected static void putState(UUID id, ClimbingState state, boolean client) {
		putState(id, state, get(client));
	}

	protected static void putState(UUID id, ClimbingState state, GlobalClimbingManager manager) {
		if (state == null) {
			manager.states.remove(id);
		} else {
			manager.states.put(id, state);
		}
	}

	protected void reset() {
		states.clear();
	}

	private static void moveNewClimber(ServerPlayer player, Vec3 clickPos, Direction facing) {
		player.stopRiding();
		if (player.isFallFlying()) {
			player.stopFallFlying();
		}
		player.fallDistance = 0;
		Vec3i facingNormal = facing.getNormal();
		Vec3 offset = new Vec3(facingNormal.getX(), facingNormal.getY(), facingNormal.getZ());
		offset = offset.scale(START_CLIMB_POS_OFFSET);
		Vec3 climberPos = clickPos.subtract(offset);
		player.connection.teleport(climberPos.x, climberPos.y - player.getEyeHeight(), climberPos.z, facing.toYRot(), 0);
	}

	private static void changeClimbingState(ServerPlayer player, @Nullable Vec3 clickPos, @Nullable Direction facing, boolean climbing) {
		ClimbingState state = getState(player);
		state.facing = facing;
		if (climbing && clickPos != null && facing != null) {
			addCupEntities(player, state, clickPos, facing);
		} else {
			removeCupEntities(player, state);
		}
		sendUpdatedStateToAll(state, player.server, false);
	}

	private static void addCupEntities(ServerPlayer player, ClimbingState state, Vec3 clickPos, Direction facing) {
		Level level = player.level;
		if (level == null) {
			return;
		}
		for (SuctionCupLimb limb : SuctionCupLimb.values()) {
			Vec3 cupPos = clickPos.add(SuccUtils.rotateVec(limb.cupOffset, facing.toYRot()));
			ClimbingSuctionCupEntity entity = new ClimbingSuctionCupEntity(level, limb, cupPos, state, facing);
			level.addFreshEntity(entity);
			state.entities.put(limb, entity);
		}
	}

	private static void removeCupEntities(ServerPlayer player, ClimbingState state) {
		state.entities.forEach((limb, entity) -> entity.discard());
		state.entities.clear();
	}

	private static void sendUpdatedStateToAll(ClimbingState state, MinecraftServer server, boolean all) {
		ServerPlayNetworking.send(PlayerLookup.all(server), STATE_CHANGE_PACKET, all ? state.syncAllToNetwork() : state.syncClimbStatusToNetwork());
	}

	public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		ClimbingState newState = new ClimbingState(handler.player.getUUID());
		putState(newState, false);
		// send player to all
		sendUpdatedStateToAll(newState, server, true);
		// send all to player
		get(false).states.forEach((uuid, state) -> sender.sendPacket(STATE_CHANGE_PACKET, state.syncAllToNetwork()));
	}

	public static void onPlayerLeave(ServerGamePacketListenerImpl handler, MinecraftServer server) {
		ServerPlayer player = handler.player;
		stopClimbing(player);
		UUID id = player.getUUID();
		putState(id, null, false);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeEnum(StateChangeType.REMOVE);
		buf.writeUUID(id);
		ServerPlayNetworking.send(PlayerLookup.all(server), STATE_CHANGE_PACKET,  buf);
	}

	public static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		if (isClimbing(oldPlayer)) {
			stopClimbing(oldPlayer);
		}
	}

	public static void onChangeWorld(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
		if (isClimbing(player)) {
			stopClimbing(player);
		}
	}

	public static void onTeleport(ServerPlayer player) {
		if (isClimbing(player)) {
			stopClimbing(player);
		}
	}

	public static void stopRequested(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
									 FriendlyByteBuf buf, PacketSender responseSender) {
		BlockPos stopPos = buf.readBlockPos();
		Direction facing = buf.readEnum(Direction.class);
		server.execute(() -> {
			BlockPos playerPos = player.blockPosition();
			if (playerPos.distSqr(stopPos) > 9) {
				Succ.LOGGER.warn("Player {} tried to stop climbing too far from their current position: [{}] to [{}]",
						player.getGameProfile().getName(), playerPos, stopPos);
				return;
			}
			if (!(player.level instanceof ServerLevel level)) {
				return;
			}
			stopClimbing(player);
			BlockPos above = stopPos.above();
			boolean cramped = !level.getBlockState(above).getCollisionShape(level, above).isEmpty();
			if (cramped) {
				player.setPose(Pose.SWIMMING);
			}
			player.teleportTo(level, stopPos.getX() + 0.5, stopPos.getY(), stopPos.getZ() + 0.5, facing.toYRot(), 0);
		});
	}

	public static void networkingInit() {
		ServerPlayNetworking.registerGlobalReceiver(REQUEST_STOP, GlobalClimbingManager::stopRequested);
	}

	@Environment(EnvType.CLIENT)
	public static void clientNetworkingInit() {
		ClientPlayNetworking.registerGlobalReceiver(STATE_CHANGE_PACKET, GlobalClimbingManager::handleChangeReceived);
	}

	@Environment(EnvType.CLIENT)
	private static void handleChangeReceived(Minecraft mc, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		StateChangeType type = buf.readEnum(StateChangeType.class);
		type.handle(mc, buf);
	}
}
