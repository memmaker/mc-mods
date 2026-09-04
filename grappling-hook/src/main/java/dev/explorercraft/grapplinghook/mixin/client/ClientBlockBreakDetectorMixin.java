package dev.explorercraft.grapplinghook.mixin.client;

import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.client.api.GrappleModClientEvents;
import dev.explorercraft.grapplinghook.client.physics.ClientPhysicsControllerTracker;
import dev.explorercraft.grapplinghook.client.physics.controller.GrapplingHookPhysicsController;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientBlockBreakDetectorMixin {

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    public void handleBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()) return;
        if (pos == null) return;

        ClientPhysicsControllerTracker physManager = GrappleModClient.get().getClientControllerManager();

        if (physManager.controllerPos.containsKey(pos)) {
            GrapplingHookPhysicsController control = physManager.controllerPos.get(pos);
            control.disable();
            physManager.controllerPos.remove(pos);

            GrappleModClientEvents.HOOK_DETACH.invoker().onHookDetach(control.holder);
        }
    }

}
