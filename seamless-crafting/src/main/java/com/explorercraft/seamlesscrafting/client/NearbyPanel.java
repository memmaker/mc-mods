package com.explorercraft.seamlesscrafting.client;

import com.explorercraft.seamlesscrafting.NearbyInventoryScanner.NearbyItemEntry;
import com.explorercraft.seamlesscrafting.SeamlessCraftingConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The nearby-items panel drawn beside the crafting and inventory screens. Both screens use
 * one instance of this and forward their input to it.
 */
public final class NearbyPanel {
	private static final int COLUMNS = 4;
	private static final int ROWS = 6;
	private static final int SLOT_SIZE = 21;
	public static final int WIDTH = COLUMNS * SLOT_SIZE + 6;
	private static final int PANEL_HEIGHT = ROWS * SLOT_SIZE + 16;
	private static final int PANEL_OFFSET_Y = 48;
	private static final int PULSE_TICKS = 6;

	private boolean open = true;
	private int scrollOffset;
	private int originX;
	private int originY;
	private int lastClickIndex = -1;
	private long lastClickTime = -1000L;
	@Nullable
	private Button nearbyButton;
	@Nullable
	private Button returnButton;
	@Nullable
	private EditBox searchBox;

	public void init(Font font, int originX, int originY, Consumer<AbstractWidget> adder, @Nullable Runnable onReturnItems) {
		NearbyItemsClientState.clear();
		this.open = SeamlessCraftingConfig.isNearbyPanelOpenByDefault();
		this.scrollOffset = 0;
		this.originX = originX;
		this.originY = originY;

		this.nearbyButton = Button.builder(Component.translatable("screen.seamlesscrafting.nearby"), button -> {
			this.open = !this.open;
			if (this.open) {
				NearbyItemsClientState.requestUpdate();
			}
		}).bounds(originX, originY + 6, 60, 20).build();
		adder.accept(this.nearbyButton);

		if (onReturnItems != null) {
			this.returnButton = Button.builder(Component.literal("X"), button -> onReturnItems.run())
					.bounds(originX + 64, originY + 6, 20, 20)
					.tooltip(Tooltip.create(Component.translatable("screen.seamlesscrafting.return_items")))
					.build();
			adder.accept(this.returnButton);
		}

		this.searchBox = new EditBox(font, originX, originY + 30, 84, 14, Component.empty());
		this.searchBox.setMaxLength(50);
		this.searchBox.setHint(Component.translatable("screen.seamlesscrafting.search"));
		adder.accept(this.searchBox);

		NearbyItemsClientState.requestUpdate();
	}

	/** Screens move when the recipe book opens, so the widgets follow the origin every frame. */
	public void draw(GuiGraphicsExtractor extractor, Font font, int originX, int originY, int mouseX, int mouseY) {
		this.originX = originX;
		this.originY = originY;

		if (this.nearbyButton != null) {
			this.nearbyButton.setX(originX);
			this.nearbyButton.setY(originY + 6);
		}
		if (this.returnButton != null) {
			this.returnButton.setX(originX + 64);
			this.returnButton.setY(originY + 6);
		}
		if (this.searchBox != null) {
			this.searchBox.setX(originX);
			this.searchBox.setY(originY + 30);
			this.searchBox.setVisible(this.open);
		}
		if (!this.open) {
			return;
		}

		List<NearbyItemEntry> entries = this.filteredEntries();
		if (entries.isEmpty()) {
			return;
		}

		int panelX = originX;
		int panelY = originY + PANEL_OFFSET_Y;
		extractor.fill(panelX, panelY, panelX + WIDTH, panelY + PANEL_HEIGHT, 0x88000000);
		extractor.text(font, Component.translatable("screen.seamlesscrafting.nearby"), panelX + 4, panelY + 4, 0xFFFFFFFF);

		int startX = panelX + 3;
		int startY = panelY + 14;
		int totalRows = Mth.ceil(entries.size() / (double)COLUMNS);
		this.scrollOffset = Mth.clamp(this.scrollOffset, 0, Math.max(0, totalRows - ROWS));

		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				int slotX = startX + column * SLOT_SIZE;
				int slotY = startY + row * SLOT_SIZE;
				extractor.fill(slotX, slotY, slotX + 18, slotY + 18, 0x55000000);
				extractor.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x2A000000);
				extractor.fill(slotX, slotY, slotX + 18, slotY + 1, 0x66FFFFFF);
				extractor.fill(slotX, slotY, slotX + 1, slotY + 18, 0x66FFFFFF);
				extractor.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0x33000000);
				extractor.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0x33000000);
			}
		}

		int startIndex = this.scrollOffset * COLUMNS;
		int endIndex = Math.min(entries.size(), startIndex + COLUMNS * ROWS);
		for (int index = startIndex; index < endIndex; index++) {
			int gridIndex = index - startIndex;
			int itemX = startX + (gridIndex % COLUMNS) * SLOT_SIZE + 2;
			int itemY = startY + (gridIndex / COLUMNS) * SLOT_SIZE + 1;
			NearbyItemEntry entry = entries.get(index);
			extractor.item(entry.stack(), itemX, itemY);
			extractor.itemDecorations(font, entry.stack(), itemX, itemY, formatCount(entry.count()));
		}

		int hoveredIndex = this.hoveredIndex(mouseX, mouseY, entries.size());
		if (hoveredIndex >= 0) {
			extractor.setTooltipForNextFrame(font, entries.get(hoveredIndex).stack(), mouseX, mouseY);
		}

		this.drawClickPulse(extractor, entries.size());
	}

	public boolean mouseClicked(MouseButtonEvent event) {
		if (!this.open || event.button() != 0 || !this.isOverPanel(event.x(), event.y())) {
			return false;
		}

		List<NearbyItemEntry> entries = this.filteredEntries();
		int index = this.hoveredIndex(event.x(), event.y(), entries.size());
		if (index < 0) {
			return false;
		}

		NearbyItemsClientState.requestHighlight(entries.get(index).stack());
		this.lastClickIndex = index;
		this.lastClickTime = levelTime();
		Minecraft.getInstance().getSoundManager().play(
				net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
		Minecraft.getInstance().gui.screen().onClose();
		return true;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
		if (!this.open || !this.isOverPanel(mouseX, mouseY)) {
			return false;
		}

		this.scrollOffset += verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
		return true;
	}

	public boolean charTyped(CharacterEvent event) {
		if (this.open && this.searchBox != null && this.searchBox.charTyped(event)) {
			this.scrollOffset = 0;
			return true;
		}
		return false;
	}

	public boolean keyPressed(KeyEvent event) {
		if (this.open && this.searchBox != null && this.searchBox.keyPressed(event)) {
			this.scrollOffset = 0;
			return true;
		}
		return false;
	}

	private boolean isOverPanel(double mouseX, double mouseY) {
		int panelY = this.originY + PANEL_OFFSET_Y;
		return mouseX >= this.originX && mouseX <= this.originX + WIDTH
				&& mouseY >= panelY && mouseY <= panelY + PANEL_HEIGHT;
	}

	private int hoveredIndex(double mouseX, double mouseY, int totalEntries) {
		int startX = this.originX + 3;
		int startY = this.originY + PANEL_OFFSET_Y + 14;
		int relativeX = (int)mouseX - startX;
		int relativeY = (int)mouseY - startY;
		if (relativeX < 0 || relativeY < 0) {
			return -1;
		}

		int column = relativeX / SLOT_SIZE;
		int row = relativeY / SLOT_SIZE;
		if (column >= COLUMNS || row >= ROWS) {
			return -1;
		}
		if (mouseX > startX + column * SLOT_SIZE + 18 || mouseY > startY + row * SLOT_SIZE + 18) {
			return -1;
		}

		int index = (this.scrollOffset + row) * COLUMNS + column;
		return index >= 0 && index < totalEntries ? index : -1;
	}

	private void drawClickPulse(GuiGraphicsExtractor extractor, int totalEntries) {
		if (!SeamlessCraftingConfig.isHighlightEnabled() || this.lastClickIndex < 0 || this.lastClickIndex >= totalEntries) {
			return;
		}

		long age = levelTime() - this.lastClickTime;
		if (age < 0 || age > PULSE_TICKS) {
			return;
		}

		int localIndex = this.lastClickIndex - this.scrollOffset * COLUMNS;
		if (localIndex < 0 || localIndex >= COLUMNS * ROWS) {
			return;
		}

		int slotX = this.originX + 3 + (localIndex % COLUMNS) * SLOT_SIZE;
		int slotY = this.originY + PANEL_OFFSET_Y + 14 + (localIndex / COLUMNS) * SLOT_SIZE;
		int alpha = Mth.clamp((int)(160 * (1.0f - age / (float)PULSE_TICKS)), 0, 160);
		extractor.fill(slotX, slotY, slotX + 18, slotY + 18, ARGB.color(alpha, SeamlessCraftingConfig.getHighlightColor()));
	}

	private List<NearbyItemEntry> filteredEntries() {
		String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
		List<NearbyItemEntry> filtered = new ArrayList<>();
		for (NearbyItemEntry entry : NearbyItemsClientState.getEntries()) {
			if (query.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
				filtered.add(entry);
			}
		}

		filtered.sort(Comparator
				.comparingInt((NearbyItemEntry entry) -> categoryRank(entry.stack()))
				.thenComparing(entry -> entry.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
		return filtered;
	}

	private static long levelTime() {
		return Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
	}

	private static int categoryRank(ItemStack stack) {
		if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)) {
			return 0;
		}
		if (stack.is(ItemTags.COAL_ORES)
				|| stack.is(ItemTags.IRON_ORES)
				|| stack.is(ItemTags.COPPER_ORES)
				|| stack.is(ItemTags.GOLD_ORES)
				|| stack.is(ItemTags.REDSTONE_ORES)
				|| stack.is(ItemTags.LAPIS_ORES)
				|| stack.is(ItemTags.DIAMOND_ORES)
				|| stack.is(ItemTags.EMERALD_ORES)) {
			return 1;
		}
		if (stack.get(DataComponents.FOOD) != null) {
			return 2;
		}
		return 3;
	}

	private static String formatCount(int count) {
		if (count < 1_000) {
			return String.valueOf(count);
		}
		if (count < 1_000_000) {
			return (count / 1_000) + "k";
		}
		if (count < 1_000_000_000) {
			return (count / 1_000_000) + "M";
		}
		return (count / 1_000_000_000) + "B";
	}
}
