package dev.explorercraft.photosafari.mixin;

import dev.explorercraft.photosafari.PhotoSafari;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Camerapture hands a finished photo to the player from a lambda deep inside its packet
/// handler, and every route it could take ends here — so this is the one place a camera's
/// default album can intercept it, without pinning a mixin to another mod's lambda name.
@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void photosafari$fileInDefaultAlbum(ItemStack stack, boolean sendPacket, CallbackInfo ci) {
        if (!player.level().isClientSide() && PhotoSafari.fileInDefaultAlbum(player, stack)) {
            stack.setCount(0);
            ci.cancel();
        }
    }
}
