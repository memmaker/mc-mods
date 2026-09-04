package dev.explorercraft.grapplinghook.content.registry.internal;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.entity.grapplinghook.GrapplinghookEntity;
import dev.explorercraft.grapplinghook.content.registry.helper.AbstractRegistryReference;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModEntities {

    private static final HashMap<Identifier, EntityEntry<?>> entities;

    static {
        entities = new HashMap<>();
    }

    public static <E extends EntityType<?>> EntityEntry<E> entity(String id, Supplier<E> type) {
        Identifier qualId = GrappleMod.id(id);
        EntityEntry<E> entry = new EntityEntry<>(qualId, type);
        entities.put(qualId, entry);
        return entry;
    }


    public static void registerAllEntities() {
        for(Map.Entry<Identifier, EntityEntry<?>> def: entities.entrySet()) {
            Identifier id = def.getKey();
            EntityEntry<?> data = def.getValue();
            EntityType<?> it = data.getFactory().get();

            data.finalize(Registry.register(BuiltInRegistries.ENTITY_TYPE, id, it));
        }
    }

    public static final EntityEntry<EntityType<GrapplinghookEntity>> GRAPPLE_HOOK = ModEntities
            .entity("grapplehook", () -> EntityType.Builder
                    .<GrapplinghookEntity>of(GrapplinghookEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    // The rope is drawn from the hook, so it has to keep ticking well past the
                    // default projectile tracking range or long swings pop out of existence.
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, GrappleMod.id("grapplehook")))
            );



    public static class EntityEntry<T extends EntityType<?>> extends AbstractRegistryReference<T> {

        protected EntityEntry(Identifier id, Supplier<T> factory) {
            super(id, factory);
        }
    }

}


