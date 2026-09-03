package dev.explorercraft.stealthandalert;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/// A thrown pebble. Harmless, but it lands loudly somewhere you are not.
public class PebbleProjectile extends ThrowableItemProjectile {
    public PebbleProjectile(EntityType<? extends PebbleProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public PebbleProjectile(Level level, LivingEntity owner) {
        super(StealthItems.PEBBLE_PROJECTILE, owner, level, new ItemStack(StealthItems.PEBBLE));
    }

    @Override
    protected Item getDefaultItem() {
        return StealthItems.PEBBLE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide()) {
            result.getEntity().hurtServer((net.minecraft.server.level.ServerLevel) level(),
                    damageSources().thrown(this, getOwner()), 0.5F);
            land();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            land();
        }
    }

    /// The whole point of the item: a loud noise over there, not here.
    private void land() {
        level().playSound(null, blockPosition(), StealthItems.PEBBLE_LAND, SoundSource.NEUTRAL, 0.5F, 2.0F);
        if (getOwner() instanceof Player thrower) {
            Acoustics.emit(level(), position(), thrower, 50.0, 9.0, AlertSoundData.LOW);
        }
        discard();
    }
}
