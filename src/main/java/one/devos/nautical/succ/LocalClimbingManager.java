package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.core.Direction.Axis;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LocalClimbingManager {
	public static final Component TOO_FAR = Component.translatable("succ.tooFar");
	public static final Component INFLEXIBLE = Component.translatable("succ.cupObstructedBlock");
	public static final Component CUP_OBSTRUCTED_BLOCK = Component.translatable("succ.cupObstructedBlock");
	public static final Component CUP_OBSTRUCTED_OTHER = Component.translatable("succ.cupObstructedOther");
	public static final int DEFAULT_ROTATION_RANGE = 90;

	public final ClimbingState state;
	public final float minYaw, maxYaw, minPitch, maxPitch;
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> cups = new ArrayList<>();
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> liftedCups = new LinkedList<>();

	private Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> movedCup = null;
	private int initialCooldown = 10;
	private SuctionCupMoveDirection lastInputDirection = null;

	/**
	 * Non-null when climbing
	 */
	@Nullable
	public static LocalClimbingManager INSTANCE = null;

	public LocalClimbingManager(Minecraft mc) {
		this.state = GlobalClimbingManager.getState(mc.player);
		this.state.entities.forEach((limb, entity) -> {
			KeyMapping key = SuccKeybinds.LIMBS_TO_KEYS.get(limb);
			cups.add(Triple.of(key, limb, entity));
		});
		LocalPlayer player = mc.player;
		float playerYaw = player.getYRot();
		minYaw = playerYaw - (DEFAULT_ROTATION_RANGE / 2f);
		maxYaw = playerYaw + (DEFAULT_ROTATION_RANGE / 2f);
		float playerPitch = player.getXRot();
		minPitch = playerPitch - (DEFAULT_ROTATION_RANGE / 2f);
		maxPitch = playerPitch + (DEFAULT_ROTATION_RANGE / 2f);
	}

	public static void tick(Minecraft mc, ClientLevel level) {
		if (INSTANCE != null) {
			INSTANCE.tickClimbing(mc, level);
		}
	}

	// runs every frame
	public static void clientTick(Minecraft mc) {
		if (INSTANCE != null) {
			INSTANCE.frameTick(mc);
		}
	}

	public static void onDisconnect(ClientPacketListener handler, Minecraft client) {
		INSTANCE = null;
		GlobalClimbingManager.get(true).reset();
	}

	private void tickClimbing(Minecraft mc, ClientLevel level) {
		initialCooldown--;
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
			ClimbingSuctionCupEntity entity = movedCup.getRight();
			SuctionCupMoveDirection direction = SuctionCupMoveDirection.findFromInputs(options);
			SuctionCupMoveDirection currentDirection = entity.getMoveDirection();
			if (currentDirection != direction) {
				Vec3 newPos = entity.getOffsetPos(direction);
				if (newCupPosCloseEnough(player, level, entity, newPos, direction)
						&& newCupPosNotObstructed(player, level, entity, newPos, direction)) {
					entity.setMoveDirectionFromClient(direction);
				}
			}
			lastInputDirection = direction;
		}
	}

	private boolean newCupPosCloseEnough(LocalPlayer player, ClientLevel level, ClimbingSuctionCupEntity entity,
										 Vec3 newPos, SuctionCupMoveDirection direction) {
		double largestDistance = 0;
		for (Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> cup : cups) {
			ClimbingSuctionCupEntity otherEntity = cup.getRight();
			if (entity != otherEntity) {
				largestDistance = Math.max(largestDistance, otherEntity.position().distanceTo(newPos));
			}
		}
		if (largestDistance >= 2) { // too far
			failMove(player, level, direction, TOO_FAR);
			return false;
		}
		return true;
	}

	private boolean newCupPosNotObstructed(LocalPlayer player, ClientLevel level, ClimbingSuctionCupEntity entity,
										   Vec3 newPos, SuctionCupMoveDirection direction) {
		BlockPos lowCorner = new BlockPos(newPos);
		BlockPos highCorner = new BlockPos(Math.ceil(newPos.x), Math.ceil(newPos.y), Math.ceil(newPos.z));
		for (BlockPos pos : BlockPos.betweenClosed(lowCorner, highCorner)) {
			BlockState state = level.getBlockState(pos);
			if (!state.getCollisionShape(level, pos).isEmpty()) {
				failMove(player, level, direction, CUP_OBSTRUCTED_BLOCK);
				return false;
			}
		}
		for (Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> cup : cups) {
			ClimbingSuctionCupEntity otherEntity = cup.getRight();
			if (entity != otherEntity) {
				AABB otherBounds = otherEntity.getBoundingBox();
				AABB newBounds = entity.getDimensions(Pose.STANDING).makeBoundingBox(newPos);
				if (otherBounds.intersects(newBounds)) {
					failMove(player, level, direction, CUP_OBSTRUCTED_OTHER);
					return false;
				}
			}
		}
		return true;
	}

	private void failMove(LocalPlayer player, ClientLevel level, SuctionCupMoveDirection direction, Component message) {
		if (direction != lastInputDirection) { // if it hasn't changed, don't spam it
			player.displayClientMessage(message, true);
			level.playLocalSound(player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON,
					SoundSource.PLAYERS, 1, 1, false);
		}
	}

	private void handleSuctionStateChanges() {
		// give a short cooldown after starting to climb
		if (initialCooldown > 0) {
			cups.forEach(triple -> {
				KeyMapping key = triple.getLeft();
				while (key.consumeClick()); // discard any clicks that happen here
			});
			return;
		}
		cups.forEach(triple -> {
			KeyMapping key = triple.getLeft();
			ClimbingSuctionCupEntity entity = triple.getRight();
			if (key.consumeClick()) {
				boolean suction = entity.getSuction();
				if (suction) { // was on the wall, now will not be
					movedCup = triple;
					liftedCups.add(triple);
					entity.setSuctionFromClient(false);
				} else { // placed onto the wall
					liftedCups.remove(triple);
					movedCup = liftedCups.isEmpty() ? null : liftedCups.get(liftedCups.size() - 1);
					lastInputDirection = null;
					entity.setSuctionFromClient(true);
				}
			}
			while (key.consumeClick()); // get rid of extras
		});
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
		for (Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> triple : cups) {
			ClimbingSuctionCupEntity cup = triple.getRight();
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
}
