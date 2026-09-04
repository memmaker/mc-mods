package dev.explorercraft.grapplinghook.content.customization.display;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.DoubleProperty;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class DoublePropertyDisplay extends AbstractPropertyDisplay<Double, DoubleProperty> {

    public DoublePropertyDisplay(DoubleProperty property) {
        super(property);
    }

    @Override
    public Component getValueHint(Double value) {
        if(value == null) return null;
        double v = Math.floor(value * 100) / 100;
        return Component.literal("%.3f".formatted(v));
    }

}
