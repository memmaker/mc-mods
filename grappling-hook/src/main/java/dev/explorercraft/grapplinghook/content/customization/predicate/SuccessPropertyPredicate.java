package dev.explorercraft.grapplinghook.content.customization.predicate;

import dev.explorercraft.grapplinghook.content.customization.data.HookCustomization;

public class SuccessPropertyPredicate implements PropertyPredicate<Object> {

    public static final SuccessPropertyPredicate INSTANCE = new SuccessPropertyPredicate();

    @Override
    public boolean shouldPass(HookCustomization volume) {
        return true;
    }

    @Override
    public boolean shouldPass(Object value) {
        return true;
    }

}
