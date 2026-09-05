package dev.explorercraft.photosafari.mixin.client;

import dev.explorercraft.photosafari.client.ContainerPictureScreen;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.AlbumLecternMenu;
import me.chrr.camerapture.gui.AlbumMenu;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/// Right-clicking a Picture sitting in any inventory slot (survival inventory, chest,
/// album grid, wherever) opens the viewer instead of vanilla's usual split-stack click.
/// The viewer closes back to the screen it was opened from, and inside an album it gets
/// the whole album to page through rather than the single clicked picture.
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

        List<ItemStack> pictures = List.of(stack);
        int index = 0;

        // Inside an album, hand the viewer every picture in the album so its forward and
        // backward buttons are there, starting on the one that was clicked.
        if (this.menu instanceof AlbumMenu || this.menu instanceof AlbumLecternMenu) {
            Container album = slot.container;
            List<ItemStack> all = new ArrayList<>();
            for (int i = 0; i < album.getContainerSize(); i++) {
                ItemStack item = album.getItem(i);
                if (item.is(Camerapture.PICTURE) && PictureItem.getPictureData(item) != null) {
                    if (item == stack) {
                        index = all.size();
                    }
                    all.add(item);
                }
            }

            if (!all.isEmpty()) {
                pictures = all;
            }
        }

        Minecraft.getInstance().gui.setScreen(
                new ContainerPictureScreen(pictures, index, (Screen) (Object) this));
        cir.setReturnValue(true);
    }
}
