package dev.explorercraft.grapplinghook.client.render.entity;

import dev.explorercraft.grapplinghook.content.customization.type.enums.RopeStyle;
import dev.explorercraft.grapplinghook.util.Vec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.2 splits entity rendering into an extract pass (has the entity) and a submit pass (does not),
 * so everything the rope and hook need is gathered here first. Positions are stored relative to the
 * render anchor, exactly as the old single-pass renderer computed them.
 */
@Environment(EnvType.CLIENT)
public class GrapplinghookRenderState extends EntityRenderState {

    /** False when the hook has no living holder — nothing is drawn. */
    public boolean drawable;

    /** Rope path, relative to the render anchor, hand end last. */
    public final List<Vec> ropePath = new ArrayList<>();
    /** Slack of the final segment; 1.0 draws it straight. */
    public double taut = 1.0;

    /** Hand end of the rope, relative to the render anchor. */
    public Vec handPosition = new Vec(0, 0, 0);
    public Vec attachDirection = new Vec(0, 1, 0);
    public int handDirection = 1;

    /** Offset from the entity's interpolated position to the anchor the rope is drawn around. */
    public Vec anchorOffset = new Vec(0, 0, 0);

    public RopeStyle style = RopeStyle.REGULAR;
    public boolean glowingRope;

    public final ItemStackRenderState hookModel = new ItemStackRenderState();
}
