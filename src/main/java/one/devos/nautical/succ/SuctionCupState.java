package one.devos.nautical.succ;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class SuctionCupState {
	public boolean suction = true;
	public SuctionCupMoveDirection moveDirection = SuctionCupMoveDirection.NONE;
	public Vec3 currentPos;
	public Direction wallFace;

	public SuctionCupState(Vec3 currentPos, Direction wallFace) {
		this.currentPos = currentPos;
		this.wallFace = wallFace;
	}
}
