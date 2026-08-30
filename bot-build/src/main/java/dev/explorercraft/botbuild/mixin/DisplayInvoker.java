package dev.explorercraft.botbuild.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/// Display entities are only configurable through NBT in vanilla; every setter behind that is
/// private. Opening these up is what lets a ghost be spawned and shrunk from code.
@Mixin(Display.class)
public interface DisplayInvoker {
    @Invoker("setTransformation")
    void botbuild$setTransformation(Transformation transformation);
}
