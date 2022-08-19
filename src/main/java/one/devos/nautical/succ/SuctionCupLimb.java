package one.devos.nautical.succ;

import net.minecraft.world.phys.Vec3;

public enum SuctionCupLimb {
	LEFT_HAND(true, true),
	RIGHT_HAND(false, true),
	LEFT_FOOT(true, false),
	RIGHT_FOOT(false, false);

	public final Vec3 cupOffset; // the offset from the player of cup start positions
	public final Vec3 offsetFromPlayer; // the offset from the player to where it connects to the torso
	public final boolean left;
	public final boolean hand;

	SuctionCupLimb(boolean left, boolean hand) {
		this.left = left;
		this.hand = hand;

		double x = left ? 0.4 : -0.4;
		double y = hand ? -0.7 : -1.7;
		double z = 0.35;
		this.cupOffset = new Vec3(x, y, z);

		x = left ? 0.3 : -0.3;
		y = hand ? 1.35 : 0.75;
		z = 0.08;
		offsetFromPlayer = new Vec3(x, y, z);
	}
}
