package dev.explorercraft.photosafari.client;

import me.chrr.camerapture.gui.PictureScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// Camerapture's picture viewer always closes straight back to the world. Opened from a
/// slot in some other screen — the inventory, a chest, the album overview — it should
/// close back to that screen instead, the way any nested GUI does.
public class ContainerPictureScreen extends PictureScreen {
    private final Screen parent;

    public ContainerPictureScreen(List<ItemStack> pictures, int index, Screen parent) {
        super(pictures);
        this.parent = parent;
        // The constructor always starts at 0; walk to the picture that was actually clicked.
        changeIndexBy(index);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
