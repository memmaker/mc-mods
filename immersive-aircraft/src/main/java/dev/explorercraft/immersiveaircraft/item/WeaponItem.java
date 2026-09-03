package dev.explorercraft.immersiveaircraft.item;

import dev.explorercraft.immersiveaircraft.entity.misc.WeaponMount;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class WeaponItem extends DescriptionItem {
    private final WeaponMount.Type mountType;

    public WeaponItem(Properties settings, WeaponMount.Type mountType) {
        super(settings);

        this.mountType = mountType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag context) {
        tooltip.accept(Component.translatable("item.immersiveaircraft.item.weapon").withStyle(ChatFormatting.GRAY));

        super.appendHoverText(stack, tooltipContext, tooltipDisplay, tooltip, context);
    }

    public WeaponMount.Type getMountType() {
        return mountType;
    }
}
