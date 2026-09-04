package dev.explorercraft.plushboots;

import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class PlushBoots implements ModInitializer {
    public static final String MOD_ID = "plushboots";

    private static final Identifier PLUSH_BOOTS_ID = Identifier.fromNamespaceAndPath(MOD_ID, "plush_boots");

    /** Points the worn model at assets/plushboots/equipment/plush.json. */
    private static final ResourceKey<EquipmentAsset> PLUSH_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MOD_ID, "plush"));

    /** Iron-grade durability, leather-grade protection: the boots are about the landing, not the fight. */
    private static final ArmorMaterial PLUSH = new ArmorMaterial(
            15,
            Map.of(ArmorType.BOOTS, 1),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0F,
            0.0F,
            ItemTags.WOOL,
            PLUSH_ASSET);

    /**
     * The whole feature. LivingEntity.calculateFallDamage multiplies the fall by
     * FALL_DAMAGE_MULTIPLIER, so scaling that to zero while the boots are worn removes fall damage
     * outright — no mixin, no tick handler, and it travels with the item stack to any wearer.
     */
    private static final AttributeModifier NO_FALL_DAMAGE = new AttributeModifier(
            Identifier.fromNamespaceAndPath(MOD_ID, "no_fall_damage"),
            -1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public static final Item PLUSH_BOOTS = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, PLUSH_BOOTS_ID))
            .humanoidArmor(PLUSH, ArmorType.BOOTS)
            .attributes(PLUSH.createAttributes(ArmorType.BOOTS)
                    .withModifierAdded(Attributes.FALL_DAMAGE_MULTIPLIER, NO_FALL_DAMAGE, EquipmentSlotGroup.FEET)));

    private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, PLUSH_BOOTS_ID, PLUSH_BOOTS);
        CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(output -> output.accept(PLUSH_BOOTS));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
    }

    /** /plushboots give | status — cheat-level helpers for testing without crafting. */
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give").executes(context -> give(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .executes(context -> give(context.getSource())));
    }

    private static int give(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack boots = new ItemStack(PLUSH_BOOTS);

        if (!player.addItem(boots)) {
            player.drop(boots, false);
        }

        source.sendSuccess(() -> Component.literal("Gave 1 pair of Plush Boots"), false);
        return 1;
    }

    /**
     * Reads back the attribute the boots modify. 0 means fall damage is off; 1 means the boots are
     * not on the wearer's feet, whatever the inventory looks like.
     */
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double multiplier = player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        boolean worn = player.getItemBySlot(EquipmentSlot.FEET).getItem() == PLUSH_BOOTS;
        String text = "boots worn=%s fallDamageMultiplier=%.2f fallDistance=%.2f".formatted(
                worn, multiplier, player.fallDistance);

        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
