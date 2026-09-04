package com.explorercraft.seamlesscrafting;

import com.explorercraft.seamlesscrafting.net.NearbyHighlightRequestPayload;
import com.explorercraft.seamlesscrafting.net.NearbyHighlightResponsePayload;
import com.explorercraft.seamlesscrafting.net.NearbyItemsPayload;
import com.explorercraft.seamlesscrafting.net.NearbyItemsSync;
import com.explorercraft.seamlesscrafting.net.RequestNearbyItemsPayload;
import com.explorercraft.seamlesscrafting.net.ReturnNearbyItemsPayload;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;

public class SeamlessCrafting implements ModInitializer {
	public static final String MOD_ID = "seamlesscrafting";

	@Override
	public void onInitialize() {
		SeamlessCraftingConfig.load();

		PayloadTypeRegistry.serverboundPlay().register(RequestNearbyItemsPayload.ID, RequestNearbyItemsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NearbyItemsPayload.ID, NearbyItemsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NearbyHighlightRequestPayload.ID, NearbyHighlightRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NearbyHighlightResponsePayload.ID, NearbyHighlightResponsePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ReturnNearbyItemsPayload.ID, ReturnNearbyItemsPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestNearbyItemsPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					if (hasCraftingGrid(context.player())) {
						NearbyItemsSync.sendNearbyItems(context.player());
					}
				}));

		ServerPlayNetworking.registerGlobalReceiver(NearbyHighlightRequestPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					if (!hasCraftingGrid(context.player())) {
						return;
					}

					List<BlockPos> positions = NearbyItemsSync.findHighlightPositions(context.player(), payload.stack());
					if (positions != null && !positions.isEmpty()) {
						ServerPlayNetworking.send(context.player(), new NearbyHighlightResponsePayload(positions));
					}
				}));

		ServerPlayNetworking.registerGlobalReceiver(ReturnNearbyItemsPayload.ID, (payload, context) ->
				context.server().execute(() -> {
					if (context.player().containerMenu instanceof NearbyCraftingAccess access) {
						access.seamless$cancelNearbyWithdrawals();
					}
				}));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	public static boolean hasCraftingGrid(ServerPlayer player) {
		return player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu;
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					int radius = SeamlessCraftingConfig.getNearbyRadius();
					int containers = NearbyInventoryScanner.findNearbyContainers(player.level(), player.blockPosition(), radius, player).size();
					int kinds = NearbyInventoryScanner.collectItemCounts(player.level(), player.blockPosition(), radius, player).size();
					context.getSource().sendSuccess(
							() -> Component.literal("Seamless Crafting: radius " + radius + ", " + containers + " containers, " + kinds + " item kinds"),
							false
					);
					return containers;
				}));
	}
}
