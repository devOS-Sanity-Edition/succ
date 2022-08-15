package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.core.Direction;

import net.minecraft.core.Direction.Axis;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class LocalClimbingManager {
	public static final int DEFAULT_ROTATION_RANGE = 90;

	public final ClimbingState state;
	public final float minYaw, maxYaw, minPitch, maxPitch;
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> cups = new ArrayList<>();
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> liftedCups = new LinkedList<>();

	private Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> movedCup = null;
	private int initialCooldown = 10;

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
		moveSelectedCup(mc.options);
	}

	private void frameTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player != null) {
			handlePlayerMovement(player);
		}
	}

	private void moveSelectedCup(Options options) {
		if (movedCup != null) {
			ClimbingSuctionCupEntity entity = movedCup.getRight();
			SuctionCupMoveDirection direction = SuctionCupMoveDirection.findFromInputs(options);
			SuctionCupMoveDirection currentDirection = entity.getMoveDirection();
			if (currentDirection != direction) {
				entity.setMoveDirectionFromClient(direction);
			}
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
					entity.setSuctionFromClient(true);
				}
			}
			while (key.consumeClick()); // get rid of extras
		});
	}

	private void handlePlayerMovement(LocalPlayer player) {
		player.setDeltaMovement(0, 0, 0);

		Direction facing = state.facing;
		if (facing == null)
			return;
		Vec3 oldPos = player.position();
		Axis ignored = facing.getAxis();
		double totalX = 0;
		double totalY = 0;
		double totalZ = 0;
		for (Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> triple : cups) {
			ClimbingSuctionCupEntity cup = triple.getRight();
			Vec3 toUse = cup.isBeingPlaced() ? cup.position() : cup.getStuckPos();
			totalX += toUse.x;
			totalY += toUse.y;
			totalZ += toUse.z;
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
		double finalX = Mth.lerp(0.2, oldPos.x, averageX);
		double finalY = Mth.lerp(0.2, oldPos.y, averageY);
		double finalZ = Mth.lerp(0.2, oldPos.z, averageZ);
		player.setPosRaw(finalX, finalY, finalZ);
	}
}
