package dev.explorercraft.photosafari.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/// {@code Mob#dropCustomDeathLoot} is protected and has no public equivalent. It is where
/// worn equipment (and any mob-specific bonus drops) leave the entity on death, so loot
/// mode needs a way to trigger it without actually killing anything.
@Mixin(Mob.class)
public interface MobDropAccessor {
    @Invoker("dropCustomDeathLoot")
    void photosafari$dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer);
}
