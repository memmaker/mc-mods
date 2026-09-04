package dev.explorercraft.grapplinghook.content.customization.predicate;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;

public interface PropertyPredicate<T> {

    boolean shouldPass(HookCustomization volume);
    boolean shouldPass(T value);

}
