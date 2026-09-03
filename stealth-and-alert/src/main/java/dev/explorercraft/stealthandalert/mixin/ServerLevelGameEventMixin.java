package dev.explorercraft.stealthandalert.mixin;

import dev.explorercraft.stealthandalert.Acoustics;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Every noise the player makes already passes through here on its way to sculk sensors.
@Mixin(ServerLevel.class)
public abstract class ServerLevelGameEventMixin {
    @Inject(method = "gameEvent", at = @At("HEAD"))
    private void stealthandalert$hearNoise(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        Acoustics.onGameEvent((ServerLevel) (Object) this, event, pos, context.sourceEntity());
    }
}
