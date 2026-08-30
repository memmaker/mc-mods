package dev.explorercraft.whiteflag.mixin;

import dev.explorercraft.whiteflag.WhiteFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Every hostile path runs through here: Mob.asValidTarget -> Mob.canAttack ->
     * LivingEntity.canAttack -> target.canBeSeenAsEnemy(). Refusing enemy status blocks new
     * target acquisition and drops targets a mob already holds, for goal-driven mobs and
     * brain-driven ones (piglins, hoglins, warden) alike, since Mob.getTarget() and
     * Mob.getTargetFromBrain() both filter through asValidTarget.
     */
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void whiteflag$hideFlagBearer(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && WhiteFlag.carriesFlag(player)) {
            cir.setReturnValue(false);
        }
    }
}
