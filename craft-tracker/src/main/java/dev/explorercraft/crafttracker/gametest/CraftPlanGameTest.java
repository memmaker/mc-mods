package dev.explorercraft.crafttracker.gametest;

import dev.explorercraft.crafttracker.CraftPlan;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

public class CraftPlanGameTest {
	/** A chain collapses to what you have to go and find, rounding each craft up. */
	@GameTest
	public void followsChainDownToRawMaterials(GameTestHelper helper) {
		Map<Item, List<CraftPlan.Craft>> table = Map.of(
				Items.STICK, List.of(new CraftPlan.Craft(4, Map.of(Items.OAK_PLANKS, 2))),
				Items.OAK_PLANKS, List.of(new CraftPlan.Craft(4, Map.of(Items.OAK_LOG, 1))));

		// 5 sticks: 2 crafts, so 4 planks, so 1 craft of planks, so 1 log.
		Map<Item, Integer> raw = CraftPlan.rawMaterials(Items.STICK, 5, CraftPlan.from(table));
		expect(helper, raw, Map.of(Items.OAK_LOG, 1));

		expect(helper, CraftPlan.rawMaterials(Items.DIAMOND, 3, CraftPlan.from(table)), Map.of(Items.DIAMOND, 3));
		helper.succeed();
	}

	/** Nine ingots out of a block is not a way to make ingots, and neither is a loop. */
	@GameTest
	public void ignoresUnpackingAndLoops(GameTestHelper helper) {
		Map<Item, List<CraftPlan.Craft>> table = Map.of(
				Items.IRON_INGOT, List.of(new CraftPlan.Craft(9, Map.of(Items.IRON_BLOCK, 1))),
				Items.IRON_BLOCK, List.of(new CraftPlan.Craft(1, Map.of(Items.IRON_INGOT, 9))));

		expect(helper, CraftPlan.rawMaterials(Items.IRON_INGOT, 5, CraftPlan.from(table)), Map.of(Items.IRON_INGOT, 5));
		expect(helper, CraftPlan.rawMaterials(Items.IRON_BLOCK, 2, CraftPlan.from(table)), Map.of(Items.IRON_INGOT, 18));
		helper.succeed();
	}

	private static void expect(GameTestHelper helper, Map<Item, Integer> actual, Map<Item, Integer> expected) {
		if (!actual.equals(expected)) {
			throw helper.assertionException("expected " + expected + ", got " + actual);
		}
	}
}
