package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@Environment(EnvType.CLIENT)
public class LocalClimbingManager {
	public static final int DEFAULT_ROTATION_RANGE = 90;

	public final Stack<SuctionCupLimb> unsuckedCups = new Stack<>();
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> cups;
	public final Map<SuctionCupLimb, ClimbingSuctionCupEntity> limbToEntity;
	public float minYaw, maxYaw, minPitch, maxPitch;

	/**
	 * Non-null when climbing
	 */
	@Nullable
	public static LocalClimbingManager INSTANCE = null;

	public LocalClimbingManager(Minecraft mc) {
		cups = new ArrayList<>();
		limbToEntity = new HashMap<>();
		LocalPlayer player = mc.player;
		Direction facing = Direction.fromYRot(player.getYRot()); // player will be facing at the wall
		Vec3 pos = player.position();
//		SuctionCupLimb[] limbs = SuctionCupLimb.values();
//		KeyMapping[] keys = SuccKeybinds.CLIMBING_KEYS;
//		for (int i = 0; i < 4; i++) {
//			SuctionCupLimb limb = limbs[i];
//			KeyMapping key = keys[i];
//			Vec3 offset = SuctionCupLimb.INITIAL_POS_OFFSETS.get(limb);
//			Vec3 cupPos = pos.add(offset);
//			SuctionCupState state = new SuctionCupState(cupPos, facing);
//			cups.add(Triple.of(key, limb, state));
//			limbToState.put(limb, state);
//		}

		float playerYaw = player.getYRot();
		minYaw = playerYaw - (DEFAULT_ROTATION_RANGE / 2f);
		maxYaw = playerYaw + (DEFAULT_ROTATION_RANGE / 2f);
		float playerPitch = player.getXRot();
		minPitch = playerPitch - (DEFAULT_ROTATION_RANGE / 2f);
		maxPitch = playerPitch + (DEFAULT_ROTATION_RANGE / 2f);
	}

	public static void tick(Minecraft mc) {
		if (INSTANCE != null) {
			INSTANCE.tickClimbing(mc);
		}
	}

	public static void onDisconnect(ClientPacketListener handler, Minecraft client) {
		INSTANCE = null;
		GlobalClimbingManager.reset();
	}

	private void tickClimbing(Minecraft mc) {
		handleSuctionStateChanges();
		moveCups(mc.options);
		makePlayerStickToWall(mc.player);
	}

	private void moveCups(Options options) {
		if (unsuckedCups.empty()) {
			return;
		}
		SuctionCupLimb moving = unsuckedCups.peek();
		ClimbingSuctionCupEntity state = limbToEntity.get(moving);
		SuctionCupMoveDirection lastMoveDirection = state.moveDirection;
		state.moveDirection = SuctionCupMoveDirection.findFromInputs(options);
	}

	private void handleSuctionStateChanges() {
		cups.forEach((triple) -> {
			KeyMapping key = triple.getLeft();
			ClimbingSuctionCupEntity entity = triple.getRight();
			SuctionCupLimb limb = triple.getMiddle();
			if (key.consumeClick()) {
				boolean suction = entity.suction;
				if (suction) { // was on the wall, now will not be
					unsuckedCups.push(limb);
					entity.suction = false;
				} else { // placed onto the wall
					unsuckedCups.pop();
					entity.suction = true;
				}
			}
			while (key.consumeClick()); // get rid of extras
		});
	}

	private void makePlayerStickToWall(LocalPlayer player) {
		player.setDeltaMovement(0, 0, 0);
	}
}
