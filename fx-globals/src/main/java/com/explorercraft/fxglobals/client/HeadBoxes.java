package com.explorercraft.fxglobals.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The real, current-frame head box of every on-screen mob, keyed by entity id. Filled in by
 * {@link LivingEntityRendererHeadMixin} from the actual posed render model — the only place in
 * the game that knows what a mob's head model looks like, since a dedicated server has none of
 * this and only ever sees a hitbox. Public so other mods can read it by reflection without a
 * build-time dependency on fxglobals; see pistol-silencer's {@code FxGlobalsIntegration}.
 */
public final class HeadBoxes {
	private static final Map<LivingEntityRenderState, Integer> ENTITY_IDS = new IdentityHashMap<>();
	private static final Map<Integer, Entry> BOXES = new ConcurrentHashMap<>();

	/** How long a head box is trusted after its last update before {@link #prune} drops it. */
	private static final long MAX_AGE_MILLIS = 2000;

	private HeadBoxes() {
	}

	public static void trackEntity(LivingEntityRenderState state, int entityId) {
		ENTITY_IDS.put(state, entityId);
	}

	public static void record(LivingEntityRenderState state, AABB headBox) {
		Integer entityId = ENTITY_IDS.get(state);

		if (entityId != null) {
			BOXES.put(entityId, new Entry(headBox, System.currentTimeMillis()));
		}
	}

	/** The mob's real head box in world coordinates, or {@code null} if it was not on screen recently. */
	public static AABB get(int entityId) {
		Entry entry = BOXES.get(entityId);
		return entry == null ? null : entry.box;
	}

	/** Drops boxes for mobs that have not been rendered in a while — gone, or off screen. */
	public static void prune() {
		long cutoff = System.currentTimeMillis() - MAX_AGE_MILLIS;
		BOXES.values().removeIf(entry -> entry.recordedAtMillis < cutoff);
	}

	private record Entry(AABB box, long recordedAtMillis) {
	}
}
