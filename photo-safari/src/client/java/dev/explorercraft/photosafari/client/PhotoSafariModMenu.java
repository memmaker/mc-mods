package dev.explorercraft.photosafari.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/// Puts the config button next to Photo Safari in Mod Menu's list.
public class PhotoSafariModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PhotoSafariConfigScreen::new;
    }
}
