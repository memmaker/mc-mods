package dev.explorercraft.immersiveaircraft.network.c2s;

import dev.explorercraft.immersiveaircraft.cobalt.network.Message;
import dev.explorercraft.immersiveaircraft.config.Config;
import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
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
        if (e.getRootVehicle() instanceof VehicleEntity vehicle) {
            vehicle.hurt(e.level().damageSources().fall(), damage);
            if (vehicle.isRemoved()) {
                float crashDamage = damage * Config.getInstance().crashDamage;
                if (Config.getInstance().preventKillThroughCrash) {
                    crashDamage = Math.min(crashDamage, e.getHealth() - 1.0f);
                }
                e.hurt(e.level().damageSources().fall(), crashDamage);
            }
        }
    }
}
