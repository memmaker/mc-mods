package com.explorercraft.seamlesscrafting.mixin;

import com.explorercraft.seamlesscrafting.client.EffectStrip;
import com.explorercraft.seamlesscrafting.client.NearbyPanel;
import com.explorercraft.seamlesscrafting.client.NearbyPanelHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> implements NearbyPanelHolder {
	@Unique
	private final NearbyPanel seamless$panel = new NearbyPanel();

	private InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
		super(menu, recipeBook, inventory, title);
	}

	@Override
	public NearbyPanel seamless$getNearbyPanel() {
		return this.seamless$panel;
	}

	/** Always beside the screen: overlapping the slots would be worse than running off a narrow window. */
	@Unique
	private int seamless$panelX() {
		return this.leftPos + this.imageWidth + 6;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void seamless$initNearbyPanel(CallbackInfo ci) {
		this.seamless$panel.init(this.font, this.seamless$panelX(), this.topPos, this::addRenderableWidget, null);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void seamless$drawNearbyPanel(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		this.seamless$panel.draw(extractor, this.font, this.seamless$panelX(), this.topPos, mouseX, mouseY);
		EffectStrip.draw(extractor, this.minecraft, this.font, mouseX, mouseY);
	}
}
