package dev.explorercraft.stealthandalert.mixin;

import dev.explorercraft.stealthandalert.StealthLookAroundGoal;
import dev.explorercraft.stealthandalert.StealthTags;
import dev.explorercraft.stealthandalert.StealthTick;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Fabric has no per-entity tick event, so the stealth pass rides on Mob#tick.
@Mixin(Mob.class)
public abstract class MobTickMixin {
    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Unique
    private boolean stealthandalert$goalsPatched;

    @Inject(method = "tick", at = @At("TAIL"))
    private void stealthandalert$stealthTick(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!stealthandalert$goalsPatched) {
            stealthandalert$goalsPatched = true;
            stealthandalert$replaceLookGoals(mob);
        }
        StealthTick.run(mob);
    }

    /// Seekers must stop staring at players on their own, and must hold still while investigating.
    @Unique
    private void stealthandalert$replaceLookGoals(Mob mob) {
        if (mob.level().isClientSide() || !StealthTags.is(mob, StealthTags.SEEKERS)) return;

        goalSelector.getAvailableGoals().removeIf(wrapped -> wrapped.getGoal() instanceof LookAtPlayerGoal);

        int priority = -1;
        Goal toRemove = null;
        for (WrappedGoal wrapped : goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof RandomLookAroundGoal goal && !(goal instanceof StealthLookAroundGoal)) {
                priority = wrapped.getPriority();
                toRemove = goal;
                break;
            }
        }

        if (toRemove != null) {
            goalSelector.removeGoal(toRemove);
            goalSelector.addGoal(priority, new StealthLookAroundGoal(mob));
        }
    }
}
