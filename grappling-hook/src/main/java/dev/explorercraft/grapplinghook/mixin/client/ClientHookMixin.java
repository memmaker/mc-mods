package dev.explorercraft.grapplinghook.mixin.client;

import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.client.ModKeys;
import dev.explorercraft.grapplinghook.client.physics.ClientPhysicsControllerTracker;
import dev.explorercraft.grapplinghook.config.GrappleModCommonConfig;
import dev.explorercraft.grapplinghook.content.item.type.IGlobalKeyObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientHookMixin {

    @Unique
    private static final boolean[] keyPressHistory = new boolean[]{ false, false, false, false, false };


    @Inject(method = "tick()V", at = @At("TAIL"))
    public void clientTickHook(CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;

        if (player == null ||  Minecraft.getInstance().isPaused())
            return;

        ClientPhysicsControllerTracker physManager = GrappleModClient.get().getClientControllerManager();
        physManager.onClientTick(player);

        // Controls should only apply when there is no menu visible. 26.2 no longer exposes the
        // current screen on Minecraft; a grabbed mouse means the player is in the world.
        if (!Minecraft.getInstance().mouseHandler.isMouseGrabbed())
            return;

        // keep in same order as enum from KeypressItem
        boolean[] keys = {
                ModKeys.HOOK_ENDER_LAUNCH.get().isDown(), ModKeys.THROW_OFF_HOOK.isDown(),
                ModKeys.THROW_MAIN_HOOK.isDown(), ModKeys.THROW_HOOKS.get().isDown(),
                ModKeys.ROCKET.get().isDown()
        };

        for (int i = 0; i < keys.length; i++) {
            boolean isKeyDown = keys[i];
            boolean prevKey = ClientHookMixin.keyPressHistory[i];

            if (isKeyDown != prevKey) {
                IGlobalKeyObserver.Keys key = IGlobalKeyObserver.Keys.values()[i];

                ItemStack stack = this.getKeypressStack(player);
                if (stack != null) {
                    if (isKeyDown) {
                        ((IGlobalKeyObserver) stack.getItem()).onCustomKeyDown(stack, player, key, true);
                    } else {
                        ((IGlobalKeyObserver) stack.getItem()).onCustomKeyUp(stack, player, key, true);
                    }
                }
            }

            ClientHookMixin.keyPressHistory[i] = isKeyDown;
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    public void handleLogOut(Screen nextScreen, boolean keepResourcePacks, CallbackInfo ci) {
        GrappleModCommonConfig.resetConfigFromServer();
    }


    @Unique
    private ItemStack getKeypressStack(Player player) {
        if (player == null) return null;

        ItemStack stack;

        stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof IGlobalKeyObserver) return stack;

        stack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (stack.getItem() instanceof IGlobalKeyObserver) return stack;

        return null;
    }
}
