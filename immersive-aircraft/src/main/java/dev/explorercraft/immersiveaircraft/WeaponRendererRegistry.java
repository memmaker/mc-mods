package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.client.render.entity.weaponRenderer.SimpleWeaponRenderer;
import dev.explorercraft.immersiveaircraft.client.render.entity.weaponRenderer.WeaponRenderer;
import dev.explorercraft.immersiveaircraft.entity.weapon.Weapon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class WeaponRendererRegistry {
    public static final Map<Identifier, WeaponRenderer<? extends Weapon>> REGISTRY = new HashMap<>();

    public static void register(Identifier id, WeaponRenderer<? extends Weapon> renderer) {
        REGISTRY.put(id, renderer);
    }

    static {
        register(Main.locate("rotary_cannon"), new SimpleWeaponRenderer("rotary_cannon"));
        register(Main.locate("heavy_crossbow"), new SimpleWeaponRenderer("heavy_crossbow"));
        register(Main.locate("telescope"), new SimpleWeaponRenderer("telescope"));
        register(Main.locate("bomb_bay"), new SimpleWeaponRenderer("bomb_bay"));
    }

    public static void bootstrap() {
        // nop, triggers static init
    }

    @SuppressWarnings("unchecked")
    public static <W extends Weapon> WeaponRenderer<W> get(W weapon) {
        return (WeaponRenderer<W>) REGISTRY.get(BuiltInRegistries.ITEM.getKey(weapon.getStack().getItem()));
    }
}
