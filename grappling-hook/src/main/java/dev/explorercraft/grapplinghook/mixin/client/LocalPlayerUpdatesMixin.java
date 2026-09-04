package dev.explorercraft.grapplinghook.mixin.client;

import com.mojang.authlib.GameProfile;
import dev.explorercraft.grapplinghook.client.GrappleModClient;
import dev.explorercraft.grapplinghook.client.physics.controller.GrapplingHookPhysicsController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public abstract class LocalPlayerUpdatesMixin {

    @Shadow public abstract GameProfile getProfile();

    @Inject(method = "setGameMode(Lnet/minecraft/world/level/GameType;)V", at = @At("TAIL"))
    private void onGamemodeChange(GameType gameMode, CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;

        if(level == null) return;

        if(gameMode == GameType.SPECTATOR){
            UUID profile = this.getProfile().id();
            Player p = level.getPlayerByUUID(profile);

            if(p == null) return;

            int id = p.getId();

            GrapplingHookPhysicsController controller = GrappleModClient.get()
                    .getClientControllerManager()
                    .getController(id);

            if(controller != null)
                controller.disable();
        }
    }

}
