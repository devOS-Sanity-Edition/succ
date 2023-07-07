package one.devos.nautical.succ;

import com.mojang.math.Vector3f;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SuccUtils {
	// around Y axis
	public static Vec3 rotateVec(Vec3 vec, double degrees) {
		float rads = (float) Math.toRadians(degrees);
		float sin = Mth.sin(rads);
		float cos = Mth.cos(rads);
		double newX = vec.x * cos - vec.z * sin;
		double newZ = vec.x * sin + vec.z * cos;
		return new Vec3(newX, vec.y, newZ);
	}

	public static Vector3f rotateVec(Vector3f vec, double degrees) {
		float rads = (float) Math.toRadians(degrees);
		float sin = Mth.sin(rads);
		float cos = Mth.cos(rads);
		float newX = vec.x() * cos - vec.z() * sin;
		float newZ = vec.x() * sin + vec.z() * cos;
		vec.set(newX, vec.y(), newZ);
		return vec;
	}

	public static boolean isClose(double x, double y, double z, Vec3 target) {
		return isClose(x, target.x) && isClose(y, target.y) && isClose(z, target.z);
	}

	public static boolean isClose(double a, double b) {
		if (a > b) {
			return a - b < 0.05;
		} else {
			return b - a < 0.05;
		}
	}

	public static void writeVec(FriendlyByteBuf buf, Vec3 vec) {
		buf.writeDouble(vec.x);
		buf.writeDouble(vec.y);
		buf.writeDouble(vec.z);
	}

	public static Vec3 readVec(FriendlyByteBuf buf) {
		return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	public static double difference(double a, double b) {
		return a > b ? a - b : b - a;
	}

	public static double axisChoose(Axis axis, Vec3 vec) {
		return axis.choose(vec.x, vec.y, vec.z);
	}

	public static Vector3f floatNormal(Direction direction) {
		return switch (direction) {
			case DOWN -> Vector3f.YN;
			case UP -> Vector3f.YP;
			case NORTH -> Vector3f.ZN;
			case SOUTH -> Vector3f.ZP;
			case WEST -> Vector3f.XN;
			case EAST -> Vector3f.XP;
		};
	}
}
