package com.explorercraft.fxglobals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Public so other mods in the pack can call it too: none of them depend on fxglobals at build
 * time, so this is invoked by name through reflection rather than a compile-time reference.
 */
public final class Headshots {
	/** No per-mob model data is available server-side, so the head is approximated as the top
	 * slice of the mob's own hitbox, narrowed inward — close to where the actual head model sits
	 * on a humanoid, and a reasonable stand-in for everything else. */
	private static final double HEAD_HEIGHT_FRACTION = 0.25;
	private static final double HEAD_WIDTH_FRACTION = 0.6;

	private Headshots() {
	}

	/**
	 * Kills the mob outright if headshots are on and the ray from {@code rayStart} to
	 * {@code rayEnd} actually crosses its head box, rather than just landing above eye height.
	 * A no-op for players, dead or non-mob entities, and anything that happens off the server
	 * thread.
	 */
	public static void apply(LivingEntity target, Vec3 rayStart, Vec3 rayEnd) {
		apply(target, rayStart, rayEnd, null);
	}

	/**
	 * Same as {@link #apply(LivingEntity, Vec3, Vec3)}, but takes a head box a client actually
	 * measured off the mob's render model — see {@code client.LivingEntityRendererHeadMixin} —
	 * instead of guessing one from the hitbox. Pass {@code null} for the guess.
	 */
	public static void apply(LivingEntity target, Vec3 rayStart, Vec3 rayEnd, AABB preciseHeadBox) {
		if (FxGlobalsConfig.headshots && target instanceof Mob mob && mob.isAlive()
				&& mob.level() instanceof ServerLevel serverLevel) {
			AABB box = preciseHeadBox != null ? preciseHeadBox : headBox(mob);

			if (box.clip(rayStart, rayEnd).isPresent()) {
				mob.kill(serverLevel);
			}
		}
	}

	private static AABB headBox(LivingEntity mob) {
		AABB box = mob.getBoundingBox();
		double headHeight = box.getYsize() * HEAD_HEIGHT_FRACTION;
		double halfWidth = box.getXsize() * HEAD_WIDTH_FRACTION / 2.0;
		double halfLength = box.getZsize() * HEAD_WIDTH_FRACTION / 2.0;
		double centerX = (box.minX + box.maxX) / 2.0;
		double centerZ = (box.minZ + box.maxZ) / 2.0;

		return new AABB(centerX - halfWidth, box.maxY - headHeight, centerZ - halfLength,
				centerX + halfWidth, box.maxY, centerZ + halfLength);
	}
}
