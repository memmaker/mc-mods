package dev.explorercraft.stealthandalert;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class StealthTags {
    private StealthTags() {
    }

    /// Mobs that run the stealth system instead of vanilla targeting.
    public static final TagKey<EntityType<?>> SEEKERS = entity("seekers");
    /// Who seekers can detect. Players, basically.
    public static final TagKey<EntityType<?>> DETECTABLE = entity("detectable");
    /// Mobs that never hunt but do remember being hit, e.g. villagers.
    public static final TagKey<EntityType<?>> PROTECTED = entity("protected");

    /// Mobs a strike from behind can take down outright.
    public static final TagKey<EntityType<?>> CAN_BE_ASSASSINATED = entity("can_be_assassinated");

    /// Weapons that can do it, and the subset that is best at it.
    public static final TagKey<Item> CAN_ASSASSINATE = item("can_assassinate");
    public static final TagKey<Item> DAGGERS = item("daggers");

    /// Blocks you can hide inside.
    public static final TagKey<Block> CAN_COVER = block("can_cover");
    /// Blocks that stop nothing: glass, fences, bars.
    public static final TagKey<Block> SEE_THROUGHS = block("see_throughs");

    /// Entity types only carry their tags through the registry holder.
    public static boolean is(Entity entity, TagKey<EntityType<?>> tag) {
        return entity.getType().builtInRegistryHolder().is(tag);
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, StealthAndAlert.id(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, StealthAndAlert.id(path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, StealthAndAlert.id(path));
    }
}
