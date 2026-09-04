package dev.explorercraft.grapplinghook.content.physics;

import dev.explorercraft.grapplinghook.GrappleMod;
import net.minecraft.resources.Identifier;

/**
 * Physics are handled on the client side, however, these IDs are useful
 * on the server side for advancements.
 */
public class PhysicsControllers {

    public static final Identifier NONE = GrappleMod.id("none");

    public static final Identifier GRAPPLING_HOOK = GrappleMod.id("grappling_hook");
    public static final Identifier AIR_FRICTION = GrappleMod.id("air_friction");
    public static final Identifier FORCEFIELD = GrappleMod.id("forcefield");

}
