package com.explorercraft.seamlesscrafting.client;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

/**
 * Active effects as a row of icons along the top left. Vanilla draws them in the space right of
 * the inventory, which is where the nearby panel lives, so they move out of its way.
 */
public final class EffectStrip {
	private static final int ICON_SIZE = 18;
	private static final int MARGIN = 3;
	private static final int STEP = ICON_SIZE + 4;

	private EffectStrip() {
	}

	public static void draw(GuiGraphicsExtractor extractor, Minecraft minecraft, Font font, int mouseX, int mouseY) {
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}

		int x = MARGIN;
		for (MobEffectInstance effect : minecraft.player.getActiveEffects().stream().sorted().toList()) {
			extractor.fill(x - 1, MARGIN - 1, x + ICON_SIZE + 1, MARGIN + ICON_SIZE + 1,
					effect.isAmbient() ? 0x55FFFFFF : 0x88000000);
			extractor.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()),
					x, MARGIN, ICON_SIZE, ICON_SIZE);

			if (mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= MARGIN && mouseY < MARGIN + ICON_SIZE) {
				extractor.setTooltipForNextFrame(font, List.of(name(effect), duration(minecraft, effect)),
						Optional.empty(), mouseX, mouseY);
			}

			x += STEP;
		}
	}

	private static Component name(MobEffectInstance effect) {
		MutableComponent name = effect.getEffect().value().getDisplayName().copy();
		int amplifier = effect.getAmplifier();
		if (amplifier >= 1 && amplifier <= 9) {
			name.append(CommonComponents.SPACE).append(Component.translatable("potion.potency." + amplifier));
		}
		return name;
	}

	private static Component duration(Minecraft minecraft, MobEffectInstance effect) {
		return MobEffectUtil.formatDuration(effect, 1.0f, minecraft.level.tickRateManager().tickrate());
	}
}
