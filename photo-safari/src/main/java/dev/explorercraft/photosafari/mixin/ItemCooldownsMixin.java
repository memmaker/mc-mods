package dev.explorercraft.photosafari.mixin;

import dev.explorercraft.photosafari.PhotoSafari;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// The tier 2 camera's whole point: no shutter cooldown. Camerapture puts its 60 tick
/// cooldown on from a private lambda, and loot mode adds its own, so the one place both
/// of them (and anything else) go through is the cooldown list itself.
@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {
    @Inject(method = "addCooldown(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private void photosafari$skipTier2CameraCooldown(ItemStack stack, int ticks, CallbackInfo ci) {
        if (PhotoSafari.isTier2Camera(stack)) {
            ci.cancel();
        }
    }
}
