package dev.explorercraft.stealthandalert;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StealthAndAlert implements ModInitializer {
    public static final String MOD_ID = "stealthandalert";

    /// What a mob knows. Synced to everyone tracking it, so the HUD can read it client-side.
    public static final AttachmentType<AlertData> ALERT = AttachmentRegistry.<AlertData>builder()
            .initializer(AlertData::createDefault)
            .persistent(AlertData.CODEC)
            .syncWith(AlertData.STREAM_CODEC.cast(), AttachmentSyncPredicate.all())
            .buildAndRegister(id("alert"));

    /// Server-only bookkeeping for a mob walking its search pattern.
    public static final AttachmentType<SearchData> SEARCH = AttachmentRegistry.<SearchData>builder()
            .initializer(() -> SearchData.DEFAULT)
            .persistent(SearchData.CODEC)
            .buildAndRegister(id("search"));

    /// The loudest thing a mob heard this tick. Server-side and transient.
    public static final AttachmentType<AlertSoundData> SOUND = AttachmentRegistry.<AlertSoundData>builder()
            .initializer(() -> AlertSoundData.NONE)
            .buildAndRegister(id("sound"));

    /// The player's own visibility, recomputed each tick and sent to them for the HUD eye.
    public static final AttachmentType<Double> VISIBILITY = AttachmentRegistry.<Double>builder()
            .initializer(() -> 1.0)
            .syncWith(ByteBufCodecs.DOUBLE.cast(), AttachmentSyncPredicate.targetOnly())
            .buildAndRegister(id("visibility"));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static double visibilityOf(Player player) {
        return player == null ? 1.0 : player.getAttachedOrCreate(VISIBILITY);
    }

    @Override
    public void onInitialize() {
        StealthItems.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
            output.accept(StealthItems.DAGGER);
            output.accept(StealthItems.PEBBLE);
        });

        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (Player player : level.players()) {
                player.setAttached(VISIBILITY, Perception.visibility(player));
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> onHurt(entity, source.getEntity(), taken));
    }

    /// A mob that gets hit knows something is out there, even if it never saw who.
    private static void onHurt(LivingEntity entity, Entity attacker, float damageDealt) {
        if (!(attacker instanceof Player player) || player.isCreative() || player.isSpectator()) return;
        if (Perception.isPlayerPet(entity, player)) return;

        boolean seeker = StealthTags.is(entity, StealthTags.SEEKERS);
        boolean protectedMob = StealthTags.is(entity, StealthTags.PROTECTED);
        if (!seeker && !protectedMob) return;

        UUID uuid = player.getUUID();
        AlertData data = entity.getAttachedOrCreate(ALERT);

        // Checked before the alert data below is rewritten: it has to see the mob as it was.
        Assassination.tryAfterHit(entity, player, damageDealt, data);
        Map<UUID, Integer> reactions = new HashMap<>(data.targetReactionTicks());
        Map<UUID, Integer> memories = new HashMap<>(data.targetMemoryTicks());
        memories.put(uuid, StealthConfig.MEMORY_TICKS);

        int state = data.state();
        Optional<Vec3> lkp = data.lastKnownPos();

        if (seeker && data.stateOf(uuid) < AlertData.TRACKING) {
            reactions.put(uuid, 0);
            // Unless it is already busy fighting someone else, it turns on the attacker's position.
            boolean fightingSomeoneElse = data.state() == AlertData.FIGHTING
                    && entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget() != player;
            if (!fightingSomeoneElse) {
                state = AlertData.SEARCHING;
                lkp = Optional.of(player.position());
            }
        }

        entity.setAttached(ALERT, new AlertData(state, data.targetAwareness(), data.targetStates(), reactions, memories,
                lkp, data.primaryTarget(), data.willFighting() ? data.stateChangeTicks() : 0, data.patienceTicks(),
                data.canSeeAnyone(), data.willFighting()));
        entity.setAttached(SEARCH, SearchData.DEFAULT);
    }

}
