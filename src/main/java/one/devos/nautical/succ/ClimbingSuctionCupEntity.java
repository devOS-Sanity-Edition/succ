package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClimbingSuctionCupEntity extends Entity {
	public static final ResourceLocation SPAWN_PACKET = Succ.id("climbing_suction_cup_spawn");
	public static final ResourceLocation UPDATE_SUCTION = Succ.id("update_suction");

	public static final EntityDataAccessor<Boolean> SUCTION = SynchedEntityData.defineId(ClimbingSuctionCupEntity.class, EntityDataSerializers.BOOLEAN);

	public static final double OFFSET_DISTANCE = -0.25;
	public static final Map<Direction, Vec3> OFFSETS = Util.make(new HashMap<>(), map -> {
		for (Direction direction : Plane.HORIZONTAL) {
			Vec3i normal = direction.getNormal();
			Vec3 offset = new Vec3(normal.getX() * OFFSET_DISTANCE, 0, normal.getZ() * OFFSET_DISTANCE);
			map.put(direction, offset);
		}
	});

	public Direction facing;
	public SuctionCupMoveDirection moveDirection = SuctionCupMoveDirection.NONE;

	// should never be null and should never change
	public /* final */ SuctionCupLimb limb;
	public /* final */ ClimbingState climbingState;

	private boolean suction = true;
	private Vec3 stuckPos;
	private Vec3 unstuckPos;
//	private Map<SuctionCupMoveDirection, Vec3> movedPositions = new HashMap<>();
	private int ageAtMoveStart = -1;
	private Vec3 movementTarget = null;

	/**
	 * @deprecated ONLY FOR CLIENT SYNC
	 */
	@Deprecated
	public ClimbingSuctionCupEntity(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	public ClimbingSuctionCupEntity(Level level, SuctionCupLimb limb, Vec3 wallPos, ClimbingState state, Direction facing) {
		super(Succ.SUCTION_CUP_ENTITY_TYPE, level);
		this.limb = limb;
		this.climbingState = state;
		this.facing = facing;
		this.stuckPos = wallPos;
		setPos(stuckPos);
		Vec3 offset = OFFSETS.get(facing);
		this.unstuckPos = stuckPos.add(offset);
	}

	@Override
	public void tick() {
		super.tick();
		if (movementTarget != null && ageAtMoveStart != -1) {
			Vec3 pos = position();
			double delta = (tickCount - ageAtMoveStart) / 5f;
			double deltaX = Mth.lerp(delta, pos.x, movementTarget.x);
			double deltaY = Mth.lerp(delta, pos.y, movementTarget.y);
			double deltaZ = Mth.lerp(delta, pos.z, movementTarget.z);
			setPos(deltaX, deltaY, deltaZ);
			if (isClose(deltaX, deltaY, deltaZ, movementTarget)) {
				movementTarget = null;
				ageAtMoveStart = -1;
			}
		}
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(SUCTION, Boolean.TRUE);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (SUCTION.equals(data)) {
			boolean oldSuction = this.suction;
			this.suction = entityData.get(SUCTION);
			if (oldSuction == suction)
				return;
			movementTarget = suction ? stuckPos : unstuckPos;
			ageAtMoveStart = tickCount;
		}
	}

	public boolean getSuction() {
		return suction;
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
		buf.writeDouble(stuckPos.x);
		buf.writeDouble(stuckPos.y);
		buf.writeDouble(stuckPos.z);
		buf.writeDouble(unstuckPos.x);
		buf.writeDouble(unstuckPos.y);
		buf.writeDouble(unstuckPos.z);
	}

	public void readExtraPacketData(FriendlyByteBuf buf) {
		limb = buf.readEnum(SuctionCupLimb.class);
		UUID playerId = buf.readUUID();
		climbingState = GlobalClimbingManager.getState(playerId, level.isClientSide());
		climbingState.entities.put(limb, this);
		stuckPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		unstuckPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	public static void networkingInit() {
		ServerPlayNetworking.registerGlobalReceiver(UPDATE_SUCTION, ClimbingSuctionCupEntity::handleSuctionUpdate);
	}

	public static void handleSuctionUpdate(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
		boolean newSuction = buf.readBoolean();
		SuctionCupLimb limb = buf.readEnum(SuctionCupLimb.class);
		server.execute(() -> {
			ClimbingState state = GlobalClimbingManager.getState(player);
			if (!state.climbing) {
				Succ.LOGGER.warn("Suction cup update from non-climbing player: [{}], [{}]", limb, player.getGameProfile().getName());
				return;
			}
			ClimbingSuctionCupEntity entity = state.entities.get(limb);
			entity.entityData.set(SUCTION, newSuction);
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
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(suction);
		buf.writeEnum(limb);
		ClientPlayNetworking.send(UPDATE_SUCTION, buf);
	}

	public static boolean isClose(double x, double y, double z, Vec3 target) {
		return isClose(x, target.x) && isClose(y, target.y) && isClose(z, target.z);
	}

	public static boolean isClose(double a, double b) {
		if (a > b) {
			return a - b < 0.01;
		} else {
			return b - a < 0.01;
		}
	}
}
