package one.devos.nautical.succ;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.world.phys.Vec3;

public enum SuctionCupMoveDirection {
	UP_LEFT(-1, 1), UP(0, 1), UP_RIGHT(1, 1),
	LEFT(-1, 0), NONE(0, 0), RIGHT(1, 0),
	DOWN_LEFT(-1, -1), DOWN(0, -1), DOWN_RIGHT(1, -1);

	public static final double OFFSET_PER_UNIT = 0.3;

	public final int xOff, yOff;
	public final Vec3 offset;

	/**
	 * Map of X values to maps of Y values to directions
	 */
	private static Int2ObjectMap<Int2ObjectMap<SuctionCupMoveDirection>> map = null;

	SuctionCupMoveDirection(int xOff, int yOff) {
		this.xOff = xOff;
		this.yOff = yOff;
		this.offset = new Vec3(xOff * -OFFSET_PER_UNIT, yOff * OFFSET_PER_UNIT, 0);
	}

	private static void fillMap() {
		map = new Int2ObjectOpenHashMap<>();
		for (SuctionCupMoveDirection moveDirection : values()) {
			map.computeIfAbsent(moveDirection.xOff, (i) -> new Int2ObjectOpenHashMap<>(3)).put(moveDirection.yOff, moveDirection);
		}
	}

	public static SuctionCupMoveDirection findFromGrid(int xOff, int yOff) {
		if (map == null)
			fillMap();
		return map.get(xOff).get(yOff);
	}

	@Environment(EnvType.CLIENT)
	public static SuctionCupMoveDirection findFromInputs(Options options) {
		int yOff = 0;
		int xOff = 0;

		if (options.keyUp.isDown()) {
			yOff++;
		}
		if (options.keyDown.isDown()) {
			yOff--;
		}
		if (options.keyLeft.isDown()) {
			xOff--;
		}
		if (options.keyRight.isDown()) {
			xOff++;
		}

		return findFromGrid(xOff, yOff);
	}
}
