package dev.explorercraft.grapplinghook.content.customization.display;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.IntegerProperty;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class IntegerPropertyDisplay extends AbstractPropertyDisplay<Integer, IntegerProperty> {

    public IntegerPropertyDisplay(IntegerProperty property) {
        super(property);
    }

    @Override
    public Component getValueHint(Integer value) {
        if(value == null) return null;
        return Component.literal(String.valueOf(value));
    }

}
