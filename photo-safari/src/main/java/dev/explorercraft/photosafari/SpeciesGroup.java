package dev.explorercraft.photosafari;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/// Where a mob comes from, straight off its registry namespace. Splits the checklist into
/// three groups so "12 of 88" means something, without a hand-maintained mob list anywhere.
public enum SpeciesGroup {
    VANILLA(Identifier.DEFAULT_NAMESPACE),
    ALEXS_MOBS("alexsmobs"),
    /// Everything registered by any other mod.
    OTHERS(null);

    private final String namespace;

    SpeciesGroup(String namespace) {
        this.namespace = namespace;
    }

    public static SpeciesGroup of(Identifier species) {
        for (SpeciesGroup group : values()) {
            if (species.getNamespace().equals(group.namespace)) {
                return group;
            }
        }

        return OTHERS;
    }

    /// The namespace this group gates on, or null for OTHERS. Doubles as the resource
    /// condition mod id during datagen.
    public String namespace() {
        return this.namespace;
    }

    public String translationKey() {
        return "text.photosafari.group." + name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable(translationKey());
    }

    /// Every species in this group the game knows about right now.
    public int total() {
        int count = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (PhotoScan.isWildlifeType(type) && of(EntityType.getKey(type)) == this) {
                count++;
            }
        }

        return count;
    }

    /// The groups a photo's new species fall into, each named exactly once, in enum order.
    /// One photo can add several species from the same group, and species from more than one.
    public static Set<SpeciesGroup> affected(Collection<Identifier> species) {
        return species.stream().map(SpeciesGroup::of)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SpeciesGroup.class)));
    }

    public int photographed(Collection<Identifier> seen) {
        return (int) seen.stream().filter(species -> of(species) == this).count();
    }
}
