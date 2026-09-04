package dev.explorercraft.grapplinghook.util;

import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.content.item.GrapplehookItem;
import dev.explorercraft.grapplinghook.content.item.LongFallBootsItem;
import dev.explorercraft.grapplinghook.network.clientbound.GrappleDetachS2CPayload;
import dev.explorercraft.grapplinghook.physics.ServerHookEntityTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared across multiple mixins to handle multiple damage-based events */
public class SharedDamageHandler {

    public static void handleDeath(Entity deadEntity) {
        Level level = deadEntity.level();

        if (level.isClientSide())
            return;

        if(deadEntity instanceof GrapplinghookEntity)
            return;

        if (ServerHookEntityTracker.isAttachedToHooks(deadEntity))
            return;

        ServerHookEntityTracker.removeAllHooksFor(deadEntity);
        GrapplehookItem.grapplehookEntitiesOffHand.remove(deadEntity);
        GrapplehookItem.grapplehookEntitiesMainHand.remove(deadEntity);

        if(deadEntity instanceof Player) {
            int id = deadEntity.getId();
            GrappleDetachS2CPayload detachPacket = new GrappleDetachS2CPayload(id);
            GrappleModUtils.sendToCorrectClient(detachPacket, id, level);
        }
    }

    /** @return true if the death should be cancelled. */
    public static boolean handleDamage(Entity damagedEntity, DamageSource source) {
        if (!(damagedEntity instanceof Player player)) return false;

        for (ItemStack armor : GrappleModUtils.armourItems(player)) {
            if (armor != null && armor.getItem() instanceof LongFallBootsItem) continue;
            if (source.is(DamageTypes.FLY_INTO_WALL)) return true;
        }

        return false;
    }
}
