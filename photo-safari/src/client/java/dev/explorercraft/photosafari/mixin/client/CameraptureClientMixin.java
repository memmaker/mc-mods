package dev.explorercraft.photosafari.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.camerapture.CameraptureClient;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// Mirrors AlbumItemMixin's swap on the client side: onUseItem opens the picture viewer
/// when this (the album's own shift check, the first of the two in this method) says
/// shift is down, so flipping it makes the viewer a shift-right-click and leaves the
/// overview (now opened by default by the mixed server-side AlbumItem#use) as the default.
/// See AlbumItemMixin for why this is @WrapOperation and not plain @Redirect.
@Mixin(CameraptureClient.class)
public abstract class CameraptureClientMixin {
    @WrapOperation(method = "onUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isShiftKeyDown()Z", ordinal = 0))
    private static boolean photosafari$invertAlbumShiftCheck(Player player, Operation<Boolean> original) {
        return !original.call(player);
    }
}
