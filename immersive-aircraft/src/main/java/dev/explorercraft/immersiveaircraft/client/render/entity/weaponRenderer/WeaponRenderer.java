package dev.explorercraft.immersiveaircraft.client.render.entity.weaponRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.BBModelRenderer;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import dev.explorercraft.immersiveaircraft.entity.weapon.Weapon;
import dev.explorercraft.immersiveaircraft.resources.BBModelLoader;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

public abstract class WeaponRenderer<W extends Weapon> {
    public <T extends VehicleEntity> void render(T entity, W weapon, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light, float time) {
        matrixStack.pushPose();
        matrixStack.mulPose(weapon.getMount().transform());

        BBModel model = BBModelLoader.MODELS.get(getModelId());
        weapon.setAnimationVariables(entity, time);
        if (model != null) {
            BBModelRenderer.renderModel(model, matrixStack, submitNodeCollector, light, time, entity, null, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        matrixStack.popPose();
    }

    protected abstract Identifier getModelId();
}
