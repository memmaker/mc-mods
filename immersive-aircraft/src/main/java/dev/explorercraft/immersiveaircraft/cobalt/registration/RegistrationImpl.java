package dev.explorercraft.immersiveaircraft.cobalt.registration;

import dev.explorercraft.immersiveaircraft.cobalt.data.JsonDataLoaderWrapper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
        EntityRendererRegistry.register(type, constructor);
    }

    @Override
    public void registerDataLoader(Identifier id, PreparableReloadListener loader) {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new JsonDataLoaderWrapper(id, loader));
    }

    @Override
    public void registerResourceLoader(Identifier id, PreparableReloadListener loader) {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new JsonDataLoaderWrapper(id, loader));
    }

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, Identifier id, Supplier<T> obj) {
        T register = Registry.register(registry, id, obj.get());
        return () -> register;
    }
}
