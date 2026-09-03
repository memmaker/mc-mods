package dev.explorercraft.immersiveaircraft.item;

import dev.explorercraft.immersiveaircraft.util.FlowingText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public abstract class DescriptionItem extends Item {
    public DescriptionItem(Properties properties) {
        super(properties);
    }

    /// Vanilla marks this hook deprecated, but the component-driven tooltip pipeline only calls
    /// the fixed set of components it knows about, so an item's own extra line still has nowhere
    /// else to come from. Overriding it is the supported path until Mojang provides one.
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, tooltipContext, tooltipDisplay, tooltip, context);

        FlowingText.wrap(Component.translatable(getDescriptionId() + ".description").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), 180).forEach(tooltip);
    }
}
