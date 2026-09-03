package dev.explorercraft.lift.client;

import dev.explorercraft.lift.Lift;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;

/**
 * The travelling plate borrows vanilla's falling-block renderer, which draws whatever the entity's
 * {@code getBlockState} returns — so the mod needs no renderer of its own.
 */
public class LiftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(Lift.LIFT_PLATE_ENTITY, FallingBlockRenderer::new);
    }
}
