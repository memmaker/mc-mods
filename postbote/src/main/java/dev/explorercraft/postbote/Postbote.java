package dev.explorercraft.postbote;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Postbote implements ModInitializer {
    public static final String MOD_ID = "postbote";

    /** How far, in blocks, a destination village must be from the player accepting the order. */
    public static final double MIN_DISTANCE = 750.0;
    /** How close, in blocks, the player must be to the destination to turn a compass in. */
    public static final double COMPLETION_RADIUS = 150.0;
    public static final int REWARD_BASE = 4;
    public static final int REWARD_PER_BLOCKS = 200;
    public static final int REWARD_CAP = 64;
    /**
     * Random offset points to try before giving up. Each one searches vanilla's own default
     * radius (100 chunks, {@code LocateCommand}'s figure) around itself for the nearest village,
     * so a handful of attempts covers a wide, cheap swath of the map.
     */
    private static final int MAX_ATTEMPTS = 8;
    private static final int SEARCH_RADIUS_CHUNKS = 100;
    /** How far around the destination (or the tracked villager's last spot) to look for one. */
    private static final double VILLAGER_SEARCH_RADIUS = 80.0;
    /** How often the tick job refreshes tracked-villager positions, in ticks. */
    private static final int TRACKING_INTERVAL_TICKS = 20;
    /** How close the debug teleport drops the player to the current delivery target. */
    static final double TELEPORT_RADIUS = 10.0;

    private static final List<String> FLAVOR_LINES = List.of(
            "Rattles when shaken. Best not to shake it.",
            "Marked FRAGILE in three languages, none of them helpful.",
            "Smells faintly of someone else's dinner.",
            "Heavier than it has any right to be.",
            "The label just says \"IMPORTANT\" and nothing else.",
            "Ticking. Almost certainly a clock.",
            "Wrapped twice. Someone did not trust the first layer.",
            "No return address. That is not your problem.");

    public static final Identifier SATCHEL_ID = id("postbote_satchel");
    public static final Identifier COMPASS_ID = id("postbote_compass");
    public static final Identifier ORDER_ID = id("order");
    public static final Identifier DELIVERED_PACKAGES_ID = id("delivered_packages");

    /** Carries an active order's destination and payout on the compass stack itself. */
    public static final DataComponentType<PostboteOrder> ORDER = DataComponentType.<PostboteOrder>builder()
            .persistent(PostboteOrder.CODEC)
            .networkSynchronized(PostboteOrder.STREAM_CODEC)
            .build();

    public static final Item SATCHEL = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, SATCHEL_ID))
            .stacksTo(1));

    /**
     * The needle is driven by the vanilla lodestone-tracker component and model, the same
     * mechanism the recovery compass uses for a fixed, non-lodestone target: see
     * {@code assets/postbote/items/postbote_compass.json}. Nothing custom renders it.
     */
    public static final Item COMPASS = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, COMPASS_ID))
            .stacksTo(1));

    private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, SATCHEL_ID, SATCHEL);
        Registry.register(BuiltInRegistries.ITEM, COMPASS_ID, COMPASS);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ORDER_ID, ORDER);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, DELIVERED_PACKAGES_ID, DELIVERED_PACKAGES_ID);
        Stats.CUSTOM.get(DELIVERED_PACKAGES_ID, StatFormatter.DEFAULT);
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> output.accept(SATCHEL));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommands(dispatcher));
        UseEntityCallback.EVENT.register(Postbote::onUseEntity);
        ServerTickEvents.END_SERVER_TICK.register(Postbote::tick);
    }

    /**
     * {@code Item#interactLivingEntity} runs too late to stop it: {@code Villager#mobInteract}
     * opens the trade screen on its own regardless of what the item's own hook returns.
     * UseEntityCallback fires ahead of all of vanilla's entity interaction and, if it doesn't
     * return PASS, cancels the packet handler outright — so it's the hook that actually gets to
     * pre-empt trading.
     */
    private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() == SATCHEL) {
            return startOrder(serverLevel, serverPlayer) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (held.getItem() == COMPASS) {
            return completeOrder(serverLevel, serverPlayer, held, (Villager) target) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    /**
     * Keeps every active compass's needle pointed at a real, currently-alive villager instead of
     * the fixed spot it was created at. Runs every {@link #TRACKING_INTERVAL_TICKS} ticks, cheap
     * enough since there is usually at most one active order per online player.
     */
    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % TRACKING_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack compass = activeCompassStack(player);
            if (compass != null) {
                updateTracking(server, compass);
            }
        }
    }

    /**
     * Re-homes the tracked villager if it died (or none was ever found yet), then points the
     * needle at wherever that villager — or the fallback destination, absent one — is right now.
     */
    static void updateTracking(MinecraftServer server, ItemStack compass) {
        PostboteOrder order = compass.get(ORDER);
        ServerLevel level = order == null ? null : server.getLevel(order.destination().dimension());
        if (level == null) {
            return;
        }

        Villager villager = order.villager()
                .map(level::getEntityInAnyDimension)
                .filter(entity -> entity instanceof Villager alive && alive.isAlive())
                .map(entity -> (Villager) entity)
                .orElseGet(() -> nearestAliveVillager(level, order.destination().pos()));

        BlockPos targetPos = villager != null ? villager.blockPosition() : order.destination().pos();
        Optional<UUID> villagerId = villager != null ? Optional.of(villager.getUUID()) : Optional.empty();

        compass.set(ORDER, new PostboteOrder(order.destination(), order.reward(), villagerId));
        compass.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(order.destination().dimension(), targetPos)), false));
    }

    private static Villager nearestAliveVillager(ServerLevel level, BlockPos anchor) {
        return level.getEntitiesOfClass(Villager.class, new AABB(anchor).inflate(VILLAGER_SEARCH_RADIUS), Villager::isAlive)
                .stream()
                .min(Comparator.comparingDouble(v -> v.blockPosition().distSqr(anchor)))
                .orElse(null);
    }

    /** Wherever the compass currently points: the tracked villager if alive, else the fallback. */
    private static BlockPos currentTargetPos(ServerLevel level, PostboteOrder order) {
        return order.villager()
                .map(level::getEntityInAnyDimension)
                .filter(entity -> entity instanceof Villager alive && alive.isAlive())
                .map(Entity::blockPosition)
                .orElseGet(() -> order.destination().pos());
    }

    /** /postbote give | status | teleport — cheat-level helpers for testing without crafting or exploring. */
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("give").executes(context -> give(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("teleport").executes(context -> teleport(context.getSource())))
                .executes(context -> give(context.getSource())));
    }

    private static int give(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack satchel = new ItemStack(SATCHEL);

        if (!player.addItem(satchel)) {
            player.drop(satchel, false);
        }

        source.sendSuccess(() -> Component.literal("Gave 1 Postbote Satchel"), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PostboteOrder order = activeOrder(player);
        String text = order == null
                ? "activeOrder=none"
                : "activeOrder=%s reward=%d".formatted(order.destination(), order.reward());

        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    /** Drops the player within {@link #TELEPORT_RADIUS} blocks of the delivery, on the surface. */
    private static int teleport(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PostboteOrder order = activeOrder(player);
        if (order == null) {
            source.sendFailure(Component.literal("No active delivery to teleport to."));
            return 0;
        }

        ServerLevel level = source.getServer().getLevel(order.destination().dimension());
        if (level == null) {
            source.sendFailure(Component.literal("Delivery dimension isn't loaded."));
            return 0;
        }

        BlockPos target = currentTargetPos(level, order);
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * TELEPORT_RADIUS;
        int x = target.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = target.getZ() + (int) Math.round(Math.sin(angle) * distance);

        // Force the chunk to finish generating before reading its heightmap — an unloaded or
        // only-partially-generated chunk reports a bogus, far-too-low height, which is what
        // dropped players into the void here.
        level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        player.teleportTo(level, x + 0.5, y, z + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        player.resetFallDistance();
        source.sendSuccess(() -> Component.literal("Teleported near the delivery."), false);
        return 1;
    }

    /** Reward for a delivery covering this many blocks, base plus distance, capped. */
    static int rewardFor(double distance) {
        return (int) Math.min(REWARD_CAP, REWARD_BASE + distance / REWARD_PER_BLOCKS);
    }

    /** The order carried on any compass in the player's inventory, or null if there is none. */
    static PostboteOrder activeOrder(Player player) {
        ItemStack stack = activeCompassStack(player);
        return stack == null ? null : stack.get(ORDER);
    }

    /** The compass stack carrying an active order, or null if there is none. */
    private static ItemStack activeCompassStack(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == COMPASS && stack.get(ORDER) != null) {
                return stack;
            }
        }
        return null;
    }

    /**
     * Picks a village at least {@link #MIN_DISTANCE} blocks away, in a random direction, and
     * hands the player a compass pointed at it.
     *
     * ponytail: distance is a heuristic, not a tracked-history check — "far enough to be new"
     * stands in for "never visited". Revisit with real per-player discovery tracking if players
     * start looping back to the same handful of nearby villages.
     */
    static boolean startOrder(ServerLevel level, ServerPlayer player) {
        if (activeOrder(player) != null) {
            player.sendSystemMessage(Component.literal("Finish your current delivery first."));
            return false;
        }

        BlockPos origin = player.blockPosition();
        RandomSource random = player.getRandom();
        HolderSet<Structure> villages = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(StructureTags.VILLAGE);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double offset = MIN_DISTANCE + random.nextDouble() * MIN_DISTANCE;
            BlockPos searchFrom = origin.offset((int) (Math.cos(angle) * offset), 0, (int) (Math.sin(angle) * offset));

            Pair<BlockPos, ?> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, villages, searchFrom, SEARCH_RADIUS_CHUNKS, false);
            if (found == null) {
                continue;
            }

            BlockPos destination = found.getFirst();
            double distance = Math.sqrt(origin.distSqr(destination));
            if (distance < MIN_DISTANCE) {
                continue;
            }

            int reward = rewardFor(distance);
            String flavor = FLAVOR_LINES.get(random.nextInt(FLAVOR_LINES.size()));
            GlobalPos globalDestination = GlobalPos.of(level.dimension(), destination);

            ItemStack compass = new ItemStack(COMPASS);
            compass.set(ORDER, new PostboteOrder(globalDestination, reward, Optional.empty()));
            // Fallback needle target until the tick job finds a real villager nearby to track:
            // see postbote_compass.json's "target": "lodestone" range_dispatch.
            compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(globalDestination), false));
            compass.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(flavor))));
            if (!player.addItem(compass)) {
                player.drop(compass, false);
            }

            player.sendSystemMessage(Component.literal(flavor));
            player.sendSystemMessage(Component.literal("New delivery accepted. Pays %d emeralds on arrival.".formatted(reward)));
            return true;
        }

        player.sendSystemMessage(Component.literal("No delivery out there right now. Try again."));
        return false;
    }

    /**
     * Pays out and consumes the compass if {@code target} is the tracked villager, or — while
     * none has been found yet — if the player is near the fallback destination.
     */
    static boolean completeOrder(ServerLevel level, ServerPlayer player, ItemStack compass, Villager target) {
        PostboteOrder order = compass.get(ORDER);
        if (order == null) {
            return false;
        }

        GlobalPos destination = order.destination();
        boolean isTrackedVillager = order.villager().map(id -> id.equals(target.getUUID())).orElse(false);
        boolean fallbackInRange = order.villager().isEmpty()
                && player.blockPosition().distSqr(destination.pos()) <= COMPLETION_RADIUS * COMPLETION_RADIUS;

        if (!destination.dimension().equals(level.dimension()) || !(isTrackedVillager || fallbackInRange)) {
            player.sendSystemMessage(Component.literal("This isn't the delivery village."));
            return false;
        }

        compass.shrink(1);
        ItemStack payout = new ItemStack(Items.EMERALD, order.reward());
        if (!player.addItem(payout)) {
            player.drop(payout, false);
        }
        player.awardStat(Stats.CUSTOM.get(DELIVERED_PACKAGES_ID));
        player.sendSystemMessage(Component.literal("Delivered. Paid %d emeralds.".formatted(order.reward())));

        if (player.getStats().getValue(Stats.CUSTOM, DELIVERED_PACKAGES_ID) % 2 == 0) {
            ItemStack bonus = new ItemStack(Items.ENDER_EYE);
            if (!player.addItem(bonus)) {
                player.drop(bonus, false);
            }
            player.sendSystemMessage(Component.literal("Bonus for your loyalty: an Eye of Ender."));
        }
        return true;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
