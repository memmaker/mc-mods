package dev.explorercraft.pistolsilencer.item;

import net.minecraft.world.item.Item;

/** Attaching/detaching is driven entirely from {@link PistolItem#use}; this class only exists so the pistol can identify it. */
public class SilencerItem extends Item {
    public SilencerItem(Properties properties) {
        super(properties);
    }
}
