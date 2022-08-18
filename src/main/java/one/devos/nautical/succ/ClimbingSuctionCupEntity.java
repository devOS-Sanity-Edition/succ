package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ClimbingSuctionCupEntity extends Entity {
	public static final ResourceLocation SPAWN_PACKET = Succ.id("climbing_suction_cup_spawn");
	public static final ResourceLocation UPDATE_SUCTION = Succ.id("update_suction");
	public static final ResourceLocation UPDATE_DIRECTION = Succ.id("update_direction");

	public static final EntityDataAccessor<Boolean> SUCTION = SynchedEntityData.defineId(ClimbingSuctionCupEntity.class, EntityDataSerializers.BOOLEAN);
	// not a typical int
	// a boolean is stored 16 bits shifted left
	public static final EntityDataAccessor<Integer> DIRECTION_ORDINAL = SynchedEntityData.defineId(ClimbingSuctionCupEntity.class, EntityDataSerializers.INT);
	public static final int SKIP_MASK = 	0b10000000000000000;
	public static final int ORDINAL_MASK = 	0b01111111111111111;

	public static final double OFFSET_DISTANCE = -0.25;
	public static final Map<Direction, Vec3> OFFSETS = Util.make(new HashMap<>(), map -> {
		for (Direction direction : Plane.HORIZONTAL) {
			Vec3i normal = direction.getNormal();
			Vec3 offset = new Vec3(normal.getX() * OFFSET_DISTANCE, 0, normal.getZ() * OFFSET_DISTANCE);
			map.put(direction, offset);
		}
	});

	// should never be null and should never change
	public /* final */ SuctionCupLimb limb;
	public /* final */ ClimbingState climbingState;
	public /* final */ Direction facing;

	private boolean suction = true;
	private SuctionCupMoveDirection moveDirection = SuctionCupMoveDirection.NONE;
	private Vec3 stuckPos;
	private Vec3 unstuckPos;
	private Vec3 handlePos;
	private Vec3 lastHandlePos;
	private int ageAtMoveStart = -1;
	private Vec3 movementTarget = null;
	private boolean beingPlaced = false;

	private Player owner = null;

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
		setYRot(facing.toYRot());
		this.stuckPos = wallPos;
		Vec3 offset = OFFSETS.get(facing);
		this.unstuckPos = stuckPos.add(offset);
		setPos(unstuckPos);
		setMoveTarget(stuckPos);
	}

	@Override
	public void setPos(double x, double y, double z) {
		super.setPos(x, y, z);
		lastHandlePos = handlePos;
		handlePos = new Vec3(x, y + 0.25, z);
	}

	@Override
	public void tick() {
		super.tick();
		if (isMoving()) {
			Vec3 pos = position();
			double delta = (tickCount - ageAtMoveStart) / 5f;
			double deltaX = Mth.lerp(delta, pos.x, movementTarget.x);
			double deltaY = Mth.lerp(delta, pos.y, movementTarget.y);
			double deltaZ = Mth.lerp(delta, pos.z, movementTarget.z);
			setPos(deltaX, deltaY, deltaZ);
			if (SuccUtils.isClose(deltaX, deltaY, deltaZ, movementTarget)) {
				beingPlaced = false;
				movementTarget = null;
				ageAtMoveStart = -1;
				checkTwisterChampionStatus();
			}
		}
		if (owner != null && owner.isRemoved()) {
			owner = null;
		}
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(SUCTION, Boolean.TRUE);
		entityData.define(DIRECTION_ORDINAL, SuctionCupMoveDirection.NONE.ordinal());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
		super.onSyncedDataUpdated(data);
		if (SUCTION.equals(data)) {
			updateSuction();
		} else if (DIRECTION_ORDINAL.equals(data)) {
			updateMoveDirection();
		}
	}

	private void updateSuction() {
		boolean oldSuction = this.suction;
		this.suction = entityData.get(SUCTION);
		if (oldSuction == suction)
			return;
		level.playSound(null, blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1, suction ? 0.5f : 1);
		if (moveDirection == SuctionCupMoveDirection.NONE) {
			setMoveTarget(suction ? stuckPos : unstuckPos);
		} else { // placing at a new pos
			unstuckPos = unstuckPos.add(moveDirection.offset);
			Vec3 offset = OFFSETS.get(facing);
			stuckPos = unstuckPos.subtract(offset);
			setMoveTarget(stuckPos);
			beingPlaced = true;
			if (!level.isClientSide()) {
				moveDirection = SuctionCupMoveDirection.NONE;
				int data = moveDirection.ordinal();
				data = data | SKIP_MASK; // updating should be skipped here
				entityData.set(DIRECTION_ORDINAL, data);
			}
		}
	}

	private void setMoveTarget(Vec3 target) {
		this.movementTarget = target;
		this.ageAtMoveStart = tickCount;
	}

	private void updateMoveDirection() {
		int data = entityData.get(DIRECTION_ORDINAL);
		int ordinal = data & ORDINAL_MASK;
		SuctionCupMoveDirection oldDirection = this.moveDirection;
		if (ordinal == oldDirection.ordinal())
			return;
		SuctionCupMoveDirection[] directions = SuctionCupMoveDirection.values();
		if (ordinal >= directions.length) {
			Succ.LOGGER.warn("Invalid movement direction ordinal received: [{}], [{}], [{}]",
					ordinal, limb, getOwner().getGameProfile().getName());
			return;
		}
		this.moveDirection = directions[ordinal];
		boolean skipPosUpdate = (data & SKIP_MASK) == SKIP_MASK;
		if (skipPosUpdate)
			return;
		Vec3 newPos = getOffsetPos(moveDirection);
		setMoveTarget(newPos);
	}

	public Vec3 getOffsetPos(SuctionCupMoveDirection direction) {
		Vec3 offset = SuccUtils.rotateVec(direction.offset, facing.toYRot());
		return unstuckPos.add(offset);
	}

	public boolean isMoving() {
		return movementTarget != null && ageAtMoveStart != -1;
	}

	public boolean isBeingPlaced() {
		return beingPlaced;
	}

	public Vec3 getStuckPos() {
		return stuckPos;
	}

	public boolean getSuction() {
		return suction;
	}

	public SuctionCupMoveDirection getMoveDirection() {
		return moveDirection;
	}

	public Vec3 getHandlePos() {
		return handlePos;
	}

	public Vec3 getHandlePos(float tickDelta) {
		double x = Mth.lerp(tickDelta, this.lastHandlePos.x, this.handlePos.x);
		double y = Mth.lerp(tickDelta, this.lastHandlePos.y, this.handlePos.y);
		double z = Mth.lerp(tickDelta, this.lastHandlePos.z, this.handlePos.z);
		return new Vec3(x, y, z);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		// not saved
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		// not saved
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
		buf.writeEnum(facing);
		SuccUtils.writeVec(buf, stuckPos);
		SuccUtils.writeVec(buf, unstuckPos);
		boolean moving = isMoving();
		buf.writeBoolean(moving);
		if (moving) {
			SuccUtils.writeVec(buf, movementTarget);
			buf.writeVarInt(ageAtMoveStart);
		}
	}

	public void readExtraPacketData(FriendlyByteBuf buf) {
		this.limb = buf.readEnum(SuctionCupLimb.class);
		UUID playerId = buf.readUUID();
		this.climbingState = GlobalClimbingManager.getState(playerId, level.isClientSide());
		this.climbingState.entities.put(limb, this);
		this.facing = buf.readEnum(Direction.class);
		setYRot(facing.toYRot());
		this.stuckPos = SuccUtils.readVec(buf);
		this.unstuckPos = SuccUtils.readVec(buf);
		boolean moving = buf.readBoolean();
		if (moving) {
			this.movementTarget = SuccUtils.readVec(buf);
			this.ageAtMoveStart = buf.readVarInt();
		}
	}

	public Player getOwner() {
		if (owner == null) {
			owner = level.getPlayerByUUID(climbingState.playerUuid);
		}
		return owner;
	}

	public void checkTwisterChampionStatus() {
		Player owner = getOwner();
		if (!(owner instanceof ServerPlayer player)) {
			return;
		}
		int flips = 0;
		// check every cup against every other
		for (Entry<SuctionCupLimb, ClimbingSuctionCupEntity> entry : climbingState.entities.entrySet()) {
			SuctionCupLimb limb = entry.getKey();
			ClimbingSuctionCupEntity entity = entry.getValue();
			Direction right = entity.facing.getClockWise();
			Axis axis = right.getAxis();
			AxisDirection direction = right.getAxisDirection();
			Vec3 pos = entity.position();
			double posOnAxis = SuccUtils.axisChoose(axis, pos);

			for (Entry<SuctionCupLimb, ClimbingSuctionCupEntity> entry2 : climbingState.entities.entrySet()) {
				SuctionCupLimb otherLimb = entry2.getKey();
				ClimbingSuctionCupEntity otherEntity = entry2.getValue();
				Vec3 otherPos = otherEntity.position();
				double otherPosOnAxis = SuccUtils.axisChoose(axis, otherPos);
				if (otherLimb.left != limb.left) { // opposite sides
					if (otherLimb.hand != limb.hand) {
						// complete opposites
						continue;
					}
					boolean correct = direction == AxisDirection.POSITIVE
							? limb.left ? posOnAxis <= otherPosOnAxis : posOnAxis >= otherPosOnAxis
							: limb.left ? posOnAxis >= otherPosOnAxis : posOnAxis <= otherPosOnAxis;
					if (!correct) {
						flips++;
					}
				} else if (otherLimb.hand != limb.hand) { // same side - check limb
					boolean shouldBeAbove = limb.hand;
					boolean correctOrder = shouldBeAbove
							? otherPos.y <= pos.y
							: otherPos.y >= pos.y;
					if (!correctOrder) {
						flips++;
					}
				} // else - both match, found itself, do nothing
			}
		}
		if (flips > 4) {
			Succ.TWISTER_CHAMPION.trigger(player);
		}
	}

	public static void networkingInit() {
		ServerPlayNetworking.registerGlobalReceiver(UPDATE_SUCTION, ClimbingSuctionCupEntity::handleSuctionUpdate);
		ServerPlayNetworking.registerGlobalReceiver(UPDATE_DIRECTION, ClimbingSuctionCupEntity::handleMoveDirectionUpdate);
	}

	public static void handleSuctionUpdate(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
		boolean newSuction = buf.readBoolean();
		SuctionCupLimb limb = buf.readEnum(SuctionCupLimb.class);
		executeOnServerWithCup(server, player, limb, (state, entity) -> {
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

	public static void handleMoveDirectionUpdate(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
		SuctionCupMoveDirection direction = buf.readEnum(SuctionCupMoveDirection.class);
		SuctionCupLimb limb = buf.readEnum(SuctionCupLimb.class);
		executeOnServerWithCup(server, player, limb,
				(state, entity) -> entity.entityData.set(DIRECTION_ORDINAL, direction.ordinal()));
	}

	private static void executeOnServerWithCup(MinecraftServer server, ServerPlayer player, SuctionCupLimb limb, BiConsumer<ClimbingState, ClimbingSuctionCupEntity> consumer) {
		server.execute(() -> {
			ClimbingState state = GlobalClimbingManager.getState(player);
			if (!state.isClimbing()) {
				Succ.LOGGER.warn("Suction cup update from non-climbing player: [{}], [{}]", limb, player.getGameProfile().getName());
				return;
			}
			ClimbingSuctionCupEntity entity = state.entities.get(limb);
			consumer.accept(state, entity);
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

	@Environment(EnvType.CLIENT)
	public void setMoveDirectionFromClient(SuctionCupMoveDirection direction) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeEnum(direction);
		buf.writeEnum(limb);
		ClientPlayNetworking.send(UPDATE_DIRECTION, buf);
	}
}
