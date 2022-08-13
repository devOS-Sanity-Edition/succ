package one.devos.nautical.succ;

import com.mojang.blaze3d.platform.InputConstants.Type;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

import one.devos.nautical.succ.mixin.KeyMappingAccessor;

import org.lwjgl.glfw.GLFW;

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

	// TEMPORARY - will be replaced by picking up all cups
	public static final KeyMapping STOP = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.succ.stopClimbing", GLFW.GLFW_KEY_SEMICOLON, "key.categories.succ"));

	public static void init() {
	}

	/**
	 * Since KeyMapping.click() gets the KeyMapping via hashmap, duplicates will be ignored.
	 * Climbing keys are defaulted to overlap with vanilla binds, so we need to update them manually.
	 */
	public static void fixStatuses(KeyMapping key) {
		for (KeyMapping climbingKey : CLIMBING_KEYS) {
			if (climbingKey.same(key)) {
				incrementClickCount(climbingKey);
			}
		}
	}

	public static void tick() {
		// while climbing, prevent normal uses of the cup keys
		if (ClimbingManager.isClimbing()) {
			for (KeyMapping climbingKey : CLIMBING_KEYS) {
				unpressMatching(climbingKey);
			}
		}

		while (STOP.consumeClick()) {
			ClimbingManager.stopClimbing();
		}
	}

	private static void unpressMatching(KeyMapping keyMapping) {
		for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
			if (keyMapping.same(key)) {
				setUnpressed(key);
			}
		}
	}

	private static void setUnpressed(KeyMapping key) {
		((KeyMappingAccessor) key).succ$clickCount(0);
		key.setDown(false);
	}

	private static void incrementClickCount(KeyMapping key) {
		KeyMappingAccessor access = (KeyMappingAccessor) key;
		access.succ$clickCount(access.succ$clickCount() + 1);
	}
}
