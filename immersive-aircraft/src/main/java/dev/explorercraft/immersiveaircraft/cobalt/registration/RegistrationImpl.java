package dev.explorercraft.immersiveaircraft.cobalt.registration;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

public class RegistrationImpl extends Registration.Impl {

    @Override
    public <T extends Entity> void registerEntityRenderer(EntityType<T> type, EntityRendererProvider<T> constructor) {
        EntityRenderers.register(type, constructor);
    }

    @Override
    public void registerDataLoader(Identifier id, PreparableReloadListener loader) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, loader);
    }

    @Override
    public void registerResourceLoader(Identifier id, PreparableReloadListener loader) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, loader);
    }

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<T> obj) {
        T register = Registry.register(registry, id, obj.get());
        return () -> register;
    }
}
