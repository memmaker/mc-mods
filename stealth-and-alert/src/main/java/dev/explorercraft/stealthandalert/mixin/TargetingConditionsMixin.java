package dev.explorercraft.stealthandalert.mixin;

import dev.explorercraft.stealthandalert.AlertData;
import dev.explorercraft.stealthandalert.StealthAndAlert;
import dev.explorercraft.stealthandalert.StealthTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// Keeps every vanilla target selector from handing a seeker a player it hasn't spotted.
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void stealthandalert$hideUnseenPlayers(ServerLevel level, LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (attacker == null || target == null) return;
        if (!StealthTags.is(attacker, StealthTags.SEEKERS) || !StealthTags.is(target, StealthTags.DETECTABLE)) return;

        AlertData data = attacker.getAttachedOrCreate(StealthAndAlert.ALERT);
        if (data.primaryTarget().isEmpty()
                || !data.primaryTarget().get().equals(target.getUUID())
                || data.state() < AlertData.FIGHTING
                || data.stateOf(target.getUUID()) < AlertData.TRACKING) {
            cir.setReturnValue(false);
        }
    }
}
