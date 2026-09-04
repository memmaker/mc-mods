package dev.explorercraft.grapplinghook.content.customization.display;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;
import dev.explorercraft.grapplinghook.content.customization.type.EnumProperty;
import dev.explorercraft.grapplinghook.util.IFriendlyNameProvider;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class EnumPropertyDisplay<E extends Enum<E>> extends AbstractPropertyDisplay<E, EnumProperty<E>> {

    public EnumPropertyDisplay(EnumProperty<E> property) {
        super(property);
    }

    @Override
    public Component getValueHint(E value) {
        if(value == null) return null;
        return this.getValueTranslationKey(value);
    }

    public Component getValueTranslationKey(E value) {
        String type = value instanceof IFriendlyNameProvider friendly
                ? friendly.getFriendlyName()
                : this.getProperty().getIdentifier().toLanguageKey();

        return Component.translatable("enum.%s.%s".formatted(
                type,
                value == null
                        ? "null"
                        : value.name().toLowerCase()
        ));
    }

}
