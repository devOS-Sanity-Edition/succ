package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.UUID;

public class ClimbingSuctionCupEntity extends Entity {
	public static final ResourceLocation SPAWN_PACKET = Succ.id("climbing_suction_cup_spawn");
	public static final ResourceLocation UPDATE_SUCTION = Succ.id("update_suction");

	public SuctionCupLimb limb;
	public ClimbingState climbingState;
	public boolean suction = true;
	public Direction facing;
	public SuctionCupMoveDirection moveDirection = SuctionCupMoveDirection.NONE;

	/**
	 * @deprecated ONLY FOR CLIENT SYNC
	 */
	@Deprecated
	public ClimbingSuctionCupEntity(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	public ClimbingSuctionCupEntity(Level level, SuctionCupLimb limb, ClimbingState state, Direction facing) {
		super(Succ.SUCTION_CUP_ENTITY_TYPE, level);
		this.limb = limb;
		this.climbingState = state;
		this.facing = facing;
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		new ClientboundAddEntityPacket(this).write(buf);
		writeExtraPacketData(buf);
		return ServerPlayNetworking.createS2CPacket(SPAWN_PACKET, buf);
	}

	public void writeExtraPacketData(FriendlyByteBuf buf) {
		buf.writeEnum(limb);
		buf.writeUUID(climbingState.playerUuid);
	}

	public void readExtraPacketData(FriendlyByteBuf buf) {
		limb = buf.readEnum(SuctionCupLimb.class);
		UUID playerId = buf.readUUID();
		climbingState = GlobalClimbingManager.CLIMBING_STATES.get(playerId);
		climbingState.entities.put(limb, this);
	}

	public static void networkingInit() {
		ServerPlayNetworking.registerGlobalReceiver(UPDATE_SUCTION, ClimbingSuctionCupEntity::handleSuctionUpdate);
	}

	public static void handleSuctionUpdate(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
		boolean newSuction = buf.readBoolean();
		SuctionCupLimb limb = buf.readEnum(SuctionCupLimb.class);
		server.execute(() -> {
			ClimbingState state = GlobalClimbingManager.CLIMBING_STATES.get(player.getUUID());
			if (!state.climbing) {
				Succ.LOGGER.warn("Suction cup update from non-climbing player: [{}], [{}]", limb, player.getGameProfile().getName());
				return;
			}
			ClimbingSuctionCupEntity entity = state.entities.get(limb);
			entity.suction = newSuction;
			// check if we should let go
			for (ClimbingSuctionCupEntity cupEntity : state.entities.values()) {
				if (cupEntity.suction) {
					return;
				}
			}
			// none have suction
			GlobalClimbingManager.stopClimbing(player);
		});
	}


	@Environment(EnvType.CLIENT)
	public static void clientNetworkingInit() {
		ClientPlayNetworking.registerGlobalReceiver(SPAWN_PACKET, ClimbingSuctionCupEntity::handleSpawnOnClient);
	}

	@Environment(EnvType.CLIENT)
	public static void handleSpawnOnClient(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) {
		ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(buf);
		buf.retain(); // save until after extra data is read
		client.execute(() -> {
			addPacket.handle(handler);
			ClimbingSuctionCupEntity entity = (ClimbingSuctionCupEntity) client.level.getEntity(addPacket.getId());
			entity.readExtraPacketData(buf);
			buf.release();
		});
	}

	@Environment(EnvType.CLIENT)
	public void setSuctionFromClient(boolean suction) {
		this.suction = suction;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(suction);
		buf.writeEnum(limb);
		ClientPlayNetworking.send(UPDATE_SUCTION, buf);
	}
}
