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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SuctionCupItem extends Item {
	public static final EquipmentSlot[] CUP_SLOTS = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET };
	public static final TagKey<Item> CLIMBING_CUPS = TagKey.create(Registry.ITEM_REGISTRY, Succ.id("climbing_cups"));
	public static final Component TOO_FAR = Component.translatable("succ.tooFar");
	public static final Component MISSING_CUPS = Component.translatable("succ.missingCups");
	public static final Component OBSTRUCTED = Component.translatable("succ.obstructed");
	public static final Component ONLY_WALLS = Component.translatable("succ.onlyWalls");

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

		if (climbPosObstructed(player, level, clickPos, ctx.getClickedPos(), clickedFace)) {
			fail(player, OBSTRUCTED);
			return;
		}
		// offset to the center of the block to prevent getting stuck in adjacent walls
		clickPos = new Vec3(Math.floor(clickPos.x) + 0.5, clickPos.y, Math.floor(clickPos.z) + 0.5);
		GlobalClimbingManager.startClimbing(player, clickPos, clickedFace);
	}

	public static boolean missingCups(Player player) {
		for (EquipmentSlot slot : CUP_SLOTS) {
			ItemStack stack = player.getItemBySlot(slot);
			if (!stack.is(CLIMBING_CUPS)) {
				return true; // missing suction cups
			}
		}
		return false;
	}

	public static boolean tooFar(Player player, Vec3 clickPos) {
		return clickPos.distanceTo(player.position()) > 2.5;
	}

	public static boolean climbPosObstructed(Player player, Level level, Vec3 clickPos, BlockPos clickedBlock, Direction clickedFace) {
		BlockPos topToCheck = clickedBlock.relative(clickedFace);
		float height = player.getEyeHeight();
		BlockPos bottomToCheck = new BlockPos(topToCheck.getX(), clickPos.y - height, topToCheck.getZ());
		for (BlockPos pos : BlockPos.betweenClosed(topToCheck, bottomToCheck)) {
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
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
