package dev.explorercraft.grapplinghook.content.customization.display;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.BooleanProperty;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class BooleanPropertyDisplay extends AbstractPropertyDisplay<Boolean, BooleanProperty> {

    public BooleanPropertyDisplay(BooleanProperty property) {
        super(property);
    }

    @Override
    public Component getValueHint(Boolean value) {
        if(value == null) return null;
        String checkboxString = value
                ? "[✓]"
                : "[ ]";
        return Component.literal(checkboxString);
    }


}
