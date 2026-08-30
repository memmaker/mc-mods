package dev.explorercraft.rebreather;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class Rebreather implements ModInitializer {
    public static final String MOD_ID = "rebreather";

    private static final Identifier REBREATHER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "rebreather");

    /**
     * A helmet-slot item with no armour material and no equipment asset: no protection, no
     * durability, and nothing drawn over the head model beyond the flat icon vanilla already
     * renders for any head item. All it does is keep the air bar full.
     *
     * <p>The refill is a tick handler rather than the {@code minecraft:oxygen_bonus} attribute
     * because that attribute is capped at 1024, which only makes each tick's air loss a
     * 1-in-1025 dice roll — close to unlimited, but not unlimited.
     */
    public static final Item REBREATHER = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, REBREATHER_ID))
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
            .equippable(EquipmentSlot.HEAD)) {

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
            // EntityEquipment ticks every worn stack with the slot it sits in, so this fires once a
            // tick on the wearer and never while the rebreather is only being carried.
            if (slot == EquipmentSlot.HEAD) {
                entity.setAirSupply(entity.getMaxAirSupply());
            }
        }
    };

    private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, REBREATHER_ID, REBREATHER);
        CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(output -> output.accept(REBREATHER));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
    }

    /** /rebreather give | status — cheat-level helpers for testing without crafting or drowning. */
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give").executes(context -> give(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .executes(context -> give(context.getSource())));
    }

    private static int give(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack rebreather = new ItemStack(REBREATHER);

        if (!player.addItem(rebreather)) {
            player.drop(rebreather, false);
        }

        source.sendSuccess(() -> Component.literal("Gave 1 Miniature Rebreather"), false);
        return 1;
    }

    /** Air pinned at the maximum while submerged is the whole feature; this shows both numbers. */
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean worn = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == REBREATHER;
        String text = "worn=%s air=%d/%d underwater=%s".formatted(
                worn, player.getAirSupply(), player.getMaxAirSupply(), player.isUnderWater());

        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
