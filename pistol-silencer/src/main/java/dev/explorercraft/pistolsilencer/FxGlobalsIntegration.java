package dev.explorercraft.pistolsilencer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Fx Globals' headshot option kills a mob outright when a shot's ray actually crosses its head
 * box. Pistol shots are a hitscan raycast rather than an arrow entity, so that mod's own mixin
 * never sees them; this calls its public {@code Headshots.apply} by reflection instead of a
 * build-time dependency, since every mod in the pack ships and builds on its own.
 */
public final class FxGlobalsIntegration {
    private static final Method APPLY = resolve();

    private FxGlobalsIntegration() {
    }

    /**
     * @param preciseHeadBox the real head box {@code HeadBoxHints} measured off the target's
     *                       render model, or {@code null} to let fx-globals guess one from the hitbox
     */
    public static void headshot(LivingEntity target, Vec3 rayStart, Vec3 rayEnd, AABB preciseHeadBox) {
        if (APPLY == null) {
            return;
        }
        try {
            APPLY.invoke(null, target, rayStart, rayEnd, preciseHeadBox);
        } catch (ReflectiveOperationException ignored) {
            // Fx Globals is present but its API changed shape; a missed headshot is not worth crashing over.
        }
    }

    private static Method resolve() {
        if (!FabricLoader.getInstance().isModLoaded("fxglobals")) {
            return null;
        }
        try {
            return Class.forName("com.explorercraft.fxglobals.Headshots")
                    .getMethod("apply", LivingEntity.class, Vec3.class, Vec3.class, AABB.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
