package dev.explorercraft.grapplinghook.client.render.item;

import com.mojang.serialization.MapCodec;
import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.client.physics.ClientPhysicsControllerTracker;
import dev.explorercraft.grapplinghook.client.physics.controller.AirFrictionPhysicsController;
import dev.explorercraft.grapplinghook.client.physics.controller.ForcefieldPhysicsController;
import dev.explorercraft.grapplinghook.client.physics.controller.GrapplingHookPhysicsController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The two model states that depend on live physics rather than on the stack: whether the holder
 * currently has a hook out, and whether a forcefield is running. 26.2 replaced the old
 * ItemProperties callbacks with registered conditional properties, referenced by name from
 * assets/grapplinghook/items/*.json. Registration goes through the vanilla id mapper, which the
 * access widener opens up — there is no Fabric API for this yet.
 */
@Environment(EnvType.CLIENT)
public final class GrappleItemModelProperties {

    /** True while the holder has a hook in flight or attached. */
    public record HookThrown() implements ConditionalItemModelProperty {

        public static final MapCodec<HookThrown> MAP_CODEC = MapCodec.unit(new HookThrown());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
            GrapplingHookPhysicsController controller = controllerFor(entity);
            return controller != null && !(controller instanceof AirFrictionPhysicsController);
        }

        @Override
        public MapCodec<HookThrown> type() {
            return MAP_CODEC;
        }
    }

    /** True while the holder's forcefield is running. */
    public record ForcefieldActive() implements ConditionalItemModelProperty {

        public static final MapCodec<ForcefieldActive> MAP_CODEC = MapCodec.unit(new ForcefieldActive());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
            return controllerFor(entity) instanceof ForcefieldPhysicsController;
        }

        @Override
        public MapCodec<ForcefieldActive> type() {
            return MAP_CODEC;
        }
    }

    private static GrapplingHookPhysicsController controllerFor(LivingEntity entity) {
        if (entity == null) return null;

        GrappleModClient client = GrappleModClient.get();
        if (client == null) return null;

        ClientPhysicsControllerTracker tracker = client.getClientControllerManager();
        return tracker == null ? null : tracker.controllers.get(entity.getId());
    }

    public static void registerAll() {
        ConditionalItemModelProperties.ID_MAPPER.put(GrappleMod.id("hook_thrown"), HookThrown.MAP_CODEC);
        ConditionalItemModelProperties.ID_MAPPER.put(GrappleMod.id("forcefield_active"), ForcefieldActive.MAP_CODEC);
    }

    private GrappleItemModelProperties() {}
}
