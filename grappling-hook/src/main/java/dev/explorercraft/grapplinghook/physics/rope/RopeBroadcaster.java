package dev.explorercraft.grapplinghook.physics.rope;

import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.network.NetworkManager;
import dev.explorercraft.grapplinghook.network.clientbound.RopeSegmentUpdateS2CPayload;
import dev.explorercraft.grapplinghook.util.GrappleModUtils;
import dev.explorercraft.grapplinghook.util.NullableDirection;
import dev.explorercraft.grapplinghook.util.Vec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class RopeBroadcaster {

    private RopeBroadcaster() {}

    static void broadcastAdd(GrapplinghookEntity hook, Level world, int index, RopeBend bend) {
        if (world.isClientSide()) return;
        RopeSegmentUpdateS2CPayload msg = new RopeSegmentUpdateS2CPayload(
                hook.getId(), true, index,
                bend.worldPos,
                NullableDirection.fromVanilla(bend.topSide),
                NullableDirection.fromVanilla(bend.bottomSide),
                bend.space);
        send(hook, world, msg);
    }

    static void broadcastRemove(GrapplinghookEntity hook, Level world, int index) {
        if (world.isClientSide()) return;
        RopeSegmentUpdateS2CPayload msg = new RopeSegmentUpdateS2CPayload(
                hook.getId(), false, index,
                new Vec(0, 0, 0), NullableDirection.DOWN, NullableDirection.DOWN,
                AnchorSpace.World.INSTANCE);
        send(hook, world, msg);
    }

    private static void send(GrapplinghookEntity hook, Level world, RopeSegmentUpdateS2CPayload msg) {
        Vec playerpoint = Vec.positionVec(hook.shootingEntity);
        NetworkManager.packetToClient(msg, GrappleModUtils.getPlayersThatCanSeeChunkAt((ServerLevel) world, playerpoint));
    }
}
