package dev.explorercraft.photosafari.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.camerapture.item.AlbumItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// Camerapture opens the editable album overview on shift-right-click, and a quick picture
/// viewer otherwise (see the client-side onUseItem hook). We want the overview by default,
/// so this flips the one shift check AlbumItem#use makes.
///
/// ponytail: uses MixinExtras' @WrapOperation rather than plain @Redirect — @Redirect on
/// this specific call trips a ClassCastException deep in MixinExtras' own factory-redirect
/// wrapper on this toolchain (confirmed by bisecting: @Inject is fine, @Redirect crashes,
/// @WrapOperation is fine). Toolchain quirk, not a logic reason to prefer one over the other.
@Mixin(AlbumItem.class)
public abstract class AlbumItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isShiftKeyDown()Z"))
    private boolean photosafari$invertShiftCheck(Player player, Operation<Boolean> original) {
        return !original.call(player);
    }
}
