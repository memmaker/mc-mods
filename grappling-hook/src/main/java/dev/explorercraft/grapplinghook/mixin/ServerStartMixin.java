package dev.explorercraft.grapplinghook.mixin;

import dev.explorercraft.grapplinghook.config.GrappleModCommonConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grappling looks like flying to the server's flight check. 26.2 dropped
 * MinecraftServer.setFlightAllowed, so the config overrides the getter instead of writing the
 * setting once at startup — same effect, and it now also survives a settings reload.
 */
@Mixin(MinecraftServer.class)
public abstract class ServerStartMixin {

    @Inject(method = "allowFlight()Z", at = @At("HEAD"), cancellable = true)
    public void grapplinghook$forceAllowFlight(CallbackInfoReturnable<Boolean> cir) {
        if (GrappleModCommonConfig.get().forceAllowFlight()) {
            cir.setReturnValue(true);
        }
    }
}
