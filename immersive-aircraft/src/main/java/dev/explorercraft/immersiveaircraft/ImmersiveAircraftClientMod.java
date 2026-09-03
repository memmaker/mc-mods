package dev.explorercraft.immersiveaircraft;

import dev.explorercraft.immersiveaircraft.client.KeyBindings;
import dev.explorercraft.immersiveaircraft.client.OverlayRenderer;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleStat;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgrade;
import dev.explorercraft.immersiveaircraft.item.upgrade.VehicleUpgradeRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ImmersiveAircraftClientMod implements ClientModInitializer {
    private final DecimalFormat fmt = new DecimalFormat("+#;-#");

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(event -> ClientMain.postLoad());

        ClientTickEvents.START_CLIENT_TICK.register(event -> ClientMain.tick());

        Renderer.bootstrap();
        WeaponRendererRegistry.bootstrap();
        // ponytail: ItemColors/ColorProviderRegistry dropped, ColorProviderRegistry+ItemColor no
        // longer exist on this MC version (item tinting moved to data-driven item model tint
        // sources). Dyed items render untinted until that's ported. See unported-client-rendering/.

        KeyBindings.list.forEach(KeyMappingHelper::registerKeyMapping);
        ItemTooltipCallback.EVENT.register(this::itemTooltipCallback);

        // Flight instrument overlay (speed/altitude/attitude/compass/warnings), drawn whenever
        // the player is riding one of our vehicles. Attached after the vanilla mount health bar
        // (matching the old renderVehicleHealth mixin, which also ran alongside vanilla's bar
        // rather than replacing it) so it doesn't stomp riding-a-horse/boat rendering.
        HudElementRegistry.attachElementAfter(VanillaHudElements.MOUNT_HEALTH,
                Main.locate("flight_overlay"),
                (graphics, deltaTracker) -> OverlayRenderer.renderOverlay(graphics, deltaTracker.getGameTimeDeltaPartialTick(false), 49));
    }

    private void itemTooltipCallback(ItemStack stack, net.minecraft.world.item.Item.TooltipContext tooltipContext, TooltipFlag context, List<Component> tooltip) {
        VehicleUpgrade upgrade = VehicleUpgradeRegistry.INSTANCE.getUpgrade(stack.getItem());
        if (upgrade != null) {
            tooltip.add(Component.translatable("item.immersiveaircraft.item.upgrade").withStyle(ChatFormatting.GRAY));

            for (Map.Entry<VehicleStat, Float> entry : upgrade.getAll().entrySet()) {
                tooltip.add(Component.translatable("immersiveaircraft.upgrade." + entry.getKey().name().toLowerCase(Locale.ROOT),
                        fmt.format(entry.getValue() * 100)
                ).withStyle(entry.getValue() * (entry.getKey().positive() ? 1 : -1) > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
    }
}
