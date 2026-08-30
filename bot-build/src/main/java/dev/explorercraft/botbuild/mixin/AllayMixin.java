package dev.explorercraft.botbuild.mixin;

import dev.explorercraft.botbuild.BotBuild;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// A build bot is an allay with its brain switched off — it wanders and begs for items otherwise.
/// The rest of the mob keeps ticking, which is what leaves it a working pathfinder to walk with.
/// This hook doubles as the bot's own per-tick slot: a bot with no outline to work goes looking
/// for dropped food.
@Mixin(Allay.class)
public abstract class AllayMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void botbuild$skipBrainForBots(ServerLevel level, CallbackInfo callbackInfo) {
        Allay allay = (Allay) (Object) this;
        if (allay.entityTags().contains(BotBuild.MOD_ID)) {
            callbackInfo.cancel();
            BotBuild.idleTick(level, allay);
        }
    }
}
