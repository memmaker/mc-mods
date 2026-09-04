package dev.explorercraft.grapplinghook.content.customization.helper;

import dev.explorercraft.grapplinghook.content.customization.type.CustomizationProperty;

public record PropertyOverride<T>(CustomizationProperty<T> property, T value) {

    public PropertyOverride {
        if(property == null) throw new IllegalArgumentException("Property cannot be null");
    }

}