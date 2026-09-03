package dev.explorercraft.pistolsilencer.item;

import dev.explorercraft.pistolsilencer.FxGlobalsIntegration;
import dev.explorercraft.pistolsilencer.HeadBoxHints;
import dev.explorercraft.pistolsilencer.PistolSilencer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/**
 * Stats hardcoded from CGM's {@code data/cgm/guns/pistol.json} rather than ported through its
 * JSON-datapack gun system, since only this one gun exists here.
 *
 * Shooting is a hitscan raycast rather than a spawned projectile entity: the original bullet
 * travels 10 blocks/tick for 25 ticks (250 blocks), which is indistinguishable from instant at
 * normal engagement ranges, so a raycast is a faithful and far simpler stand-in.
 */
public class PistolItem extends Item {
    public static final int MAX_AMMO = 16;
    public static final int FIRE_RATE_TICKS = 4;
    public static final int RELOAD_AMOUNT = MAX_AMMO;
    public static final float DAMAGE = 9.0F;
    public static final float RANGE = 60.0F;
    public static final float SPREAD_DEGREES = 1.0F;
    public static final float RECOIL_ANGLE = 2.5F;

    public PistolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            return toggleSilencer(player, stack) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        fire((ServerLevel) level, player, stack);
        return InteractionResult.CONSUME;
    }

    private boolean toggleSilencer(Player player, ItemStack pistol) {
        if (isSilenced(pistol)) {
            setSilenced(pistol, false);
            ItemStack silencer = new ItemStack(PistolSilencer.SILENCER);
            if (!player.getInventory().add(silencer)) {
                player.drop(silencer, false);
            }
            return true;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof SilencerItem) {
            offhand.shrink(1);
            setSilenced(pistol, true);
            return true;
        }
        return false;
    }

    private static void fire(ServerLevel level, Player shooter, ItemStack stack) {
        if (shooter.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        boolean unlimitedAmmo = shooter.getAbilities().instabuild;
        int ammo = stack.getOrDefault(PistolSilencer.AMMO, MAX_AMMO);
        if (ammo <= 0 && !unlimitedAmmo) {
            return;
        }
        if (!unlimitedAmmo) {
            stack.set(PistolSilencer.AMMO, ammo - 1);
        }
        shooter.getCooldowns().addCooldown(stack, FIRE_RATE_TICKS);

        RandomSource random = shooter.getRandom();
        float yaw = shooter.getYRot() + (random.nextFloat() - 0.5F) * SPREAD_DEGREES;
        float pitch = shooter.getXRot() + (random.nextFloat() - 0.5F) * SPREAD_DEGREES;
        Vec3 start = shooter.getEyePosition(1.0F);
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
        Vec3 end = start.add(direction.scale(RANGE));

        BlockHitResult blockHit = level.clipIncludingBorder(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        EntityHitResult entityHit = raycastEntity(shooter, start, limit);

        if (entityHit != null) {
            entityHit.getEntity().hurtServer(level, level.damageSources().playerAttack(shooter), DAMAGE);
            Vec3 hit = entityHit.getLocation();
            if (entityHit.getEntity() instanceof LivingEntity livingTarget) {
                AABB preciseHeadBox = HeadBoxHints.forTarget(shooter.getUUID(), livingTarget.getId());
                FxGlobalsIntegration.headshot(livingTarget, start, limit, preciseHeadBox);
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, hit.x, hit.y, hit.z, 6, 0.1, 0.1, 0.1, 0.1);
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            Vec3 hit = blockHit.getLocation();
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), hit.x, hit.y, hit.z,
                    10, 0.05, 0.05, 0.05, 0.1);
            level.playSound(null, blockHit.getBlockPos(), state.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 0.4F, 1.2F);
        }
        // Muzzle smoke, in place of CGM's custom flash-quad renderer.
        Vec3 muzzle = start.add(direction.scale(0.8));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.02, 0.02, 0.02, 0.01);

        // Recoil is sent as a relative rotation packet; a server-side setXRot would be overwritten
        // by the client's next movement packet.
        if (shooter instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundPlayerRotationPacket(0.0F, true, -RECOIL_ANGLE, true));
        }

        boolean silenced = isSilenced(stack);
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                silenced ? PistolSilencer.SILENCED_FIRE : PistolSilencer.FIRE,
                SoundSource.PLAYERS, silenced ? 0.6F : 1.5F, 1.0F);
    }

    /** The entity a ray from {@code start} to {@code end} would hit, ignoring the shooter — shared
     * so the client can identify the same target the server is about to shoot at. */
    public static EntityHitResult raycastEntity(Player shooter, Vec3 start, Vec3 end) {
        AABB searchBox = shooter.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        return ProjectileUtil.getEntityHitResult(shooter, start, end, searchBox,
                entity -> !entity.isSpectator() && entity.isPickable() && entity != shooter,
                start.distanceToSqr(end));
    }

    public static void reload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PistolItem)) {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof PistolItem)) {
            return;
        }
        int ammo = stack.getOrDefault(PistolSilencer.AMMO, MAX_AMMO);
        if (ammo >= MAX_AMMO) {
            return;
        }
        if (player.getAbilities().instabuild) {
            stack.set(PistolSilencer.AMMO, MAX_AMMO);
        } else {
            ItemStack ammoStack = findAmmo(player.getInventory());
            if (ammoStack.isEmpty()) {
                return;
            }
            int amount = Math.min(RELOAD_AMOUNT, MAX_AMMO - ammo);
            amount = Math.min(amount, ammoStack.getCount());
            if (amount <= 0) {
                return;
            }
            ammoStack.shrink(amount);
            stack.set(PistolSilencer.AMMO, ammo + amount);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                PistolSilencer.RELOAD, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static ItemStack findAmmo(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == PistolSilencer.PISTOL_AMMO) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }


    public static boolean isSilenced(ItemStack stack) {
        CustomModelData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
        return data != null && Boolean.TRUE.equals(data.getBoolean(0));
    }

    public static void setSilenced(ItemStack stack, boolean silenced) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(silenced), List.of(), List.of()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        int ammo = stack.getOrDefault(PistolSilencer.AMMO, MAX_AMMO);
        tooltip.accept(Component.translatable("item.pistolsilencer.pistol.ammo", ammo, MAX_AMMO));
        if (isSilenced(stack)) {
            tooltip.accept(Component.translatable("item.pistolsilencer.pistol.silenced"));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(PistolSilencer.AMMO, MAX_AMMO) != MAX_AMMO;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * stack.getOrDefault(PistolSilencer.AMMO, MAX_AMMO) / MAX_AMMO);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFCC33;
    }
}
