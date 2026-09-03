package dev.explorercraft.photosafari.client;

import dev.explorercraft.photosafari.PhotoSafariConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/// The Mod Menu config screen, built from vanilla's own options widgets.
public class PhotoSafariConfigScreen extends OptionsSubScreen {
    private final OptionInstance<Boolean> peacefulLoot = OptionInstance.createBoolean(
            "options.photosafari.peaceful_loot",
            OptionInstance.cachedConstantTooltip(Component.translatable("options.photosafari.peaceful_loot.tooltip")),
            PhotoSafariConfig.peacefulLoot,
            value -> PhotoSafariConfig.peacefulLoot = value);

    public PhotoSafariConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable("options.photosafari.title"));
    }

    @Override
    protected void addOptions() {
        this.list.addBig(this.peacefulLoot);
    }

    @Override
    public void removed() {
        PhotoSafariConfig.save();
        super.removed();
    }
}
