package dev.explorercraft.whiteflag;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WhiteFlag implements ModInitializer {
    public static final String MOD_ID = "whiteflag";

    public static final Identifier WHITE_FLAG_ID = Identifier.fromNamespaceAndPath(MOD_ID, "white_flag");

    public static final Item WHITE_FLAG = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, WHITE_FLAG_ID))
            .stacksTo(1));

    private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, WHITE_FLAG_ID, WHITE_FLAG);
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> output.accept(WHITE_FLAG));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommand(dispatcher));
    }

    /** /whiteflag — hands the caller a flag without crafting it. Cheat-level, for testing. */
    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whiteflag")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ItemStack flag = new ItemStack(WHITE_FLAG);
                    if (!player.addItem(flag)) {
                        player.drop(flag, false);
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Gave 1 White Flag"), false);
                    return 1;
                }));
    }

    /** A player carrying the flag anywhere in the hotbar is off limits to every hostile mob. */
    public static boolean carriesFlag(Player player) {
        var items = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && stack.getItem() == WHITE_FLAG) {
                return true;
            }
        }
        return false;
    }
}
