package com.explorercraft.fxglobals.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Only job: keep {@link HeadBoxes} from growing forever as mobs despawn or wander off screen. */
public class FxGlobalsClient implements ClientModInitializer {
	private static final int PRUNE_INTERVAL_TICKS = 100;

	private int ticksUntilPrune = PRUNE_INTERVAL_TICKS;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (--ticksUntilPrune <= 0) {
				ticksUntilPrune = PRUNE_INTERVAL_TICKS;
				HeadBoxes.prune();
			}
		});
	}
}
