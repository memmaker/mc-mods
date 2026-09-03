package com.explorercraft.fxglobals.mixin.client;

import com.explorercraft.fxglobals.client.HeadBoxes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The only place in the game that actually knows what a mob's head looks like: a dedicated
 * server never loads render models, so fx-globals' server-side headshot check normally has to
 * guess a head box from the hitbox. This reads the real one straight out of the posed model —
 * the same cubes and transforms the renderer is about to draw — for {@link HeadBoxes} to hand
 * out, and for pistol-silencer to network to the server for its hitscan shots.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererHeadMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void fxglobals$trackEntity(T entity, S state, float partialTick, CallbackInfo ci) {
		HeadBoxes.trackEntity(state, entity.getId());
	}

	/**
	 * Runs right after the model has been posed for this frame, so the pose stack already holds
	 * every part's current transform — walking, looking around, sneaking, whatever this mob is
	 * doing right now — and not just its resting pose.
	 */
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
	private void fxglobals$captureHeadBox(S state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		LivingEntityRenderer<?, ?, ?> self = (LivingEntityRenderer<?, ?, ?>) (Object) this;
		EntityModel<?> model = self.getModel();

		if (model == null) {
			return;
		}

		float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
				-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		boolean[] found = {false};

		model.root().visit(poseStack, (pose, name, index, cube) -> {
			if (!PartNames.HEAD.equals(name)) {
				return;
			}

			found[0] = true;
			Matrix4f matrix = pose.pose();

			for (float cx : new float[] {cube.minX, cube.maxX}) {
				for (float cy : new float[] {cube.minY, cube.maxY}) {
					for (float cz : new float[] {cube.minZ, cube.maxZ}) {
						Vector3f corner = matrix.transformPosition(new Vector3f(cx, cy, cz));
						bounds[0] = Math.min(bounds[0], corner.x());
						bounds[1] = Math.min(bounds[1], corner.y());
						bounds[2] = Math.min(bounds[2], corner.z());
						bounds[3] = Math.max(bounds[3], corner.x());
						bounds[4] = Math.max(bounds[4], corner.y());
						bounds[5] = Math.max(bounds[5], corner.z());
					}
				}
			}
		});

		if (!found[0]) {
			return;
		}

		// The pose stack is camera-relative (the dispatcher translates by camera-to-entity
		// offset before calling submit), so the camera's own world position closes the gap.
		Vec3 cam = camera.pos;
		HeadBoxes.record(state, new AABB(bounds[0] + cam.x, bounds[1] + cam.y, bounds[2] + cam.z,
				bounds[3] + cam.x, bounds[4] + cam.y, bounds[5] + cam.z));
	}
}
