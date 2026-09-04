package dev.explorercraft.photosafari.mixin.client;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.PictureScreen;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/// Right-clicking a Picture sitting in any inventory slot (survival inventory, chest,
/// album grid, wherever) opens the viewer instead of vanilla's usual split-stack click.
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Shadow
    protected abstract Slot getHoveredSlot(double mouseX, double mouseY);

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void photosafari$openPictureOnRightClick(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 1 || !this.menu.getCarried().isEmpty()) {
            return;
        }

        Slot slot = this.getHoveredSlot(event.x(), event.y());
        if (slot == null || !slot.hasItem()) {
            return;
        }

        ItemStack stack = slot.getItem();
        if (!stack.is(Camerapture.PICTURE) || PictureItem.getPictureData(stack) == null) {
            return;
        }

        Minecraft.getInstance().gui.setScreen(new PictureScreen(List.of(stack)));
        cir.setReturnValue(true);
    }
}
