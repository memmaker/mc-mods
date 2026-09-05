package dev.explorercraft.photosafari.mixin.client;

import me.chrr.camerapture.picture.PictureTaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// True for the one frame Camerapture is about to grab the framebuffer from: the shutter
/// hides the HUD, the frame renders, and {@code renderTickEnd} screenshots it. Our entity
/// outlines are world geometry, so hiding the HUD does not hide them — they have to bow
/// out themselves or they end up baked into the photo.
@Mixin(PictureTaker.class)
public interface PictureTakerAccessor {
    @Accessor("takingPicture")
    boolean photosafari$isTakingPicture();
}
