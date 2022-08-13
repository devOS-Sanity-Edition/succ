package one.devos.nautical.succ;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClimbingManager {
	private static boolean climbing = false;

	public static void startClimbing() {
		if (isClimbing())
			return;
		System.out.println("started climbing");
		climbing = true;
	}

	public static boolean isClimbing() {
		return climbing;
	}

	public static void stopClimbing() {
		System.out.println("stopped climbing");
		climbing = false;
	}
}
