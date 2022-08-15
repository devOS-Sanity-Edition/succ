package one.devos.nautical.succ;

import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

public enum SuctionCupLimb {
	LEFT_HAND, RIGHT_HAND, LEFT_FOOT, RIGHT_FOOT;

	public static final Map<SuctionCupLimb, Vec3> INITIAL_POS_OFFSETS = Util.make(new IdentityHashMap<>(), map -> {
		map.put(SuctionCupLimb.LEFT_HAND, new Vec3(-0.5, -0.7, -0.28));
		map.put(SuctionCupLimb.RIGHT_HAND, new Vec3(0.5, -0.7, -0.28));
		map.put(SuctionCupLimb.LEFT_FOOT, new Vec3(-0.5, -1.7, -0.28));
		map.put(SuctionCupLimb.RIGHT_FOOT, new Vec3(0.5, -1.7, -0.28));
	});
}
