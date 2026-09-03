package dev.explorercraft.immersiveaircraft.network.c2s;

import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import dev.explorercraft.immersiveaircraft.config.Config;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class CollisionMessage extends Message {
    private final float damage;

    public CollisionMessage(float damage) {
        this.damage = damage;
    }

    public CollisionMessage(FriendlyByteBuf b) {
        damage = b.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf b) {
        b.writeFloat(damage);
    }

    @Override
    public void receive(Player e) {
        // Server-bound message, so the sender's level is always a ServerLevel; the check is what
        // lets the damage go through hurtServer rather than the deprecated side-agnostic hurt.
        if (e.getRootVehicle() instanceof VehicleEntity vehicle && e.level() instanceof ServerLevel level) {
            vehicle.hurtServer(level, level.damageSources().fall(), damage);
            if (vehicle.isRemoved()) {
                float crashDamage = damage * Config.getInstance().crashDamage;
                if (Config.getInstance().preventKillThroughCrash) {
                    crashDamage = Math.min(crashDamage, e.getHealth() - 1.0f);
                }
                e.hurtServer(level, level.damageSources().fall(), crashDamage);
            }
        }
    }
}
