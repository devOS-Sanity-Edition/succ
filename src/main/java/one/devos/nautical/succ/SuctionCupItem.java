package one.devos.nautical.succ;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SuctionCupItem extends Item {
	public static final EquipmentSlot[] CUP_SLOTS = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET };
	public static final TagKey<Item> HAND_CLIMBING_CUPS = TagKey.create(Registry.ITEM_REGISTRY, Succ.id("hand_climbing_cups"));
	public static final TagKey<Item> FEET_CLIMBING_CUPS = TagKey.create(Registry.ITEM_REGISTRY, Succ.id("feet_climbing_cups"));
	public static final Component TOO_FAR = Component.translatable("succ.wallTooFar");
	public static final Component MISSING_CUPS = Component.translatable("succ.missingCups");
	public static final Component OBSTRUCTED = Component.translatable("succ.obstructed");
	public static final Component ONLY_WALLS = Component.translatable("succ.onlyWalls");
	public static final Component OBSTRUCTED_LIQUID = Component.translatable("succ.cupObstructedLiquid");
	public static final Component NOT_ENOUGH_WALL = Component.translatable("succ.notEnoughWall");

	public SuctionCupItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player instanceof ServerPlayer serverPlayer) {
			tryStartClimbing(serverPlayer, context);
		}
		return InteractionResult.FAIL;
	}

	public void tryStartClimbing(ServerPlayer player, UseOnContext ctx) {
		if (GlobalClimbingManager.isClimbing(player)) {
			return;
		}
		Direction clickedFace = ctx.getClickedFace();
		if (clickedFace.getAxis() == Axis.Y) {
			fail(player, ONLY_WALLS);
			return;
		}
		Level level = ctx.getLevel();
		if (level == null) {
			return;
		}
		if (missingCups(player)) {
			fail(player, MISSING_CUPS);
			return;
		}
		Vec3 clickPos = ctx.getClickLocation();
		if (tooFar(player, clickPos)) {
			fail(player, TOO_FAR);
			return;
		}
		BlockPos wallPos = ctx.getClickedPos();
		BlockPos headPos = wallPos.relative(ctx.getClickedFace());
		if (climbPosObstructed(player, level, clickPos, headPos, clickedFace)) {
			return;
		}
		// offset to the center of the block to prevent getting stuck in adjacent walls
		clickPos = new Vec3(Math.floor(headPos.getX()) + 0.5, clickPos.y, Math.floor(headPos.getZ()) + 0.5);
		GlobalClimbingManager.startClimbing(player, clickPos, clickedFace);
	}

	public static boolean missingCups(Player player) {
		for (EquipmentSlot slot : CUP_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			TagKey<Item> tagToCheck = slot == EquipmentSlot.FEET ? FEET_CLIMBING_CUPS : HAND_CLIMBING_CUPS;
			if (!stack.is(tagToCheck)) {
				return true; // missing suction cups
			}
		}
		return false;
	}

	public static boolean tooFar(Player player, Vec3 clickPos) {
		return clickPos.distanceTo(player.position()) > 2.5;
	}

	public static boolean climbPosObstructed(ServerPlayer player, Level level, Vec3 clickPos, BlockPos headPos, Direction clickedFace) {
		Direction facing = clickedFace.getOpposite();
		AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(player.position());
		double height = bounds.maxY - bounds.minY;
		BlockPos bottomToCheck = new BlockPos(headPos.getX(), clickPos.y - height, headPos.getZ());
		for (BlockPos pos : BlockPos.betweenClosed(headPos, bottomToCheck)) {
			BlockState state = level.getBlockState(pos);
			if (!state.getCollisionShape(level, pos).isEmpty()) {
				fail(player, OBSTRUCTED);
				return true;
			} else if (!state.getFluidState().isEmpty()) {
				fail(player, OBSTRUCTED_LIQUID);
				return true;
			}
			BlockPos wallPos = pos.relative(facing);
			if (level.getBlockState(wallPos).getCollisionShape(level, wallPos).isEmpty()) {
				fail(player, NOT_ENOUGH_WALL);
				return true;
			}
		}
		return false;
	}

	public static void fail(ServerPlayer player, Component reason) {
		player.sendSystemMessage(reason, true);
		player.playNotifySound(SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1, 1f);
	}
}
