package com.explorercraft.fxglobals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * World-wide pacing tweaks — longer days, slower hunger — plus the starter kit new players get
 * on first join, drawn from whatever companion Explorercraft mods happen to be installed.
 */
public class FxGlobals implements ModInitializer {
	public static final String MOD_ID = "fxglobals";

	/** Held so a config change can re-apply the clock rate without a world reload. */
	@Nullable
	private static MinecraftServer server;

	/** Set the first time a player receives starter gear, so it is never handed out twice. */
	public static final AttachmentType<Boolean> STARTER_GEAR_GIVEN = AttachmentRegistry.create(
			Identifier.fromNamespaceAndPath(MOD_ID, "starter_gear_given"),
			builder -> builder.persistent(Codec.BOOL).initializer(() -> false));

	/** Other Explorercraft mods' items, looked up by ID so this mod need not depend on them. */
	private static final Identifier SATCHEL_ID = Identifier.fromNamespaceAndPath("postbote", "postbote_satchel");
	private static final Identifier CAMERA_ID = Identifier.fromNamespaceAndPath("camerapture", "camera");
	private static final Identifier ALBUM_ID = Identifier.fromNamespaceAndPath("camerapture", "album");
	private static final Identifier WHITE_FLAG_ID = Identifier.fromNamespaceAndPath("whiteflag", "white_flag");
	private static final Identifier CLIMBING_CLAWS_ID = Identifier.fromNamespaceAndPath("climbingclaws", "climbing_claws");
	private static final Identifier PLUSH_BOOTS_ID = Identifier.fromNamespaceAndPath("nofall", "plush_boots");
	private static final Identifier BACKPACK_ID = Identifier.fromNamespaceAndPath("travelersbackpack", "standard");
	private static final int STARTER_PAPER_COUNT = 24;

	/** Used once: awards every recipe in the game, including ones added by other mods, then is spent. */
	public static final Identifier RECIPE_BOOK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "recipe_book");
	public static final Item RECIPE_BOOK = new Item(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, RECIPE_BOOK_ID))
			.stacksTo(1));

	@Override
	public void onInitialize() {
		FxGlobalsConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
			server = startedServer;
			applyClockRate();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(stoppedServer -> server = null);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, joinedServer) -> giveStarterGear(handler.player));
		Registry.register(BuiltInRegistries.ITEM, RECIPE_BOOK_ID, RECIPE_BOOK);
		UseItemCallback.EVENT.register(FxGlobals::onUseItem);
	}

	/** The recipe book learns every recipe currently loaded — vanilla's and every mod's alike,
	 * since they all end up in the same {@link net.minecraft.world.item.crafting.RecipeManager}. */
	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		if (player.getItemInHand(hand).getItem() != RECIPE_BOOK || level.isClientSide()) {
			return InteractionResult.PASS;
		}

		ServerPlayer serverPlayer = (ServerPlayer) player;
		serverPlayer.awardRecipes(level.getServer().getRecipeManager().getRecipes());
		player.getItemInHand(hand).shrink(1);
		return InteractionResult.SUCCESS;
	}

	/** Hands out the starter kit once per player, the first time they ever join. */
	public static void giveStarterGear(ServerPlayer player) {
		if (!FxGlobalsConfig.starterGear || player.getAttachedOrCreate(STARTER_GEAR_GIVEN)) {
			return;
		}

		player.setAttached(STARTER_GEAR_GIVEN, true);
		giveStarterKit(player);
	}

	/** The kit itself, with no once-per-player or setting checks — what the debug command hands out. */
	public static void giveStarterKit(ServerPlayer player) {
		giveOptional(player, SATCHEL_ID, 1);
		if (giveOptional(player, CAMERA_ID, 1)) {
			give(player, new ItemStack(Items.PAPER, STARTER_PAPER_COUNT));
		}
		giveOptional(player, ALBUM_ID, 1);
		give(player, new ItemStack(Items.COMPASS));
		give(player, new ItemStack(Items.LODESTONE));
		giveOptional(player, WHITE_FLAG_ID, 1);
		giveOptional(player, CLIMBING_CLAWS_ID, 1);
		giveOptional(player, PLUSH_BOOTS_ID, 1);
		giveOptional(player, BACKPACK_ID, 1);
		give(player, new ItemStack(RECIPE_BOOK));
	}

	/** Gives one stack of a companion mod's item, if that mod is installed. Returns whether it was. */
	private static boolean giveOptional(ServerPlayer player, Identifier itemId, int count) {
		return BuiltInRegistries.ITEM.getOptional(itemId)
				.map(item -> give(player, new ItemStack((Item) item, count)))
				.isPresent();
	}

	/** Adds an item to the inventory, dropping it at the player's feet if there is no room. */
	private static boolean give(ServerPlayer player, ItemStack stack) {
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
		return true;
	}

	/**
	 * Writes the configured rate onto the vanilla overworld clock — the same knob {@code /time rate}
	 * uses, so sleeping, daylight sensors and the in-game clock stay consistent. Safe to call any
	 * time; without a running server there is nothing to write to yet.
	 */
	public static void applyClockRate() {
		MinecraftServer running = server;

		if (running != null) {
			running.clockManager().setRate(overworldClock(running), FxGlobalsConfig.clockRate());
		}
	}

	/**
	 * /fxglobals status — the live values, so a tweak that did not take shows up straight away.
	 * /fxglobals startergear — hands the kit out again, ignoring the setting and the once-per-player
	 * flag, so the join-time gear can be inspected without making a fresh player.
	 */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> status(context.getSource()))
				.then(Commands.literal("status").executes(context -> status(context.getSource())))
				.then(Commands.literal("startergear")
						.executes(context -> starterGear(context.getSource(), List.of(context.getSource().getPlayerOrException())))
						.then(Commands.argument("targets", EntityArgument.players())
								.executes(context -> starterGear(context.getSource(),
										EntityArgument.getPlayers(context, "targets"))))));
	}

	private static int starterGear(CommandSourceStack source, Collection<ServerPlayer> targets) {
		targets.forEach(FxGlobals::giveStarterKit);
		source.sendSuccess(() -> Component.literal("Gave starter gear to " + targets.size() + " player(s)"), true);
		return targets.size();
	}

	private static int status(CommandSourceStack source) {
		MinecraftServer running = source.getServer();
		ClockState clock = running.clockManager().packState().clocks().get(overworldClock(running));
		String text = "dayLength=%.2fx clockRate=%.4f hunger=%.2fx pickupArrows=%s headshots=%s clockTicks=%d".formatted(
				FxGlobalsConfig.dayLengthFactor, clock == null ? Float.NaN : clock.rate(),
				FxGlobalsConfig.hungerFactor, FxGlobalsConfig.pickupArrows, FxGlobalsConfig.headshots,
				running.clockManager().getTotalTicks(overworldClock(running)));

		source.sendSuccess(() -> Component.literal(text), false);
		return 1;
	}

	public static Holder<WorldClock> overworldClock(MinecraftServer server) {
		return server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
	}
}
