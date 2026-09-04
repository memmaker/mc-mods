package dev.explorercraft.grapplinghook.mixin;

import dev.explorercraft.grapplinghook.content.item.type.IDropHandling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.2 moved the three-argument drop() up from Player to LivingEntity.
@Mixin(LivingEntity.class)
public class PlayerDropHandlerMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"))
    public void handleDrop(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        if(!((Object) this instanceof Player player)) return;
        if(droppedItem.getItem() instanceof IDropHandling item) {
            item.onDroppedByPlayer(droppedItem, player);
        }
    }

}
