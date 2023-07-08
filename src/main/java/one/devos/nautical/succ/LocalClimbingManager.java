package one.devos.nautical.succ;

import java.util.ArrayList;
import java.util.List;

import org.quiltmc.loader.api.minecraft.ClientOnly;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@ClientOnly
public class LocalClimbingManager {
	public static final Component TOO_FAR = Component.translatable("succ.tooFar");
	public static final Component CUP_OBSTRUCTED_BLOCK = Component.translatable("succ.cupObstructedBlock");
	public static final Component CUP_OBSTRUCTED_OTHER = Component.translatable("succ.cupObstructedOther");
	public static final Component CUP_OBSTRUCTED_BAD_WALL = Component.translatable("succ.cupObstructedBadWall"); // TODO
	public static final Component STOP_3 = Component.translatable("succ.stopClimbing", 3).withStyle(ChatFormatting.GOLD);
	public static final Component STOP_2 = Component.translatable("succ.stopClimbing", 2).withStyle(ChatFormatting.GOLD);
	public static final Component STOP_1 = Component.translatable("succ.stopClimbing", 1).withStyle(ChatFormatting.GOLD);
	public static final int DEFAULT_ROTATION_RANGE = 90;

	public final ClimbingState state;
	public final float minYaw, maxYaw, minPitch, maxPitch;

	public final List<SuctionCupLimb> liftedCups = new ArrayList<>();

	SuctionCupLimb movedCup = null;
	private int initialCooldown = 10;
	private int ticksTillStop = 0;
	SuctionCupMoveDirection lastInputDirection = null;
	private Component lastSentMessage = null;

	/**
	 * Non-null when climbing
	 */
	@Nullable
	public static LocalClimbingManager INSTANCE = null;

	public LocalClimbingManager(LocalPlayer player, ClimbingState state) {
		this.state = state;
		player.yBodyRot = player.getYRot();
		float playerYaw = player.getYRot();
		minYaw = playerYaw - (DEFAULT_ROTATION_RANGE / 2f);
		maxYaw = playerYaw + (DEFAULT_ROTATION_RANGE / 2f);
		float playerPitch = player.getXRot();
		minPitch = playerPitch - (DEFAULT_ROTATION_RANGE / 2f);
		maxPitch = playerPitch + (DEFAULT_ROTATION_RANGE / 2f);
	}

	public static void tick(Minecraft mc, ClientLevel level) {
		if (INSTANCE != null && INSTANCE.isValid()) {
			INSTANCE.tickClimbing(mc, level);
		}
	}

	// runs every frame
	public static void clientTick(Minecraft mc) {
		if (INSTANCE != null && INSTANCE.isValid()) {
			INSTANCE.frameTick(mc);
		}
	}

	public static void onDisconnect(ClientPacketListener handler, Minecraft client) {
		INSTANCE = null;
		GlobalClimbingManager.get(true).reset();
	}

	private void tickClimbing(Minecraft mc, ClientLevel level) {
		initialCooldown--;
		ticksTillStop--;
		handleSuctionStateChanges();
		moveSelectedCup(mc.player, mc.level, mc.options);
	}

	private void frameTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player != null) {
			handlePlayerMovement(player, mc.getFrameTime());
		}
	}

	private void moveSelectedCup(LocalPlayer player, ClientLevel level, Options options) {
		if (movedCup != null) {
			ClimbingSuctionCupEntity entity = state.entities.get(movedCup);
			SuctionCupMoveDirection direction = SuctionCupMoveDirection.findFromInputs(options);
			SuctionCupMoveDirection currentDirection = entity.getMoveDirection();
			if (currentDirection != direction) {
				Vec3 newPos = entity.getOffsetPos(direction);
				if (newCupPosCloseEnough(player, level, entity, newPos, direction)
						&& newCupPosNotObstructed(player, level, entity, newPos, direction)) {
					if (!tryStopClimbing(player, level, entity, newPos, direction)) {
						ticksTillStop = -1;
						entity.setMoveDirectionFromClient(direction);
					}

				}
			}
			if (direction == SuctionCupMoveDirection.NONE) {
				ticksTillStop = -1;
			}
			lastInputDirection = direction;
		}
	}

	// true if trying to stop
	private boolean tryStopClimbing(LocalPlayer player, ClientLevel level, ClimbingSuctionCupEntity entity, Vec3 newPos, SuctionCupMoveDirection direction) {
		BlockPos blockPos = BlockPos.containing(newPos);
		Direction facing = entity.facing;
		BlockPos wallPos = blockPos.relative(facing);
		BlockState wallState = level.getBlockState(wallPos);
		if (wallState.getCollisionShape(level, wallPos).isEmpty()) {
			if (ticksTillStop == 10) {
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeBlockPos(wallPos);
				buf.writeEnum(facing);
				ClientPlayNetworking.send(GlobalClimbingManager.REQUEST_STOP, buf);
			} else if (ticksTillStop < 0) {
				ticksTillStop = 80;
			}
			sendNotification(player, level, direction, getTimerComponent());
			return true;
		}
		return false;
	}

	private Component getTimerComponent() {
		if (ticksTillStop > 60) {
			return STOP_3;
		} else if (ticksTillStop > 40) {
			return STOP_2;
		} else {
			return STOP_1;
		}
	}

	private boolean newCupPosCloseEnough(LocalPlayer player, ClientLevel level, ClimbingSuctionCupEntity entity,
										 Vec3 newPos, SuctionCupMoveDirection direction) {
		double largestDistance = 0;
		for (SuctionCupLimb limb : SuctionCupLimb.VALUES) {
			ClimbingSuctionCupEntity otherEntity = state.entities.get(limb);
			if (entity != otherEntity) {
				largestDistance = Math.max(largestDistance, otherEntity.position().distanceTo(newPos));
			}
		}
		if (largestDistance >= 2) { // too far
			sendNotification(player, level, direction, TOO_FAR);
			return false;
		}
		return true;
	}

	private boolean newCupPosNotObstructed(LocalPlayer player, ClientLevel level, ClimbingSuctionCupEntity entity,
										   Vec3 newPos, SuctionCupMoveDirection direction) {
		BlockPos newBlockPos = BlockPos.containing(newPos);
		BlockState state = level.getBlockState(newBlockPos);
		if (!state.getCollisionShape(level, newBlockPos).isEmpty()) {
			sendNotification(player, level, direction, CUP_OBSTRUCTED_BLOCK);
			return false;
		}
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty()) {
			sendNotification(player, level, direction, SuctionCupItem.OBSTRUCTED_LIQUID);
			return false;
		}
		for (SuctionCupLimb limb : SuctionCupLimb.VALUES) {
			ClimbingSuctionCupEntity otherEntity = this.state.entities.get(limb);
			if (entity != otherEntity) {
				AABB otherBounds = otherEntity.getBoundingBox();
				AABB newBounds = entity.getDimensions(Pose.STANDING).makeBoundingBox(newPos);
				if (otherBounds.intersects(newBounds)) {
					sendNotification(player, level, direction, CUP_OBSTRUCTED_OTHER);
					return false;
				}
			}
		}
		return true;
	}

	private void sendNotification(LocalPlayer player, ClientLevel level, SuctionCupMoveDirection direction, Component message) {
		// if it hasn't changed, don't spam it    // new message, send it
		if (direction != lastInputDirection || lastSentMessage != message) {
			player.displayClientMessage(message, true);
			lastSentMessage = message;
			level.playLocalSound(player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON,
					SoundSource.PLAYERS, 1, 1, false);
		}
	}

	private void handleSuctionStateChanges() {
		// give a short cooldown after starting to climb
		if (initialCooldown > 0) {
			for (KeyMapping key : SuccKeybinds.CLIMBING_KEYS) {
				while (key.consumeClick()); // discard any clicks that happen here
			}
			return;
		}
		for (SuctionCupLimb limb : SuctionCupLimb.VALUES) {
			KeyMapping key = SuccKeybinds.LIMBS_TO_KEYS.get(limb);
			ClimbingSuctionCupEntity entity = state.entities.get(limb);
			if (key.consumeClick()) {
				boolean suction = entity.getSuction();
				entity.setSuctionFromClient(!suction);
			}
			while (key.consumeClick()); // get rid of extras
		}
	}

	public void entitySuctionUpdated(ClimbingSuctionCupEntity entity) {
		if (entity.getSuction()) { // placed down
			liftedCups.remove(entity.limb);
			movedCup = liftedCups.isEmpty() ? null : liftedCups.get(liftedCups.size() - 1);
			lastInputDirection = null;
		} else { // picked up
			movedCup = entity.limb;
			liftedCups.add(entity.limb);
		}
	}

	private void handlePlayerMovement(LocalPlayer player, float partialTicks) {
		player.setDeltaMovement(0, 0, 0);

		Direction facing = state.facing;
		if (facing == null)
			return;
		Vec3 oldPos = player.getPosition(partialTicks);
		Axis ignored = facing.getAxis();
		double totalX = 0;
		double totalY = 0;
		double totalZ = 0;
		for (SuctionCupLimb limb : SuctionCupLimb.VALUES) {
			ClimbingSuctionCupEntity cup = state.entities.get(limb);
			Vec3 toUse = cup.isBeingPlaced() ? cup.getPosition(partialTicks) : cup.getStuckPos();
			totalX += Mth.lerp(partialTicks, toUse.x, cup.xOld);
			totalY += Mth.lerp(partialTicks, toUse.y, cup.yOld);
			totalZ += Mth.lerp(partialTicks, toUse.z, cup.zOld);
		}
		double averageX = totalX / 4;
		double averageY = totalY / 4;
		double averageZ = totalZ / 4;
		switch (ignored) {
			case X -> averageX = oldPos.x;
			case Y -> averageY = oldPos.y;
			case Z -> averageZ = oldPos.z;
		}
		// move down a bit to align with the cups
		averageY -= 0.5;
		double finalX = Mth.lerp(partialTicks * 2, oldPos.x, averageX);
		double finalY = Mth.lerp(partialTicks * 2, oldPos.y, averageY);
		double finalZ = Mth.lerp(partialTicks * 2, oldPos.z, averageZ);
		player.setPos(finalX, finalY, finalZ);
	}

	public boolean isValid() {
		return state.entities.size() == 4;
	}
}
