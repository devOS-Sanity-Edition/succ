package one.devos.nautical.succ;

import net.minecraft.world.phys.Vec3;

public enum SuctionCupLimb {
	LEFT_HAND(true, true),
	RIGHT_HAND(false, true),
	LEFT_FOOT(true, false),
	RIGHT_FOOT(false, false);

	public final Vec3 offset;

	SuctionCupLimb(boolean left, boolean hand) {
		double x = left ? -0.5 : 0.5;
		double y = hand ? -0.7 : -1.7;
		double z = -0.28;
		this.offset = new Vec3(x, y, z);
	}
}
