package dev.explorercraft.grapplinghook.network;

import dev.explorercraft.grapplinghook.GrappleMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public interface C2SPayloadProcessor {

    void process(ServerPlayNetworking.Context ctx);

}
