package dev.explorercraft.grapplinghook.content.item;

import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/*
 * This file is part of GrappleMod.

    GrappleMod is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GrappleMod is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GrappleMod.  If not, see <http://www.gnu.org/licenses/>.
 */

public class EnderStaffItem extends Item {
	
	public EnderStaffItem(Item.Properties properties) {
		super(properties.stacksTo(1));
	}

	
    @Override
	@NotNull
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand hand) {
    	ItemStack stack = playerIn.getItemInHand(hand);

		if (!worldIn.isClientSide())
			return InteractionResult.CONSUME;

		GrappleModClient.get().launchPlayer(playerIn);

    	return InteractionResult.SUCCESS;
	}
    
	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                            Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		// 26.2 hands tooltips to a Consumer; collect into a list so the body below is unchanged.
		List<Component> tooltipComponents = new java.util.ArrayList<>();
		try {
		Options options = Minecraft.getInstance().options;

		tooltipComponents.add(Component
				.translatable("grappletooltip.launcheritem.desc")
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		tooltipComponents.add(Component.literal(""));


		tooltipComponents.add(Component
				.translatable("grappletooltip.controls.title")
				.withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
		);
		tooltipComponents.add(TextUtils.keybinding("grappletooltip.launcheritemcontrols.desc", options.keyUse));
	
		} finally {
			tooltipComponents.forEach(tooltipAdder);
		}
	}
}
