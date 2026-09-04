package dev.explorercraft.grapplinghook.content.registry;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.customization.CustomizationCategory;
import dev.explorercraft.grapplinghook.content.customization.type.CustomizationProperty;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class GrappleModRegistries {

    public static final ResourceKey<Registry<CustomizationProperty<?>>> CUSTOMIZATION_PROPERTIES_KEY = ResourceKey.createRegistryKey(GrappleMod.id("customization_property"));
    public static final ResourceKey<Registry<CustomizationCategory>> CUSTOMIZATION_CATEGORY_KEY = ResourceKey.createRegistryKey(GrappleMod.id("customization_category"));


    public static final MappedRegistry<CustomizationProperty<?>> CUSTOMIZATION_PROPERTIES = FabricRegistryBuilder
            .create(CUSTOMIZATION_PROPERTIES_KEY)
            .attribute(RegistryAttribute.SYNCED)
            .buildAndRegister();

    public static final MappedRegistry<CustomizationCategory> CUSTOMIZATION_CATEGORIES = FabricRegistryBuilder
            .create(CUSTOMIZATION_CATEGORY_KEY)
            .buildAndRegister();

}
