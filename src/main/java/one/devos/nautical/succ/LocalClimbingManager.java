package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;

@Environment(EnvType.CLIENT)
public class LocalClimbingManager {
	public static final int DEFAULT_ROTATION_RANGE = 100;
	public static final Map<KeyMapping, Vec3> OFFSETS = Util.make(new IdentityHashMap<>(), map -> {
		map.put(SuccKeybinds.LEFT_HAND, new Vec3(-0.5, 1, 0));
		map.put(SuccKeybinds.RIGHT_HAND, new Vec3(0.5, 1, 0));
		map.put(SuccKeybinds.LEFT_FOOT, new Vec3(-0.5, 0.1, 0));
		map.put(SuccKeybinds.RIGHT_FOOT, new Vec3(0.5, 0.1, 0));
	});

	private final Stack<KeyMapping> unsuckedCups = new Stack<>();
	private final Map<KeyMapping, SuctionCupState> cups;
	public float minYaw, maxYaw, minPitch, maxPitch;

	/**
	 * Non-null when climbing
	 */
	@Nullable
	public static LocalClimbingManager INSTANCE = null;

	public LocalClimbingManager(Minecraft mc) {
		cups = new HashMap<>();
		LocalPlayer player = mc.player;
		Direction facing = Direction.fromYRot(player.getYRot()); // player will be facing at the wall
		Vec3 pos = player.position();
		for (KeyMapping key : SuccKeybinds.CLIMBING_KEYS) {
			Vec3 offset = OFFSETS.get(key);
			Vec3 cupPos = pos.add(offset);
			cups.put(key, new SuctionCupState(cupPos, facing));
		}

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
		KeyMapping moving = unsuckedCups.peek();
		SuctionCupState state = cups.get(moving);
		SuctionCupMoveDirection lastMoveDirection = state.moveDirection;
		state.moveDirection = SuctionCupMoveDirection.findFromInputs(options);
	}

	private void handleSuctionStateChanges() {
		cups.forEach((key, state) -> {
			if (key.consumeClick()) {
				boolean suction = state.suction;
				if (suction) { // was on the wall, now will not be
					unsuckedCups.push(key);
					state.suction = false;
				} else { // placed onto the wall
					unsuckedCups.pop();
					state.suction = true;
				}
			}
			while (key.consumeClick()); // get rid of extras
		});
	}

	private void makePlayerStickToWall(LocalPlayer player) {
		player.setDeltaMovement(0, 0, 0);
	}
}
