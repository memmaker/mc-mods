package dev.explorercraft.botbuild;

import com.mojang.math.Transformation;
import dev.explorercraft.botbuild.mixin.BlockDisplayInvoker;
import dev.explorercraft.botbuild.mixin.DisplayInvoker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

/// Wand marks two corners, the box between them fills with ghost blocks, and one bot ferries the
/// real blocks over from a nearby container (or the owner's inventory) and places them one at a
/// time, retiring a ghost per placement.
public class BotBuild implements ModInitializer {
    public static final String MOD_ID = "botbuild";

    /// Ceiling on a single outline, so a stray far-away corner can't queue a million ghosts.
    static final int MAX_BLOCKS = 512;
    /// How far from the first corner to look for a container to draw blocks from.
    static final int CONTAINER_RANGE = 12;
    /// How far from the build site a placed bot will answer the call.
    static final double BOT_RANGE = 24.0;
    /// How close the bot has to get before it can take or place a block.
    static final double REACH = 2.0;
    /// How far an idle bot will go out of its way for a dropped snack.
    static final double FOOD_RANGE = 10.0;
    /// Ticks a bot may spend getting nowhere before the job gives up on a spot it can't reach.
    static final int STALL_LIMIT = 200;

    public static final Identifier WAND_ID = id("build_wand");
    public static final Identifier BOT_ID = id("build_bot");

    public static final Item WAND = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, WAND_ID))
            .stacksTo(1)) {
        @Override
        public InteractionResult useOn(UseOnContext context) {
            return onWandUse(context);
        }
    };

    /// Spawns its bot where you click, spawn-egg style, and is spent doing it.
    public static final Item BOT = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, BOT_ID))
            .stacksTo(16)) {
        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
                return InteractionResult.SUCCESS;
            }
            spawnBot(level, Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace())));
            context.getItemInHand().consume(1, player);
            return InteractionResult.SUCCESS;
        }
    };

    private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools"));

    /// Player UUID -> the first corner they clicked, waiting on a second.
    private static final Map<UUID, BlockPos> corners = new HashMap<>();
    /// Every build in progress, drained by {@link #tick}.
    private static final List<Job> jobs = new ArrayList<>();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, WAND_ID, WAND);
        Registry.register(BuiltInRegistries.ITEM, BOT_ID, BOT);
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> {
            output.accept(WAND);
            output.accept(BOT);
        });
        ServerTickEvents.END_SERVER_TICK.register(BotBuild::tick);
        // Ghosts are display entities and would otherwise be saved into the world.
        // ponytail: a hard crash still leaves them behind; add a scan on load if that ever bites.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            jobs.forEach(Job::cancel);
            jobs.clear();
        });
    }

    private static InteractionResult onWandUse(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            corners.remove(player.getUUID());
            jobs.removeIf(job -> {
                if (!job.owner.getUUID().equals(player.getUUID())) {
                    return false;
                }
                job.cancel();
                return true;
            });
            player.sendSystemMessage(Component.literal("Build cancelled."));
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        BlockPos first = corners.remove(player.getUUID());
        if (first == null) {
            corners.put(player.getUUID(), clicked);
            player.sendSystemMessage(Component.literal("Corner set. Click the opposite corner."));
            return InteractionResult.SUCCESS;
        }

        startJob(level, player, first, clicked);
        return InteractionResult.SUCCESS;
    }

    /// The material is whatever block the first corner was clicked on — no second item to hold.
    static void startJob(ServerLevel level, ServerPlayer owner, BlockPos first, BlockPos second) {
        BlockState state = level.getBlockState(first);
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            owner.sendSystemMessage(Component.literal("That block can't be built with."));
            return;
        }

        List<BlockPos> targets = plan(level, first, second);
        if (targets.isEmpty()) {
            owner.sendSystemMessage(Component.literal("Nothing to fill there."));
            return;
        }

        List<Allay> bots = claimBots(level, targets.getFirst());
        if (bots.isEmpty()) {
            owner.sendSystemMessage(Component.literal("No build bot within %d blocks. Place one first.".formatted((int) BOT_RANGE)));
            return;
        }

        Job job = new Job(level, owner, state, item, findSource(level, first, item));
        bots.forEach(bot -> job.workers.add(new Worker(bot)));
        for (BlockPos pos : targets) {
            job.ghosts.put(pos, spawnGhost(level, pos, state));
        }
        jobs.add(job);
        owner.sendSystemMessage(Component.literal("Building %d x %s with %d bot%s."
                .formatted(targets.size(), state.getBlock().getName().getString(), bots.size(), bots.size() == 1 ? "" : "s")));
    }

    /// Every replaceable spot in the box between the corners, up to {@link #MAX_BLOCKS}.
    static List<BlockPos> plan(Level level, BlockPos first, BlockPos second) {
        List<BlockPos> targets = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                continue;
            }
            targets.add(pos.immutable());
            if (targets.size() == MAX_BLOCKS) {
                break;
            }
        }
        return targets;
    }

    /// Nearest container around the outline that actually holds the block, or null for "use the player".
    static BlockPos findSource(ServerLevel level, BlockPos around, Item item) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-CONTAINER_RANGE, -CONTAINER_RANGE, -CONTAINER_RANGE),
                around.offset(CONTAINER_RANGE, CONTAINER_RANGE, CONTAINER_RANGE))) {
            if (!(level.getBlockEntity(pos) instanceof Container container) || container.countItem(item) == 0) {
                continue;
            }
            double distance = pos.distSqr(around);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    static boolean takeOne(Container container, Item item) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).getItem() == item) {
                container.removeItem(slot, 1);
                container.setChanged();
                return true;
            }
        }
        return false;
    }

    private static Display.BlockDisplay spawnGhost(ServerLevel level, BlockPos pos, BlockState state) {
        Display.BlockDisplay ghost = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.MOB_SUMMONED);
        ((BlockDisplayInvoker) ghost).botbuild$setBlockState(state);
        // Shrunk and centred, so a ghost never reads as a block that is already there.
        ((DisplayInvoker) ghost).botbuild$setTransformation(
                new Transformation(new Vector3f(0.3F, 0.3F, 0.3F), null, new Vector3f(0.4F, 0.4F, 0.4F), null));
        ghost.setPos(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(ghost);
        return ghost;
    }

    private static void tick(MinecraftServer server) {
        for (Iterator<Job> iterator = jobs.iterator(); iterator.hasNext(); ) {
            Job job = iterator.next();
            if (job.owner.isRemoved() || job.ghosts.isEmpty() || !job.step()) {
                job.cancel();
                iterator.remove();
            }
        }
    }

    /// An allay, flagged as ours and with its own brain switched off — the job drives it.
    static Allay spawnBot(ServerLevel level, Vec3 pos) {
        Allay allay = EntityTypes.ALLAY.create(level, EntitySpawnReason.MOB_SUMMONED);
        allay.addTag(MOD_ID);
        // The tag is what AllayMixin looks for to keep the vanilla brain from wandering off with
        // it. Everything else about the mob keeps ticking, pathfinder included.
        allay.setInvulnerable(true);
        allay.setPersistenceRequired();
        allay.setCustomName(Component.literal("Build Bot"));
        allay.setPos(pos);
        level.addFreshEntity(allay);
        return allay;
    }

    /// Every placed bot around the site that no other job has already taken, nearest first.
    static List<Allay> claimBots(ServerLevel level, BlockPos site) {
        return level.getEntities(EntityTypes.ALLAY, new AABB(site).inflate(BOT_RANGE),
                        allay -> allay.isAlive() && allay.entityTags().contains(MOD_ID)
                                && jobs.stream().flatMap(job -> job.workers.stream()).noneMatch(worker -> worker.bot == allay))
                .stream()
                .sorted(Comparator.comparingDouble(allay -> allay.distanceToSqr(Vec3.atCenterOf(site))))
                .toList();
    }

    /// What a bot does when no outline needs it: go and eat whatever food is lying around.
    /// Called once a tick per bot from {@code AllayMixin}, in place of the vanilla brain.
    public static void idleTick(ServerLevel level, Allay bot) {
        if (jobs.stream().flatMap(job -> job.workers.stream()).anyMatch(worker -> worker.bot == bot)) {
            return;
        }

        ItemEntity food = level.getEntities(EntityTypes.ITEM, bot.getBoundingBox().inflate(FOOD_RANGE),
                        item -> item.isAlive() && item.getItem().has(DataComponents.FOOD))
                .stream()
                .min(Comparator.comparingDouble(item -> item.distanceToSqr(bot)))
                .orElse(null);
        if (food == null) {
            return;
        }

        if (!moveTo(bot, food.position())) {
            return;
        }
        ItemStack stack = food.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            food.discard();
        } else {
            food.setItem(stack);
        }
        level.playSound(null, bot.getX(), bot.getY(), bot.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /// Walks the bot one tick along its path, reporting whether it is close enough to act.
    private static boolean moveTo(Allay bot, Vec3 goal) {
        if (bot.distanceToSqr(goal) <= REACH * REACH) {
            bot.getNavigation().stop();
            return true;
        }
        if (bot.getNavigation().isDone()) {
            bot.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0);
        }
        bot.getLookControl().setLookAt(goal);
        return false;
    }

    /// One bot and the ghost it is currently on the hook for.
    static final class Worker {
        final Allay bot;
        BlockPos target;
        boolean carrying;
        int stalled;

        Worker(Allay bot) {
            this.bot = bot;
        }
    }

    static final class Job {
        final ServerLevel level;
        final ServerPlayer owner;
        final BlockState state;
        final Item item;
        final BlockPos source;
        final Map<BlockPos, Display.BlockDisplay> ghosts = new LinkedHashMap<>();
        final List<Worker> workers = new ArrayList<>();

        Job(ServerLevel level, ServerPlayer owner, BlockState state, Item item, BlockPos source) {
            this.level = level;
            this.owner = owner;
            this.state = state;
            this.item = item;
            this.source = source;
        }

        /// One tick of work for every bot on the job. False means the job is over — finished, out
        /// of blocks, or out of bots.
        boolean step() {
            workers.removeIf(worker -> !worker.bot.isAlive() || worker.bot.isRemoved());
            for (Worker worker : List.copyOf(workers)) {
                if (!stepWorker(worker)) {
                    return false;
                }
            }
            if (workers.isEmpty()) {
                owner.sendSystemMessage(Component.literal("No build bots left on the job."));
                return false;
            }
            return !ghosts.isEmpty();
        }

        /// False only when the whole job is done for: the blocks ran out.
        private boolean stepWorker(Worker worker) {
            if (worker.target == null || !ghosts.containsKey(worker.target)) {
                worker.target = nextTarget();
                worker.stalled = 0;
            }
            if (worker.target == null) {
                // More bots than ghosts left; this one waits out the rest of the job.
                return true;
            }
            if (++worker.stalled > STALL_LIMIT) {
                // Its spot may just be unreachable for this bot, so hand the spot back and let
                // another one try. A bot that gives up is off the job.
                owner.sendSystemMessage(Component.literal("A build bot couldn't get there and stopped."));
                release(worker);
                return true;
            }

            if (!worker.carrying) {
                Container container = container();
                Vec3 goal = source != null && container != null
                        ? Vec3.atCenterOf(source)
                        : owner.position().add(0.0, 1.0, 0.0);
                if (!moveTo(worker.bot, goal)) {
                    return true;
                }
                Container from = container != null ? container : owner.getInventory();
                if (!takeOne(from, item)) {
                    owner.sendSystemMessage(Component.literal("Out of %s.".formatted(state.getBlock().getName().getString())));
                    return false;
                }
                worker.carrying = true;
                worker.stalled = 0;
                worker.bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                return true;
            }

            if (!moveTo(worker.bot, Vec3.atCenterOf(worker.target).add(0.0, 1.0, 0.0))) {
                return true;
            }
            // Someone may have filled the spot in the meantime; the ghost goes either way.
            if (level.getBlockState(worker.target).canBeReplaced()) {
                level.setBlockAndUpdate(worker.target, state);
                level.playSound(null, worker.target, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                worker.carrying = false;
                worker.bot.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            ghosts.remove(worker.target).discard();
            worker.target = null;
            worker.stalled = 0;
            return true;
        }

        /// The first ghost no other bot on this job has taken.
        private BlockPos nextTarget() {
            return ghosts.keySet().stream()
                    .filter(pos -> workers.stream().noneMatch(other -> pos.equals(other.target)))
                    .findFirst()
                    .orElse(null);
        }

        /// The chosen container, as long as it still exists and still has the block.
        private Container container() {
            return source != null && level.getBlockEntity(source) instanceof Container container && container.countItem(item) > 0
                    ? container
                    : null;
        }

        /// A bot is a crafted, placed thing: it keeps whatever it was carrying out of the job and
        /// stays put, waiting for the next outline.
        private void release(Worker worker) {
            if (worker.carrying) {
                worker.bot.spawnAtLocation(level, new ItemStack(item));
                worker.bot.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                worker.carrying = false;
            }
            worker.bot.getNavigation().stop();
            workers.remove(worker);
        }

        void cancel() {
            ghosts.values().forEach(Display.BlockDisplay::discard);
            ghosts.clear();
            List.copyOf(workers).forEach(this::release);
        }
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
