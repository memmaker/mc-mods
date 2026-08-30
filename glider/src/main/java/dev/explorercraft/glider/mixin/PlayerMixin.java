package dev.explorercraft.glider.mixin;

import dev.explorercraft.glider.GliderMod;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The whole glider. Runs on both sides — client and server each decide independently from state
 * they both have, so the two agree without a single custom packet.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Unique private boolean glider$gliding;
    @Unique private double glider$fallen;
    @Unique private double glider$prevY;
    @Unique private ItemStack glider$flagged = ItemStack.EMPTY;

    /**
     * TAIL, not HEAD: travel() has already moved the player for this tick and left deltaMovement
     * holding the velocity the *next* move() will use, with gravity for that tick already folded
     * in. Clamping here therefore pins the actual descent to GLIDE_FALL_SPEED per tick. Clamping
     * at HEAD would let travel() re-apply gravity before moving and the player would sink roughly
     * three times as fast.
     */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void glider$tick(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Tracked here instead of read off Entity.fallDistance, which vanilla and other mods both
        // reset mid-fall for their own reasons — and which this mixin itself zeroes below.
        if (player.onGround() || player.getY() > glider$prevY) {
            glider$fallen = 0;
        } else {
            glider$fallen += glider$prevY - player.getY();
        }
        glider$prevY = player.getY();

        ItemStack main = player.getMainHandItem();
        boolean canGlide = !player.onGround()
                && !player.isFallFlying()
                && !player.getAbilities().flying
                && player.getVehicle() == null
                && !player.isInWater()
                && main.is(GliderMod.GLIDER);
        // Once open, the canopy stays open until the player lands or stows it. Re-testing the fall
        // distance every tick would snap it shut the instant anything nudged the player upward.
        glider$gliding = canGlide
                && (glider$gliding || glider$fallen >= GliderMod.DEPLOY_FALL_DISTANCE);

        if (glider$gliding) {
            player.fallDistance = 0;
            Vec3 m = player.getDeltaMovement();
            if (m.y < GliderMod.GLIDE_FALL_SPEED) {
                player.setDeltaMovement(m.x, GliderMod.GLIDE_FALL_SPEED, m.z);
            }
        }

        if (!player.level().isClientSide()) {
            glider$markStack(main);
        }
    }

    /**
     * Server side only, so the client never fights the sync. Held-slot changes are followed
     * explicitly: a player who swaps away mid-glide would otherwise leave a stack stuck in the
     * open-canopy pose forever.
     */
    @Unique
    private void glider$markStack(ItemStack main) {
        if (glider$flagged != main && !glider$flagged.isEmpty()) {
            glider$flagged.remove(GliderMod.GLIDING);
            glider$flagged = ItemStack.EMPTY;
        }
        if (main.isEmpty()) return;
        if (glider$gliding) {
            main.set(GliderMod.GLIDING, Unit.INSTANCE);
            glider$flagged = main;
        } else if (main.has(GliderMod.GLIDING)) {
            main.remove(GliderMod.GLIDING);
            glider$flagged = ItemStack.EMPTY;
        }
    }

    /**
     * getFrictionInfluencedSpeed() reaches for this whenever the player is off the ground, so
     * overriding it is what buys steering authority under the canopy instead of a dead drop.
     */
    @Inject(method = "getFlyingSpeed()F", at = @At("HEAD"), cancellable = true)
    private void glider$airControl(CallbackInfoReturnable<Float> cir) {
        if (glider$gliding) {
            cir.setReturnValue(GliderMod.GLIDE_AIR_SPEED);
        }
    }
}
