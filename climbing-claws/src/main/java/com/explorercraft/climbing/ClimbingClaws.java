package com.explorercraft.climbing;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public class ClimbingClaws implements ModInitializer {
	public static final String MOD_ID = "climbingclaws";

	/** Metal name -> tint, matching the dyed_color set by each recipe. */
	private static final Map<String, Integer> METAL_TINTS = new LinkedHashMap<>(Map.of(
			"iron", 0xD8D8D8,
			"copper", 0xE0794B,
			"gold", 0xF5D145,
			"netherite", 0x6E5B57));

	public static final ResourceKey<Item> CLIMBING_CLAWS_KEY =
			ResourceKey.create(Registries.ITEM, id("climbing_claws"));

	public static final Item CLIMBING_CLAWS = Registry.register(
			BuiltInRegistries.ITEM,
			CLIMBING_CLAWS_KEY,
			new Item(new Item.Properties().setId(CLIMBING_CLAWS_KEY).stacksTo(1)));

	@Override
	public void onInitialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> METAL_TINTS.forEach((metal, tint) -> output.accept(stackOf(metal, tint))));
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
	}

	/** /climbingclaws [metal] | status — cheat-level helpers for testing without crafting. */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> give(context.getSource(), "iron"));

		METAL_TINTS.keySet().forEach(metal ->
				root.then(Commands.literal(metal).executes(context -> give(context.getSource(), metal))));

		root.then(Commands.literal("status").executes(context -> status(context.getSource())));
		dispatcher.register(root);
	}

	private static int give(CommandSourceStack source, String metal) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ItemStack claws = stackOf(metal, METAL_TINTS.get(metal));

		if (!player.addItem(claws)) {
			player.drop(claws, false);
		}

		source.sendSuccess(() -> Component.literal("Gave 1 pair of " + metal + " Climbing Claws"), false);
		return 1;
	}

	/**
	 * The four inputs the mixin decides on. onClimbable() is the answer vanilla movement acts on,
	 * so a climb that fails can be traced to the wall or to the hotbar without guessing.
	 */
	private static int status(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String text = "clawsInHotbar=%s touchingWall=%s facingWall=%s onClimbable=%s".formatted(
				hasClawsInHotbar(player), player.horizontalCollision, facingWall(player), player.onClimbable());

		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	/** Claws work from anywhere in the hotbar — no need to hold them. */
	public static boolean hasClawsInHotbar(Player player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
			if (inventory.getItem(slot).getItem() == CLIMBING_CLAWS) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A wall counts only when the player is looking at it: nudge the whole hitbox 0.15 along the
	 * horizontal look direction and see whether it hits block collision. Whole hitbox, not two
	 * sample heights — a thin sliver of wall next to the feet is what carries a player over the
	 * top edge, and sampling points loses it.
	 */
	public static boolean facingWall(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0.0, look.z);

		// Staring almost straight up or down leaves no meaningful facing.
		if (forward.lengthSqr() < 1.0E-4) {
			return false;
		}

		AABB probe = player.getBoundingBox().move(forward.normalize().scale(0.15));
		return player.level().getBlockCollisions(player, probe).iterator().hasNext();
	}

	private static ItemStack stackOf(String metal, int tint) {
		ItemStack stack = new ItemStack(CLIMBING_CLAWS);
		stack.set(DataComponents.DYED_COLOR, new DyedItemColor(tint));
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item." + MOD_ID + ".climbing_claws." + metal));
		return stack;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
