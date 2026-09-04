package dev.explorercraft.grapplinghook.mixin;

import dev.explorercraft.grapplinghook.physics.persistence.HookPersistenceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 saves entities through ValueOutput/ValueInput instead of a raw CompoundTag. The hook
 * persistence format is unchanged: the whole blob rides along as one codec-stored compound.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerPersistenceMixin {

    private static final String KEY = "grapplinghook:hooks";

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void grapplinghook$saveHooks(ValueOutput output, CallbackInfo ci) {
        CompoundTag tag = new CompoundTag();
        HookPersistenceManager.saveForPlayer((ServerPlayer) (Object) this, tag);
        if (!tag.isEmpty()) output.store(KEY, CompoundTag.CODEC, tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void grapplinghook$loadHooks(ValueInput input, CallbackInfo ci) {
        HookPersistenceManager.loadForPlayer((ServerPlayer) (Object) this,
                input.read(KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }
}
