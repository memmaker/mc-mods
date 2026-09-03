package dev.explorercraft.pistolsilencer.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;

/**
 * Reads fx-globals' real, on-screen head box for a mob by reflection. Client-only: that data only
 * exists in the render model, which a dedicated server never loads, so this must never be touched
 * from common code.
 */
final class FxGlobalsHeadBoxClient {
    private static final Method GET = resolve();

    private FxGlobalsHeadBoxClient() {
    }

    static AABB get(int entityId) {
        if (GET == null) {
            return null;
        }
        try {
            return (AABB) GET.invoke(null, entityId);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method resolve() {
        if (!FabricLoader.getInstance().isModLoaded("fxglobals")) {
            return null;
        }
        try {
            return Class.forName("com.explorercraft.fxglobals.client.HeadBoxes").getMethod("get", int.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
