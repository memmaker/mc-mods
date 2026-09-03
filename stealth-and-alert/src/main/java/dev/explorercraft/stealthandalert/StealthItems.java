package dev.explorercraft.stealthandalert;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class StealthItems {
    private StealthItems() {
    }

    private static final Identifier DAGGER_ID = StealthAndAlert.id("dagger");
    private static final Identifier PEBBLE_ID = StealthAndAlert.id("pebble");
    private static final Identifier PEBBLE_PROJECTILE_ID = StealthAndAlert.id("pebble_projectile");
    private static final Identifier PEBBLE_LAND_ID = StealthAndAlert.id("pebble_land");

    /// Weak and fast in a straight fight; the best thing there is for a strike from behind.
    public static final Item DAGGER = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, DAGGER_ID))
            .sword(ToolMaterial.IRON, 1.5F, -1.6F));

    public static final Item PEBBLE = new PebbleItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, PEBBLE_ID)));

    public static final EntityType<PebbleProjectile> PEBBLE_PROJECTILE = EntityType.Builder
            .<PebbleProjectile>of(PebbleProjectile::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, PEBBLE_PROJECTILE_ID));

    public static final SoundEvent PEBBLE_LAND = SoundEvent.createVariableRangeEvent(PEBBLE_LAND_ID);

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, DAGGER_ID, DAGGER);
        Registry.register(BuiltInRegistries.ITEM, PEBBLE_ID, PEBBLE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, PEBBLE_PROJECTILE_ID, PEBBLE_PROJECTILE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PEBBLE_LAND_ID, PEBBLE_LAND);
    }
}
