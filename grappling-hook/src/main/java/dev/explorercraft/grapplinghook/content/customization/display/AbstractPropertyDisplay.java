package dev.explorercraft.grapplinghook.content.customization.display;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.CustomizationProperty;
import net.minecraft.network.chat.Component;


public abstract class AbstractPropertyDisplay<T, P extends CustomizationProperty<T>> {

    private final P property;

    public AbstractPropertyDisplay(P property) {
        this.property = property;
    }

    public Component getValueHint(HookCustomization volume) {
        if(!volume.has(this.property)) return null;
        return this.getValueHint(volume.get(this.property));
    }

    public abstract Component getValueHint(T value);

    public final P getProperty() {
        return this.property;
    }
}
