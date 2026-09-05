package dev.explorercraft.photosafari.mixin;

import dev.explorercraft.photosafari.PhotoSafari;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/// Two inventory click behaviours, both the interaction a Bundle uses to absorb an item:
/// a Picture clicked onto an Album lands in its first free spot, and an Album clicked onto
/// a Camera becomes that camera's default album. Camerapture's own items never override
/// this vanilla hook, so this targets the base Item and guards by type — every other item
/// keeps the vanilla (do nothing) default.
@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void photosafari$appendPictureToAlbum(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction,
            Player player, SlotAccess carriedItem, CallbackInfoReturnable<Boolean> cir) {
        if (clickAction != ClickAction.PRIMARY || !self.is(Camerapture.ALBUM) || !other.is(Camerapture.PICTURE)
                || PictureItem.getPictureData(other) == null || !slot.allowModification(player)) {
            return;
        }

        // Album full: fall through to the vanilla default instead of silently doing nothing.
        if (!PhotoSafari.addPictureToAlbum(self, other)) {
            return;
        }

        other.shrink(1);

        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null) {
            menu.slotsChanged(player.getInventory());
        }

        cir.setReturnValue(true);
    }

    /// Clicking an album onto a camera makes it that camera's default album: every photo
    /// the camera takes from then on is filed into it. The album stays in hand, only the
    /// pairing id is written to both stacks.
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void photosafari$setDefaultAlbum(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction,
            Player player, SlotAccess carriedItem, CallbackInfoReturnable<Boolean> cir) {
        if (clickAction != ClickAction.PRIMARY || !self.is(Camerapture.CAMERA) || !other.is(Camerapture.ALBUM)
                || !slot.allowModification(player)) {
            return;
        }

        UUID albumId = other.get(PhotoSafari.DEFAULT_ALBUM);
        if (albumId == null) {
            albumId = UUID.randomUUID();
            other.set(PhotoSafari.DEFAULT_ALBUM, albumId);
        }

        self.set(PhotoSafari.DEFAULT_ALBUM, albumId);

        // Server-side only: the client sets the same components predictively, and a second
        // identical overlay message would just fight with this one.
        if (!player.level().isClientSide()) {
            player.sendOverlayMessage(Component.translatable("text.photosafari.default_album_set"));
        }

        cir.setReturnValue(true);
    }
}
