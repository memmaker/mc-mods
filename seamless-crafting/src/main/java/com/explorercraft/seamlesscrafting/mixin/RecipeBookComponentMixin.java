package com.explorercraft.seamlesscrafting.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	@Shadow
	@Nullable
	private RecipeBookTabButton selectedTab;

	@Shadow
	@Nullable
	private RecipeCollection lastRecipeCollection;

	@Shadow
	@Nullable
	private RecipeDisplayId lastRecipe;

	@Invoker("tryPlaceRecipe")
	protected abstract boolean seamless$invokeTryPlaceRecipe(RecipeCollection collection, RecipeDisplayId recipe, boolean craftAll);

	@Invoker("updateStackedContents")
	protected abstract void seamless$invokeUpdateStackedContents();

	/** The nearby payload arrives after the recipe book built its item counts, so rebuild them. */
	@Inject(method = "recipesUpdated", at = @At("HEAD"))
	private void seamless$refreshInputsForNearbyPayload(CallbackInfo ci) {
		// No tab yet means the book was never opened, and rebuilding its collections would
		// walk into a null tab.
		if (this.selectedTab != null) {
			this.seamless$invokeUpdateStackedContents();
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void seamless$spacebarPlacesOneSet(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (event.key() != GLFW.GLFW_KEY_SPACE || this.lastRecipeCollection == null || this.lastRecipe == null) {
			return;
		}

		AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
		cir.setReturnValue(this.seamless$invokeTryPlaceRecipe(this.lastRecipeCollection, this.lastRecipe, false));
	}
}
