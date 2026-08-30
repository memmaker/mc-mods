package com.explorercraft.seamlesscrafting.client;

import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.NearbyItemEntry;
import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig;
import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig.LocateTrailParticle;
import com.explorercraft.seamlesscrafting.net.NearbyHighlightRequestPayload;
import com.explorercraft.seamlesscrafting.net.NearbyItemsPayload;
import com.explorercraft.seamlesscrafting.net.RequestNearbyItemsPayload;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.ARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What the client knows about nearby containers, plus the locate effects. */
public final class NearbyItemsClientState {
	private static final double TRAIL_MIN_DISTANCE_SQUARED = 81.0;
	private static final int TRAIL_TICKS = 14;
	private static final int TRAIL_PARTICLES_PER_TICK = 10;

	private static List<NearbyItemEntry> entries = Collections.emptyList();
	private static List<ItemStack> craftableStacks = Collections.emptyList();
	private static boolean locateFeedbackPending;
	private static Vec3 trailStart = Vec3.ZERO;
	private static Vec3 trailTarget = Vec3.ZERO;
	private static int trailTicks;
	private static double trailProgress;

	private NearbyItemsClientState() {
	}

	public static List<NearbyItemEntry> getEntries() {
		return entries;
	}

	public static List<ItemStack> getCraftableStacks() {
		return craftableStacks;
	}

	public static void clear() {
		entries = Collections.emptyList();
		craftableStacks = Collections.emptyList();
		locateFeedbackPending = false;
		trailTicks = 0;
		trailProgress = 0.0;
	}

	public static void requestUpdate() {
		if (ClientPlayNetworking.canSend(RequestNearbyItemsPayload.ID)) {
			ClientPlayNetworking.send(new RequestNearbyItemsPayload());
		}
	}

	public static void applyPayload(NearbyItemsPayload payload) {
		entries = payload.entries();
		craftableStacks = payload.craftableStacks();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			if (minecraft.gui.screen() instanceof RecipeUpdateListener listener) {
				listener.recipesUpdated();
			}
		});
	}

	public static void requestHighlight(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}

		locateFeedbackPending = true;
		if (ClientPlayNetworking.canSend(NearbyHighlightRequestPayload.ID)) {
			ClientPlayNetworking.send(new NearbyHighlightRequestPayload(stack.copyWithCount(1)));
		}
	}

	/**
	 * Highlights are gizmos: the game fades and expires them on its own, so nothing here
	 * has to be ticked or rendered per frame.
	 */
	public static void showHighlight(List<BlockPos> positions) {
		if (positions.isEmpty()) {
			return;
		}

		if (SeamlessCraftingConfig.isHighlightEnabled()) {
			int durationMillis = SeamlessCraftingConfig.getHighlightDurationTicks() * 50;
			int color = SeamlessCraftingConfig.getHighlightColor();
			int stroke = ARGB.color(255, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
			int fill = ARGB.color((int)(SeamlessCraftingConfig.getHighlightOpacity() * 255), ARGB.red(color), ARGB.green(color), ARGB.blue(color));

			for (BlockPos pos : positions) {
				Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(stroke, 2.0f, fill))
						.setAlwaysOnTop()
						.persistForMillis(durationMillis)
						.fadeOut();
			}
		}

		Vec3 target = nearestCenter(positions);
		if (target == null || !locateFeedbackPending) {
			return;
		}

		locateFeedbackPending = false;
		if (SeamlessCraftingConfig.isSnapAimEnabled()) {
			aimAt(target);
		}
		if (SeamlessCraftingConfig.isLocateTrailEnabled()) {
			startTrail(target);
		}
		if (SeamlessCraftingConfig.isHighlightEnabled() && SeamlessCraftingConfig.isDistanceLabelEnabled()) {
			labelDistance(positions, target);
		}
	}

	public static void tick(Minecraft minecraft) {
		if (trailTicks <= 0 || minecraft.level == null) {
			trailProgress = 0.0;
			return;
		}

		double nextProgress = smoothProgress((TRAIL_TICKS - trailTicks + 1) / (double)TRAIL_TICKS);
		Vec3 delta = trailTarget.subtract(trailStart);
		ParticleOptions particle = trailParticle(SeamlessCraftingConfig.getLocateTrailParticle());
		boolean endRod = SeamlessCraftingConfig.getLocateTrailParticle() == LocateTrailParticle.END_ROD;
		double jitter = endRod ? 0.03 : 0.08;
		double verticalVelocity = endRod ? 0.0 : 0.01;

		for (int index = 0; index < TRAIL_PARTICLES_PER_TICK; index++) {
			double particleProgress = trailProgress
					+ (nextProgress - trailProgress) * (index / (double)Math.max(1, TRAIL_PARTICLES_PER_TICK - 1));
			Vec3 position = trailStart.add(delta.scale(particleProgress));
			minecraft.level.addParticle(
					particle,
					position.x + (minecraft.level.getRandom().nextDouble() - 0.5) * jitter,
					position.y + (minecraft.level.getRandom().nextDouble() - 0.5) * jitter,
					position.z + (minecraft.level.getRandom().nextDouble() - 0.5) * jitter,
					0.0,
					verticalVelocity,
					0.0
			);
		}

		trailProgress = nextProgress;
		trailTicks--;
	}

	private static void labelDistance(List<BlockPos> positions, Vec3 target) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		Vec3 eyePosition = minecraft.player.getEyePosition();
		int color = ARGB.opaque(SeamlessCraftingConfig.getHighlightColor());
		int durationMillis = SeamlessCraftingConfig.getHighlightDurationTicks() * 50;
		for (BlockPos pos : positions) {
			double distance = eyePosition.distanceTo(Vec3.atCenterOf(pos));
			Gizmos.billboardTextOverBlock(String.format("%.1fm", distance), pos, 0, color, 1.0f)
					.persistForMillis(durationMillis)
					.fadeOut();
		}
	}

	@Nullable
	private static Vec3 nearestCenter(List<BlockPos> positions) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || positions.isEmpty()) {
			return null;
		}

		Vec3 eyePosition = minecraft.player.getEyePosition();
		Vec3 nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : positions) {
			Vec3 center = Vec3.atCenterOf(pos);
			double distance = eyePosition.distanceToSqr(center);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = center;
			}
		}
		return nearest;
	}

	private static void aimAt(Vec3 target) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		Vec3 delta = target.subtract(minecraft.player.getEyePosition());
		double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		minecraft.player.setYRot((float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
		minecraft.player.setXRot((float)(-Math.toDegrees(Math.atan2(delta.y, horizontalDistance))));
	}

	private static void startTrail(Vec3 target) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		Vec3 eyePosition = minecraft.player.getEyePosition();
		if (eyePosition.distanceToSqr(target) < TRAIL_MIN_DISTANCE_SQUARED) {
			return;
		}

		trailStart = eyePosition;
		trailTarget = target;
		trailTicks = TRAIL_TICKS;
		trailProgress = 0.0;
	}

	private static double smoothProgress(double progress) {
		double clamped = Math.max(0.0, Math.min(1.0, progress));
		return clamped * clamped * (3.0 - 2.0 * clamped);
	}

	private static ParticleOptions trailParticle(LocateTrailParticle particle) {
		return switch (particle) {
			case CLOUD -> ParticleTypes.CLOUD;
			case SMOKE -> ParticleTypes.SMOKE;
			case END_ROD -> ParticleTypes.END_ROD;
		};
	}
}
