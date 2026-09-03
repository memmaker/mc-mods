package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.explorercraft.immersiveaircraft.Main;
import dev.explorercraft.immersiveaircraft.WeaponRendererRegistry;
import dev.explorercraft.immersiveaircraft.client.ColorUtils;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.BBModelRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import dev.explorercraft.immersiveaircraft.client.render.entity.weaponRenderer.WeaponRenderer;
import dev.explorercraft.immersiveaircraft.entity.InventoryVehicleEntity;
import dev.explorercraft.immersiveaircraft.entity.inventory.VehicleInventoryDescription;
import dev.explorercraft.immersiveaircraft.entity.weapon.Weapon;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBMesh;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBModel;
import dev.explorercraft.immersiveaircraft.resources.bbmodel.BBObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public abstract class InventoryVehicleRenderer<T extends InventoryVehicleEntity> extends DyeableVehicleEntityRenderer<T> {
    public InventoryVehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void renderLocal(T entity, float yaw, float tickDelta, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, int light) {
        super.renderLocal(entity, yaw, tickDelta, matrixStack, submitNodeCollector, light);

        // Render weapons
        LocalPlayer player = Minecraft.getInstance().player;
        for (List<Weapon> weapons : entity.getWeapons().values()) {
            for (Weapon weapon : weapons) {
                if (!weapon.getMount().blocking() || !Main.firstPersonGetter.isFirstPerson() || player == null || !entity.hasPassenger(player)) {
                    WeaponRenderer<Weapon> renderer = WeaponRendererRegistry.get(weapon);
                    if (renderer != null) {
                        renderer.render(entity, weapon, matrixStack, submitNodeCollector, light, tickDelta);
                    }
                }
            }
        }
    }

    public void renderBanners(BBModel model, BBObject ignoredObject, SubmitNodeCollector submitNodeCollector, T entity, PoseStack matrixStack, int light, float ignoredTime, ModelPartRenderHandler<T> ignoredModelPartRenderer) {
        // ponytail: banner-pattern decal rendering dropped along with BBModelRenderer.renderBanner
        // (see the ponytail note there) - the material/atlas sprite lookup it needs isn't
        // available at submit() time. Not called for now; kept as a hook for when that's ported.
    }

    public void renderSails(BBObject object, SubmitNodeCollector submitNodeCollector, T entity, PoseStack matrixStack, int light, float time) {
        List<ItemStack> slots = entity.getSlots(VehicleInventoryDescription.DYE);
        ItemStack stack = slots.stream().findFirst().orElse(ItemStack.EMPTY);
        DyeColor color = stack.getItem() instanceof DyeItem ? stack.get(DataComponents.DYE) : null;
        if (color == null) {
            color = DyeColor.WHITE;
        }
        float[] rgb = ColorUtils.hexToDecimalRGB(color.getTextureDiffuseColor());
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];

        if (object instanceof BBMesh mesh) {
            BBModelRenderer.renderSailObject(mesh, matrixStack, submitNodeCollector, light, time, r, g, b, 1.0f);
        }
    }
}
