package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Environment(EnvType.CLIENT)
public class LocalClimbingManager {
	public static final int DEFAULT_ROTATION_RANGE = 90;

	public final Stack<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> unsuckedCups = new Stack<>();
	public final ClimbingState state;
	public final List<Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity>> cups = new ArrayList<>();
	public final float minYaw, maxYaw, minPitch, maxPitch;

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

	public static void onDisconnect(ClientPacketListener handler, Minecraft client) {
		INSTANCE = null;
		GlobalClimbingManager.get(true).reset();
	}

	private void tickClimbing(Minecraft mc, ClientLevel level) {
		initialCooldown--;
		handleSuctionStateChanges();
		moveSelectedCup(mc.options);
		makePlayerStickToWall(mc.player);
	}

	private void moveSelectedCup(Options options) {
		if (unsuckedCups.empty()) {
			return;
		}
		Triple<KeyMapping, SuctionCupLimb, ClimbingSuctionCupEntity> cup = unsuckedCups.peek();
		ClimbingSuctionCupEntity entity = cup.getRight();
		SuctionCupMoveDirection direction = SuctionCupMoveDirection.findFromInputs(options);
		SuctionCupMoveDirection currentDirection = entity.getMoveDirection();
		if (currentDirection != direction) {
			entity.setMoveDirectionFromClient(direction);
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
					unsuckedCups.push(triple);
					entity.setSuctionFromClient(false);
				} else { // placed onto the wall
					unsuckedCups.pop();
					entity.setSuctionFromClient(true);
				}
			}
			while (key.consumeClick()); // get rid of extras
		});
	}

	private void makePlayerStickToWall(LocalPlayer player) {
		player.setDeltaMovement(0, 0, 0);
	}
}
