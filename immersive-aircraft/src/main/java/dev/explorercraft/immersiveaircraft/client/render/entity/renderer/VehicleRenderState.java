package dev.explorercraft.immersiveaircraft.client.render.entity.renderer;

import dev.explorercraft.immersiveaircraft.entity.VehicleEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * ponytail: the "proper" new-pipeline design would copy every field renderLocal()/BBModelRenderer
 * touch (engine power, wobble, trails, weapons, slots, animation variables, ...) into this state
 * during extractRenderState() so submit() never looks at a live entity. That would mean rewriting
 * ~15 renderer classes and every ModelPartRenderHandler animation/render lambda (which all take
 * the live entity directly) into a state-only shape - out of scope for getting vehicles visible.
 * Instead we stash the entity reference itself; submit() runs synchronously right after extract
 * every frame in this pipeline, so reading live entity state from it is safe in practice, just not
 * how vanilla mobs do it. Upgrade path: extract fields explicitly if submit() is ever moved off
 * the main thread relative to extraction.
 */
public class VehicleRenderState<T extends VehicleEntity> extends EntityRenderState {
    public T entity;
    public float tickDelta;
}
