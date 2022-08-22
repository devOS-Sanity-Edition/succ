package one.devos.nautical.succ;

import com.mojang.blaze3d.platform.InputConstants.Type;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;

import one.devos.nautical.succ.mixin.KeyMappingAccessor;

import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class SuccKeybinds {
	public static final KeyMapping LEFT_HAND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.succ.leftHand", Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT, "key.categories.succ"));
	public static final KeyMapping RIGHT_HAND = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.succ.rightHand", Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT, "key.categories.succ"));
	public static final KeyMapping LEFT_FOOT = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.succ.leftFoot", GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.succ"));
	public static final KeyMapping RIGHT_FOOT = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.succ.rightFoot", GLFW.GLFW_KEY_SPACE, "key.categories.succ"));
	public static final KeyMapping[] CLIMBING_KEYS = { LEFT_HAND, RIGHT_HAND, LEFT_FOOT, RIGHT_FOOT };

	public static final Map<SuctionCupLimb, KeyMapping> LIMBS_TO_KEYS = Util.make(new IdentityHashMap<>(), map -> {
		map.put(SuctionCupLimb.LEFT_HAND, SuccKeybinds.LEFT_HAND);
		map.put(SuctionCupLimb.RIGHT_HAND, SuccKeybinds.RIGHT_HAND);
		map.put(SuctionCupLimb.LEFT_FOOT, SuccKeybinds.LEFT_FOOT);
		map.put(SuctionCupLimb.RIGHT_FOOT, SuccKeybinds.RIGHT_FOOT);
	});

	public static Iterator<KeyMapping> keepClimbBindsOutOfMap(Iterator<KeyMapping> original) {
		return new WrappedKeyMappingIterator(original);
	}

	public static boolean onClick(KeyMapping keybind) {
		if (LocalClimbingManager.INSTANCE != null) {
			for (KeyMapping climbKey : SuccKeybinds.CLIMBING_KEYS) {
				if (climbKey.same(keybind)) {
					keybind.setDown(false);
					incrementClickCount(climbKey);
					return true;
				}
			}
		}
		return false;
	}

	private static void incrementClickCount(KeyMapping key) {
		KeyMappingAccessor access = (KeyMappingAccessor) key;
		access.succ$clickCount(access.succ$clickCount() + 1);
	}

	public static void init() {
	}
}
