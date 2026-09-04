package dev.explorercraft.grapplinghook.content.item;

import dev.explorercraft.grapplinghook.GrappleMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import dev.explorercraft.grapplinghook.content.registry.internal.ModTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.function.Consumer;

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

/**
 * 26.2 dropped the ArmorMaterial registry: a material is now a plain record handed to
 * Item.Properties, and the worn model comes from assets/grapplinghook/equipment/long_fall_boot_ish.json.
 */
public class LongFallBootsItem extends Item {

	private static final ResourceKey<EquipmentAsset> ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, GrappleMod.id("long_fall_boot_ish"));

	private static final ArmorMaterial MATERIAL = new ArmorMaterial(
			15,
			Map.of(ArmorType.BOOTS, 3),
			10,
			SoundEvents.ARMOR_EQUIP_CHAIN,
			2.0F,
			0.0F,
			ModTags.LONG_FALL_BOOTS_REPAIR,
			ASSET);

	public LongFallBootsItem(Item.Properties properties) {
		super(properties
				.stacksTo(1)
				.humanoidArmor(MATERIAL, ArmorType.BOOTS)
				.attributes(MATERIAL.createAttributes(ArmorType.BOOTS)));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
	                            Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
		tooltipAdder.accept(Component
				.translatable("grappletooltip.longfallboots.desc")
				.withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY)
		);
	}

}
