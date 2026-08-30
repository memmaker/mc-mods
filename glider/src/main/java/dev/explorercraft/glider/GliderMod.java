package dev.explorercraft.glider;

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
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class GliderMod implements ModInitializer {
    public static final String MOD_ID = "glider";

    /** How far a player must have dropped before the canopy opens, in blocks. */
    public static final double DEPLOY_FALL_DISTANCE = 1.45;
    /** Descent under an open canopy, in blocks per tick. */
    public static final double GLIDE_FALL_SPEED = -0.05;
    /** Horizontal air control while gliding — vanilla's sprinting-in-air figure. */
    public static final float GLIDE_AIR_SPEED = 0.025999999F;

    public static final Identifier GLIDER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "glider");
    public static final Identifier GLIDING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "gliding");

    public static final Item GLIDER = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, GLIDER_ID))
            .stacksTo(1));

    /**
     * Marks the held stack while its owner is airborne under an open canopy. It lives on the item
     * rather than the player because vanilla inventory sync then carries it to everyone rendering
     * that player for free, and that is what drives both the open-canopy item model and the
     * raised-arm pose. Nothing custom goes over the wire.
     */
    public static final DataComponentType<Unit> GLIDING = DataComponentType.<Unit>builder()
            .persistent(Unit.CODEC)
            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
            .ignoreSwapAnimation()
            .build();

    private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, GLIDER_ID, GLIDER);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, GLIDING_ID, GLIDING);
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> output.accept(GLIDER));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
    }

    /** /glider give | status — cheat-level helpers for testing without crafting or falling. */
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give").executes(context -> give(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .executes(context -> give(context.getSource())));
    }

    private static int give(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack glider = new ItemStack(GLIDER);

        if (!player.addItem(glider)) {
            player.drop(glider, false);
        }

        source.sendSuccess(() -> Component.literal("Gave 1 Glider"), false);
        return 1;
    }

    /**
     * Reports the canopy marker the client renders from, so a glide that looks wrong can be told
     * apart from one that only renders wrong.
     */
    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack main = player.getMainHandItem();
        String text = "held=%s canopy=%s onGround=%s fallDistance=%.2f".formatted(
                main.getItem() == GLIDER ? "glider" : main.getItem().toString(),
                main.has(GLIDING) ? "open" : "closed",
                player.onGround(),
                player.fallDistance);

        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
