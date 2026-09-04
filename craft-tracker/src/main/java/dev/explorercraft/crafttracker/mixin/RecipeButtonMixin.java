package dev.explorercraft.crafttracker.mixin;

import dev.explorercraft.crafttracker.HoveredRecipe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// The recipe book keeps its hovered button private and hands out no getter, so each button
/// reports itself while it draws.
@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void craftTracker$trackHover(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RecipeButton self = (RecipeButton) (Object) this;
        ItemStack stack = self.getDisplayStack();
        if (self.isHovered() && !stack.isEmpty()) {
            HoveredRecipe.set(stack.getItem());
        } else {
            HoveredRecipe.clearIf(stack.getItem());
        }
    }
}
