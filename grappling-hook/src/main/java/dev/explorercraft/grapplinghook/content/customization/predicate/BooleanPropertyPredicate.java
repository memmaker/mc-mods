package dev.explorercraft.grapplinghook.content.customization.predicate;

import dev.explorercraft.grapplinghook.content.customization.type.CustomizationProperty;

public class BooleanPropertyPredicate extends SinglePropertyPredicate<Boolean> {

    private final Boolean expectedValue;

    public BooleanPropertyPredicate(CustomizationProperty<Boolean> property, Boolean expectedValue) {
        super(property);
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean shouldPass(Boolean value) {
        if(expectedValue == null || value == null)
            return expectedValue == value;

        return expectedValue.booleanValue() == value.booleanValue();
    }

    public Boolean getExpectedValue() {
        return this.expectedValue;
    }
}
