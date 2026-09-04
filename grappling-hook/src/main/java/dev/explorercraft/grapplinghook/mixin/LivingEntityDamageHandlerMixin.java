package dev.explorercraft.grapplinghook.mixin;

import dev.explorercraft.grapplinghook.content.item.LongFallBootsItem;
import dev.explorercraft.grapplinghook.util.GrappleModUtils;
import dev.explorercraft.grapplinghook.util.SharedDamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageHandlerMixin {

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    public void handleDeath(DamageSource source, CallbackInfo ci){
        SharedDamageHandler.handleDeath((Entity) (Object) this);
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    public void handleDamage(ServerLevel level, DamageSource source, float damage, CallbackInfo ci){
        LivingEntity thiss = (LivingEntity) (Object) this;

        if(thiss.isInvulnerableTo(level, source)) return;
        if(SharedDamageHandler.handleDamage((Entity) (Object) this, source)) ci.cancel();
    }

    @Inject(method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    public void handleFall(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Entity thiss = (Entity) (Object) this;
        if (thiss instanceof Player player) {

            for (ItemStack armorStack : GrappleModUtils.armourItems(player)) {
                if(armorStack == null) continue;
                if(armorStack.getItem() instanceof LongFallBootsItem)
                    cir.setReturnValue(false);
            }
        }
    }
}
