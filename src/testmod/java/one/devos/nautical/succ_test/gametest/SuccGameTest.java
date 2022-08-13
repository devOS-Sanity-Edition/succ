package one.devos.nautical.succ_test.gametest;

import one.devos.nautical.succ_test.SuccTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

public class SuccGameTest implements FabricGameTest {
	@GameTest(template = SuccTest.ID + ":minecart_test")
	public void minecartTest(GameTestHelper helper) {
		helper.setBlock(2, 2, 0, Blocks.REDSTONE_BLOCK);
		helper.succeedWhenEntityPresent(EntityType.MINECART, 2, 2, 3);
	}
}
