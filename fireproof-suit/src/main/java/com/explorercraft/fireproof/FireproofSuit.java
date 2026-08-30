package com.explorercraft.fireproof;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class FireproofSuit implements ModInitializer {
	public static final String MOD_ID = "fireproofsuit";

	/** What one worn piece takes off fire and lava damage. Four pieces reach 1.0 — immunity. */
	public static final double PROTECTION_PER_PIECE = 0.25;

	/** Points the worn model at assets/fireproofsuit/equipment/fireproof.json. */
	private static final ResourceKey<EquipmentAsset> FIREPROOF_ASSET =
			ResourceKey.create(EquipmentAssets.ROOT_ID, id("fireproof"));

	private static final TagKey<Item> REPAIRS_FIREPROOF_SUIT =
			TagKey.create(Registries.ITEM, id("repairs_fireproof_suit"));

	/**
	 * Iron-grade defence so the suit is a sidegrade, not a replacement for late-game armour:
	 * what you actually wear it for is the fire protection, which no armour value can buy.
	 * Durability sits between iron and diamond — magma cream is nether-gated, not cheap.
	 */
	private static final ArmorMaterial FIREPROOF = new ArmorMaterial(
			22,
			Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2),
			10,
			SoundEvents.ARMOR_EQUIP_NETHERITE,
			0.0F,
			0.0F,
			REPAIRS_FIREPROOF_SUIT,
			FIREPROOF_ASSET);

	/**
	 * LivingEntity.igniteForTicks scales every burn by BURNING_TIME, so a quarter off per piece
	 * means a full suit never catches fire at all. Damage reduction alone would leave you
	 * standing in lava permanently ablaze but unhurt, which reads as a bug rather than a suit.
	 *
	 * <p>The id has to differ per piece: attributes hold modifiers in a map keyed by id, so four
	 * pieces sharing one id would collapse into a single quarter off no matter how much you wear.
	 * ADD_MULTIPLIED_BASE rather than ADD_MULTIPLIED_TOTAL because the latter compounds — four
	 * quarters off would leave 0.75^4 of the burn rather than none of it.
	 */
	private static AttributeModifier lessBurning(ArmorType type) {
		return new AttributeModifier(
				id("burns_less_" + type.getName()),
				-PROTECTION_PER_PIECE,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	public static final Item FIREPROOF_HELMET = armorPiece("fireproof_helmet", ArmorType.HELMET);
	public static final Item FIREPROOF_CHESTPLATE = armorPiece("fireproof_chestplate", ArmorType.CHESTPLATE);
	public static final Item FIREPROOF_LEGGINGS = armorPiece("fireproof_leggings", ArmorType.LEGGINGS);
	public static final Item FIREPROOF_BOOTS = armorPiece("fireproof_boots", ArmorType.BOOTS);

	/** The suit, head to feet: the slot each piece has to be worn in to count. */
	public static final Map<EquipmentSlot, Item> PIECES = new LinkedHashMap<>(Map.of(
			EquipmentSlot.HEAD, FIREPROOF_HELMET,
			EquipmentSlot.CHEST, FIREPROOF_CHESTPLATE,
			EquipmentSlot.LEGS, FIREPROOF_LEGGINGS,
			EquipmentSlot.FEET, FIREPROOF_BOOTS));

	@Override
	public void onInitialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(output -> PIECES.values().forEach(output::accept));
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
	}

	/**
	 * The fraction of fire damage the entity's worn pieces cancel, 0.0 to 1.0. Counting worn
	 * items rather than tracking state keeps this correct for any LivingEntity, including mobs
	 * that picked the armour up, and survives the wearer swapping pieces mid-burn.
	 */
	public static double fireProtection(LivingEntity entity) {
		int worn = 0;

		for (Map.Entry<EquipmentSlot, Item> piece : PIECES.entrySet()) {
			if (entity.getItemBySlot(piece.getKey()).getItem() == piece.getValue()) {
				worn++;
			}
		}

		return Math.min(1.0, worn * PROTECTION_PER_PIECE);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static Item armorPiece(String name, ArmorType type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));

		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties()
				.setId(key)
				.fireResistant()
				.humanoidArmor(FIREPROOF, type)
				.attributes(FIREPROOF.createAttributes(type)
						.withModifierAdded(Attributes.BURNING_TIME, lessBurning(type), EquipmentSlotGroup.bySlot(type.getSlot())))));
	}

	/** /fireproofsuit give | status — cheat-level helpers for testing without crafting. */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("give").executes(context -> give(context.getSource())))
				.then(Commands.literal("status").executes(context -> status(context.getSource())))
				.executes(context -> give(context.getSource())));
	}

	private static int give(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();

		for (Item piece : PIECES.values()) {
			ItemStack stack = new ItemStack(piece);
			if (!player.addItem(stack)) {
				player.drop(stack, false);
			}
		}

		source.sendSuccess(() -> Component.literal("Gave a full Fireproof Suit"), false);
		return 1;
	}

	/**
	 * Reads back the two numbers the suit acts on: the protection the mixin applies to fire
	 * damage, and the burning-time attribute that decides whether you catch fire at all.
	 */
	private static int status(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String text = "fireProtection=%.2f burningTime=%.2f onFire=%s".formatted(
				fireProtection(player),
				player.getAttributeValue(Attributes.BURNING_TIME),
				player.isOnFire());

		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}
}
