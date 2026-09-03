package dev.explorercraft.stealthandalert.mixin;

import dev.explorercraft.stealthandalert.AlertData;
import dev.explorercraft.stealthandalert.StealthAndAlert;
import dev.explorercraft.stealthandalert.StealthTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// A seeker may only lock onto a player it has actually confirmed. Vanilla AI keeps trying anyway.
@Mixin(Mob.class)
public abstract class MobSetTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void stealthandalert$gateTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        Mob mob = (Mob) (Object) this;
        if (!StealthTags.is(mob, StealthTags.SEEKERS) || !StealthTags.is(target, StealthTags.DETECTABLE)) return;

        AlertData data = mob.getAttachedOrCreate(StealthAndAlert.ALERT);
        if (data.primaryTarget().isEmpty() || !data.primaryTarget().get().equals(target.getUUID())) {
            ci.cancel();
            return;
        }

        if (data.state() < AlertData.FIGHTING || data.stateOf(target.getUUID()) < AlertData.TRACKING) {
            ci.cancel();
        }
    }
}
