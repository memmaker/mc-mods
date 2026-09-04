package dev.explorercraft.grapplinghook.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.physics.attach.HookAttachment;
import dev.explorercraft.grapplinghook.physics.rope.RopeSegmentHandler;
import dev.explorercraft.grapplinghook.content.registry.CustomizationProperties;
import dev.explorercraft.grapplinghook.content.registry.internal.ModDataComponents;
import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.enums.RopeStyle;
import dev.explorercraft.grapplinghook.util.Vec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;



/** This file is part of GrappleMod.

 GrappleMod is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 GrappleMod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with GrappleMod.  If not, see <a href="http://www.gnu.org/licenses/">...</a>.
 */

@Environment(EnvType.CLIENT)
public class GrapplinghookEntityRenderer<T extends GrapplinghookEntity> extends EntityRenderer<T, GrapplinghookRenderState> {

	public static final Vector3f X_AXIS = new Vector3f(1, 0, 0);
	public static final Vector3f Y_AXIS = new Vector3f(0, 1, 0);
	public static final Vector3f Z_AXIS = new Vector3f(0, 0, 1);

    private static final Identifier HOOK_TEXTURES = GrappleMod.id("textures/entity/hook.png");
    private static final Identifier ROPE_TEXTURES = GrappleMod.id("textures/entity/rope.png");

	// 26.2 ships an emissive translucent entity layer, so the hand-built CompositeState this
	// used to declare (and the access widener it needed) are gone.
	private static final RenderType ROPE_RENDER_GLOWING = RenderTypes.entityTranslucentEmissive(ROPE_TEXTURES);
	private static final RenderType ROPE_RENDER = RenderTypes.entityTranslucent(ROPE_TEXTURES);

	private final EntityRendererProvider.Context context;
	private final Item item;

	public GrapplinghookEntityRenderer(EntityRendererProvider.Context context, Item itemIn) {
		super(context);
		this.item = itemIn;
		this.context = context;
	}

    @Override
    public GrapplinghookRenderState createRenderState() {
        return new GrapplinghookRenderState();
    }

    /**
     * The whole of the old render() geometry pass. It runs here because this is the only place
     * 26.2 still hands the renderer the entity and its holder.
     */
    @Override
    public void extractRenderState(T hookEntity, GrapplinghookRenderState state, float partialTicks) {
        super.extractRenderState(hookEntity, state, partialTicks);

        state.drawable = false;
        state.ropePath.clear();

        if (hookEntity == null || !hookEntity.isAlive()) return;
        if (!(hookEntity.shootingEntity instanceof LivingEntity holder)) return;
        if (!holder.isAlive()) return;

        RopeSegmentHandler ropeHandler = hookEntity.getSegmentHandler();

        // is right hand?
        int handDirection = (holder.getMainArm() == HumanoidArm.RIGHT ? 1 : -1) * (hookEntity.isHeldInMainHand() ? 1 : -1);

        // attack/swing progress
        float completion = holder.getAttackAnim(partialTicks);
        float swingPosition = Mth.sin(Mth.sqrt(completion) * (float) Math.PI);

        // get the offset from the center of the head to the hand
        boolean isFirstPerson = this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && holder == Minecraft.getInstance().player;
        Vec handOffset = isFirstPerson
                ? this.getFirstPersonHandOffset(holder, handDirection, swingPosition, partialTicks)
                : this.getThirdPersonHandOffset(holder, handDirection, swingPosition, partialTicks);

        // get the hand position
        handOffset.y += holder.getEyeHeight();
        Vec handPosition = handOffset.add(Vec.partialPositionVec(holder, partialTicks));

        double dispatcherLerpX = Mth.lerp((double) partialTicks, hookEntity.xOld, hookEntity.getX());
        double dispatcherLerpY = Mth.lerp((double) partialTicks, hookEntity.yOld, hookEntity.getY());
        double dispatcherLerpZ = Mth.lerp((double) partialTicks, hookEntity.zOld, hookEntity.getZ());

        HookAttachment attachment = hookEntity.attachment();
        Vec renderAnchor;
        if (attachment != null && attachment.rendersViaExplicitAnchor()) {
            Vec3 w = attachment.worldHitPoint(partialTicks);
            renderAnchor = new Vec(w.x, w.y, w.z);
            state.anchorOffset = new Vec(
                    renderAnchor.x - dispatcherLerpX,
                    renderAnchor.y - dispatcherLerpY,
                    renderAnchor.z - dispatcherLerpZ);
        } else {
            renderAnchor = new Vec(dispatcherLerpX, dispatcherLerpY, dispatcherLerpZ);
            state.anchorOffset = new Vec(0, 0, 0);
        }

        state.handDirection = handDirection;
        state.handPosition = this.getRelativeToAnchor(renderAnchor, new Vec(handPosition));
        state.attachDirection = this.resolveAttachDirection(hookEntity, ropeHandler, handPosition, renderAnchor);

        // rope path, anchor-relative, hand end last
        if (ropeHandler == null) {
            state.ropePath.add(new Vec(0, 0, 0));
            state.ropePath.add(state.handPosition);
        } else {
            List<Vec> segments = ropeHandler.getSegments();
            for (int i = 0; i < segments.size(); i++) {
                Vec point = i == 0
                        ? renderAnchor
                        : (i == segments.size() - 1 ? handPosition : segments.get(i));
                state.ropePath.add(this.getRelativeToAnchor(renderAnchor, point));
            }
        }
        state.taut = hookEntity.taut;

        HookCustomization volume = hookEntity.getCurrentCustomizations();
        state.glowingRope = volume.get(CustomizationProperties.GLOWING_ROPE.get());
        state.style = volume.get(CustomizationProperties.ROPE_STYLE.get());

        this.context.getItemModelResolver().updateForNonLiving(
                state.hookModel, this.getStackToRender(), ItemDisplayContext.NONE, hookEntity);

        state.drawable = true;
    }

    @Override
    public void submit(GrapplinghookRenderState state, PoseStack matrix, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.drawable) {
            super.submit(state, matrix, collector, camera);
            return;
        }

        matrix.pushPose();
        matrix.translate(state.anchorOffset.x, state.anchorOffset.y, state.anchorOffset.z);

        this.submitHook(state, matrix, collector);
        this.submitRope(state, matrix, collector);

        matrix.popPose();
        super.submit(state, matrix, collector, camera);
    }

    /** Which way the hook model points: its own motion, its recorded attach normal, or the rope. */
    private Vec resolveAttachDirection(T hookEntity, RopeSegmentHandler ropeHandler, Vec handPosition, Vec renderAnchor) {
        Vec attachDirection = Vec.motionVec(hookEntity).scale(-1);

        if (attachDirection.length() == 0) {
            if (hookEntity.attachDirection != null) {
                attachDirection = hookEntity.attachDirection;
            } else {
                List<Vec> dirSegs = ropeHandler == null ? null : ropeHandler.getSegments();
                if (dirSegs == null || dirSegs.size() <= 2) {
                    attachDirection = this.getRelativeToAnchor(renderAnchor, new Vec(handPosition));
                } else {
                    attachDirection = dirSegs.get(1).sub(renderAnchor);
                }
            }
        }

        attachDirection.mutableNormalize();

        if (hookEntity.isAttachedToSurface() && hookEntity.attachDirection != null)
            attachDirection = hookEntity.attachDirection;

        hookEntity.attachDirection = attachDirection;
        return attachDirection;
    }

	protected Vec getFirstPersonHandOffset(LivingEntity grappleHookHolder, int handDirection, float swingPos, float partialTicks) {
		// base hand offset (no swing, when facing +Z)
		double d7 = this.entityRenderDispatcher.options.fov().get();
		d7 = d7 / 100.0D;

		Vec handOffset = new Vec(
				(double) handDirection * -0.46D * d7,
				-0.18D * d7,
				0.38D
		);

		// apply swing
		handOffset = handOffset.rotatePitch(-swingPos * 0.7F);
		handOffset = handOffset.rotateYaw(-swingPos * 0.5F);

		// apply looking direction
		handOffset = handOffset.rotatePitch(-Vec.lerp(partialTicks, grappleHookHolder.xRotO, grappleHookHolder.getXRot()) * ((float)Math.PI / 180F));
		return handOffset.rotateYaw(Vec.lerp(partialTicks, grappleHookHolder.yRotO, grappleHookHolder.getYRot()) * ((float)Math.PI / 180F));
	}

	protected Vec getThirdPersonHandOffset(LivingEntity grappleHookHolder, int handDirection, float swingPos, float partialTicks) {
		// base hand offset (no swing, when facing +Z)
		Vec handOffset = new Vec(
				(double) handDirection * -0.36D,
				-0.65D + (grappleHookHolder.isCrouching() ? -0.1875F : 0.0F),
				0.6D
		);

		// apply swing
		handOffset = handOffset.rotatePitch(swingPos * 0.7F);

		// apply body rotation
		return handOffset.rotateYaw(Vec.lerp(partialTicks, grappleHookHolder.yBodyRotO, grappleHookHolder.yBodyRot) * ((float)Math.PI / 180F));
	}

    private Vec getRelativeToAnchor(Vec renderAnchor, Vec inVec) {
    	return inVec.sub(renderAnchor);
    }

	private void submitHook(GrapplinghookRenderState state, PoseStack matrix, SubmitNodeCollector collector) {
		Vec attachDirection = state.attachDirection;

		// transformation so hook texture is facing the correct way
		matrix.pushPose();
		matrix.scale(0.5F, 0.5F, 0.5F);

		matrix.mulPose(rotateAxis(-attachDirection.getYaw(), Y_AXIS));
		matrix.mulPose(rotateAxis(attachDirection.getPitch() - 90.0f, X_AXIS));
		matrix.mulPose(rotateAxis(45.0f * state.handDirection, Y_AXIS));
		matrix.mulPose(rotateAxis(-45.0f, Z_AXIS));

		state.hookModel.submit(matrix, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

		// revert transformation
		matrix.popPose();
	}

	public static Quaternionf rotateAxis(double angleDegrees, Vector3f axis) {
		return new Quaternionf().rotateAxis((float) Math.toRadians(angleDegrees), axis);
	}

	private void submitRope(GrapplinghookRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		RenderType layer = state.glowingRope ? ROPE_RENDER_GLOWING : ROPE_RENDER;

		collector.submitCustomGeometry(poseStack, layer, (pose, vertexBuffer) -> {
			List<Vec> path = state.ropePath;
			for (int i = 0; i < path.size() - 1; i++) {
				double taut = i == path.size() - 2 ? state.taut : 1.0D;
				this.drawSegment(path.get(i), path.get(i + 1), taut, vertexBuffer, pose,
						state.lightCoords, state.style);
			}

			this.drawRopeEnding(state, vertexBuffer, pose);
		});
	}

	private void drawRopeEnding(GrapplinghookRenderState state, VertexConsumer vertexBuffer, PoseStack.Pose pose) {
		// draw tip of rope closest to hand
		List<Vec> path = state.ropePath;
		Vec handPosition = state.handPosition;
		Vec hand_closest = path.size() <= 2
				? path.get(0)
				: path.get(path.size() - 2);

		Vec diff = hand_closest.sub(handPosition);
		Vec forward = diff.withMagnitude(1);
		Vec up = forward.cross(new Vec(1, 0, 0));

		if (up.length() == 0)
			up = forward.cross(new Vec(0, 0, 1));

		up.mutableSetMagnitude(0.025);

		Vec sideDir = forward.cross(up);
		sideDir.mutableSetMagnitude(0.025);

		Vec[] corners = new Vec[] {
				up.scale(-1).add(sideDir.scale(-1)),
				up.scale(-1).add(sideDir),
				up.add(sideDir),
				up.add(sideDir.scale(-1))
		};

		float[][] uvs = new float[][] {
				{state.style.getTextureMinBound(),  0f},
				{state.style.getTextureMidBound(),  0f},
				{state.style.getTextureMidBound(),  1f / 16f},
				{state.style.getTextureMinBound(),  1f / 16f}
		};

		for (int side = 0; side < 4; side++) {
			Vec corner = corners[side];
			Vec normal = corner.normalize(); //.add(forward.normalize().mult(-1)).normalize();
			Vec cornerPos = handPosition.add(corner);
			vertexBuffer
					.addVertex(pose, (float) cornerPos.x, (float) cornerPos.y, (float) cornerPos.z)
					.setColor(255, 255, 255, 255)
					.setUv(uvs[side][0], uvs[side][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(state.lightCoords)
					.setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
		}
	}

	// draw a segment of the rope
    private void drawSegment(Vec start, Vec finish, double taut, VertexConsumer vertexBuffer, PoseStack.Pose pose, int packedLight, RopeStyle style) {
    	if (start.sub(finish).length() < 0.05)
			return;

		float ropeStyleUVStart = style.getTextureMinBound();
		float ropeStyleUVEnd = style.getTextureMaxBound();

		int number_squares = taut == 1.0F ? 1 : 16;

    	Vec diff = finish.sub(start);
        
        Vec forward = diff.withMagnitude(1);
        Vec up = forward.cross(new Vec(1, 0, 0));

        if (up.length() == 0)
			up = forward.cross(new Vec(0, 0, 1));

        up.mutableSetMagnitude(0.025);
        Vec sideDir = forward.cross(up);
        sideDir.mutableSetMagnitude(0.025);
        
        Vec[] corners = new Vec[] {
				up.scale(-1).add(sideDir.scale(-1)),
				up.add(sideDir.scale(-1)),
				up.add(sideDir),
				up.scale(-1).add(sideDir)
		};

        for (int side = 0; side < 4; side++) {
            Vec corner1 = corners[side];
            Vec corner2 = corners[(side + 1) % 4];

        	Vec normal1 = corner1.normalize();
        	Vec normal2 = corner2.normalize();

			boolean flipNormal = side % 2 == 0;
            
            for (int square_num = 0; square_num < number_squares; square_num++) {
                float squarefrac1 = (float)square_num / (float) number_squares;
                Vec pos1 = start.add(diff.scale(squarefrac1));
                pos1.y += - (1 - taut) * (0.25 - Math.pow((squarefrac1 - 0.5), 2)) * 1.5;

                float squarefrac2 = ((float) square_num+1) / (float) number_squares;
                Vec pos2 = start.add(diff.scale(squarefrac2));
                pos2.y += - (1 - taut) * (0.25 - Math.pow((squarefrac2 - 0.5), 2)) * 1.5;
                
                Vec corner1pos1 = pos1.add(corner1);
                Vec corner2pos1 = pos1.add(corner2);
                Vec corner1pos2 = pos2.add(corner1);
                Vec corner2pos2 = pos2.add(corner2);

				float uLeft = flipNormal ? ropeStyleUVEnd : ropeStyleUVStart;
				float uRight = flipNormal ? ropeStyleUVStart : ropeStyleUVEnd;

                vertexBuffer
						.addVertex(pose, (float) corner1pos1.x, (float) corner1pos1.y, (float) corner1pos1.z)
						.setColor(255, 255, 255, 255)
						.setUv(uLeft, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
						.setNormal(pose, (float) normal1.x, (float) normal1.y, (float) normal1.z);
                vertexBuffer
						.addVertex(pose, (float) corner2pos1.x, (float) corner2pos1.y, (float) corner2pos1.z)
						.setColor(255, 255, 255, 255)
						.setUv(uRight, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
						.setNormal(pose, (float) normal2.x, (float) normal2.y, (float) normal2.z);
                vertexBuffer
						.addVertex(pose, (float) corner2pos2.x, (float) corner2pos2.y, (float) corner2pos2.z)
						.setColor(255, 255, 255, 255)
						.setUv(uRight, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
						.setNormal(pose, (float) normal2.x, (float) normal2.y, (float) normal2.z);
				vertexBuffer
						.addVertex(pose, (float) corner1pos2.x, (float) corner1pos2.y, (float) corner1pos2.z)
						.setColor(255, 255, 255, 255)
						.setUv(uLeft, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight)
						.setNormal(pose, (float) normal1.x, (float) normal1.y, (float) normal1.z);
            }
        }
        
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
		// The rope reaches far outside the hook's own box, so never let the frustum cull it.
		return true;
	}

	public ItemStack getStackToRender() {
		ItemStack stack = new ItemStack(this.item);
		stack.set(ModDataComponents.FORCE_HOOK_DISPLAY, Unit.INSTANCE);
        return stack;
    }

}
