package dev.explorercraft.crafttracker.gametest;

import com.google.gson.JsonObject;
import dev.explorercraft.crafttracker.CraftQueue;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;

public class CraftTrackerGameTest {
	/** Counts accumulate, entries vanish at zero, and a round trip through JSON keeps both. */
	@GameTest
	public void queueCountsAndSurvivesSerialization(GameTestHelper helper) {
		CraftQueue queue = new CraftQueue();
		queue.add(Items.IRON_INGOT, 3);
		queue.add(Items.IRON_INGOT, 2);
		queue.add(Items.STICK, 1);
		queue.add(Items.STICK, -1);

		if (queue.entries().get(Items.IRON_INGOT) != 5) {
			throw helper.assertionException("expected 5 iron ingots, got " + queue.entries().get(Items.IRON_INGOT));
		}
		if (queue.entries().containsKey(Items.STICK)) {
			throw helper.assertionException("entry should be gone once its count hits zero");
		}

		JsonObject json = queue.toJson();
		json.addProperty("not:a real item", 7);

		CraftQueue restored = new CraftQueue();
		restored.fromJson(json);
		if (!restored.entries().equals(queue.entries())) {
			throw helper.assertionException("round trip changed the queue: " + restored.entries());
		}

		helper.succeed();
	}
}
