package dev.explorercraft.stealthandalert;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// A strike on someone who never saw it coming. No animation, no cutscene — hit an unaware mob
/// from behind with a blade and it takes the damage a normal swing would only dream of.
public final class Assassination {
    private Assassination() {
    }

    private static final ResourceKey<DamageType> ASSASSINATION =
            ResourceKey.create(Registries.DAMAGE_TYPE, StealthAndAlert.id("assassination"));

    /// Daggers are built for this; anything else is improvising.
    private static final float DAGGER_MULTIPLIER = 6.0F;
    private static final float BLADE_MULTIPLIER = 3.0F;

    /// True while the bonus hit is being dealt, so it cannot trigger itself.
    private static boolean dealing;

    /// Called right after a normal hit landed, before the mob has processed being alerted.
    public static void tryAfterHit(LivingEntity target, Player player, float damageDealt, AlertData targetAlert) {
        if (dealing || damageDealt <= 0) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        if (!StealthTags.is(target, StealthTags.CAN_BE_ASSASSINATED) || !target.isAlive() || target.isVehicle()) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(StealthTags.CAN_ASSASSINATE)) return;
        if (!unaware(target, player, targetAlert) || !fromBehind(target, player)) return;

        float multiplier = weapon.is(StealthTags.DAGGERS) ? DAGGER_MULTIPLIER : BLADE_MULTIPLIER;
        float bonus = damageDealt * (multiplier - 1.0F);

        DamageSource source = new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ASSASSINATION), player, player);

        dealing = true;
        try {
            target.hurtServer(level, source, bonus);
        } finally {
            dealing = false;
        }

        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.7, 0);
        level.sendParticles(ParticleTypes.CRIT, chest.x, chest.y, chest.z, 12, 0.2, 0.2, 0.2, 0.4);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /// The mob must not already be hunting this player, or fighting at all.
    private static boolean unaware(LivingEntity target, Player player, AlertData alert) {
        if (StealthTags.is(target, StealthTags.SEEKERS)) {
            return alert.state() < AlertData.FIGHTING && alert.stateOf(player.getUUID()) < AlertData.TRACKING;
        }
        return !(target instanceof Mob mob) || mob.getTarget() != player;
    }

    /// Behind means behind: the player has to be in the half-space the mob is not facing.
    private static boolean fromBehind(LivingEntity target, Player player) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYRot());
        Vec3 toPlayer = player.position().subtract(target.position());
        return facing.dot(new Vec3(toPlayer.x, 0, toPlayer.z).normalize()) < 0;
    }
}
