package one.devos.nautical.succ;

import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;

public enum SuctionCupLimb {
	LEFT_HAND(true, true, PlayerModelPart.LEFT_SLEEVE),
	RIGHT_HAND(false, true, PlayerModelPart.RIGHT_SLEEVE),
	LEFT_FOOT(true, false, PlayerModelPart.LEFT_PANTS_LEG),
	RIGHT_FOOT(false, false, PlayerModelPart.RIGHT_PANTS_LEG);

	public static final SuctionCupLimb[] VALUES = values();

	public final Vec3 cupOffset; // the offset from the player of cup start positions
	public final Vec3 offsetFromPlayer; // the offset from the player to where it connects to the torso
	public final boolean left;
	public final boolean hand;
	public final PlayerModelPart playerPart;

	SuctionCupLimb(boolean left, boolean hand, PlayerModelPart playerPart) {
		this.left = left;
		this.hand = hand;
		this.playerPart = playerPart;

		double x = left ? 0.4 : -0.4;
		double y = hand ? -0.7 : -1.7;
		double z = 0.35;
		this.cupOffset = new Vec3(x, y, z);

		x = left ? 0.3 : -0.3;
		if (!hand) {
			x *= 0.75;
		}
		y = hand ? 1.35 : 0.75;
		z = 0.1;
		offsetFromPlayer = new Vec3(x, y, z);
	}
}
