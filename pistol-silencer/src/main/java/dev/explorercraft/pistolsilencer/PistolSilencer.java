package dev.explorercraft.pistolsilencer;

import dev.explorercraft.pistolsilencer.item.PistolItem;
import dev.explorercraft.pistolsilencer.item.SilencerItem;
import dev.explorercraft.pistolsilencer.network.HeadBoxHintPayload;
import dev.explorercraft.pistolsilencer.network.ReloadPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public class PistolSilencer implements ModInitializer {
    public static final String MOD_ID = "pistolsilencer";

    /** Ammo remaining in a pistol stack. Whether a silencer is attached is stored via the vanilla custom_model_data component so item model selection can read it natively. */
    public static final DataComponentType<Integer> AMMO = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, id("ammo"),
            DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final Item PISTOL = new PistolItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id("pistol")))
            .stacksTo(1)
            .component(AMMO, PistolItem.MAX_AMMO));

    public static final Item SILENCER = new SilencerItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id("silencer"))));

    public static final Item PISTOL_AMMO = new Item(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id("pistol_ammo"))));

    public static final SoundEvent FIRE = SoundEvent.createVariableRangeEvent(id("item.pistol.fire"));
    public static final SoundEvent SILENCED_FIRE = SoundEvent.createVariableRangeEvent(id("item.pistol.silenced_fire"));
    public static final SoundEvent RELOAD = SoundEvent.createVariableRangeEvent(id("item.pistol.reload"));
    public static final SoundEvent COCK = SoundEvent.createVariableRangeEvent(id("item.pistol.cock"));

    private static final ResourceKey<CreativeModeTab> COMBAT_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, id("pistol"), PISTOL);
        Registry.register(BuiltInRegistries.ITEM, id("silencer"), SILENCER);
        Registry.register(BuiltInRegistries.ITEM, id("pistol_ammo"), PISTOL_AMMO);

        Registry.register(BuiltInRegistries.SOUND_EVENT, id("item.pistol.fire"), FIRE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("item.pistol.silenced_fire"), SILENCED_FIRE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("item.pistol.reload"), RELOAD);
        Registry.register(BuiltInRegistries.SOUND_EVENT, id("item.pistol.cock"), COCK);

        CreativeModeTabEvents.modifyOutputEvent(COMBAT_TAB).register(output -> {
            output.accept(PISTOL);
            output.accept(SILENCER);
            output.accept(PISTOL_AMMO);
        });

        PayloadTypeRegistry.serverboundPlay().register(ReloadPayload.TYPE, ReloadPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReloadPayload.TYPE, (payload, context) -> PistolItem.reload(context.player()));

        PayloadTypeRegistry.serverboundPlay().register(HeadBoxHintPayload.TYPE, HeadBoxHintPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HeadBoxHintPayload.TYPE,
                (payload, context) -> HeadBoxHints.record(context.player().getUUID(), payload));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
