package dev.explorercraft.photosafari.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Lets a Picture be clicked straight onto an Album slot to append it to the first free
/// spot, the same interaction a Bundle uses to absorb an item. Camerapture's AlbumItem
/// never overrides this vanilla hook, so this targets the base Item and guards by type —
/// every other item keeps the vanilla (do nothing) default.
@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void photosafari$appendPictureToAlbum(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction,
            Player player, SlotAccess carriedItem, CallbackInfoReturnable<Boolean> cir) {
        if (clickAction != ClickAction.PRIMARY || !self.is(Camerapture.ALBUM) || !other.is(Camerapture.PICTURE)
                || PictureItem.getPictureData(other) == null || !slot.allowModification(player)) {
            return;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(AlbumItem.SLOTS, ItemStack.EMPTY);
        self.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);

        int freeSlot = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                freeSlot = i;
                break;
            }
        }

        // Album full: fall through to the vanilla default instead of silently doing nothing.
        if (freeSlot == -1) {
            return;
        }

        items.set(freeSlot, other.copyWithCount(1));
        self.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        other.shrink(1);

        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            menu.slotsChanged(player.getInventory());
        }

        cir.setReturnValue(true);
    }
}
