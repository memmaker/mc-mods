package dev.explorercraft.grapplinghook.content.registry.internal;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.item.*;
import dev.explorercraft.grapplinghook.content.item.smithing.GrapplingHookUpgradeTemplateItem;
import dev.explorercraft.grapplinghook.content.item.smithing.LongFallBootsTemplateItem;
import dev.explorercraft.grapplinghook.content.registry.helper.AbstractRegistryReference;
import dev.explorercraft.grapplinghook.content.registry.helper.TabBuilder;
import dev.explorercraft.grapplinghook.content.customization.HookTemplates;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ModItems {

    private static final ArrayList<Identifier> itemsInRegistryOrder;
    private static final HashMap<Identifier, ItemEntry<?>> items;

    static {
        items = new HashMap<>();
        itemsInRegistryOrder = new ArrayList<>();
    }

    public static final ItemEntry<GrapplehookItem> GRAPPLING_HOOK = ModItems.item("grappling_hook", GrapplehookItem::new, ItemEntry.populateHookVariantsInTab());
    public static final ItemEntry<EnderStaffItem> ENDER_STAFF = ModItems.item("ender_staff", EnderStaffItem::new);
    public static final ItemEntry<ForcefieldItem> FORCE_FIELD = ModItems.item("forcefield", ForcefieldItem::new);
    public static final ItemEntry<RocketItem> ROCKET = ModItems.item("rocket", RocketItem::new);

    public static final ItemEntry<GrapplingHookUpgradeTemplateItem> BASE_UPGRADE = ModItems.item("base_upgrade", GrapplingHookUpgradeTemplateItem::new);

    public static final ItemEntry<LongFallBootsItem> LONG_FALL_BOOTS = ModItems.item("long_fall_boots", LongFallBootsItem::new);
    public static final ItemEntry<LongFallBootsTemplateItem> LONG_FALL_BOOTS_SMITHING_TEMPLATE = ModItems.item("long_fall_boots_smithing_template", LongFallBootsTemplateItem::new);

    private static final CreativeModeTab.DisplayItemsGenerator MOD_TAB_GENERATOR = (displayParameters, output) -> {

        List<ItemStack> creativeMenu = itemsInRegistryOrder.stream()
                .map(items::get)
                .map(ItemEntry::getTabProvider)
                .map(provider -> provider.build(displayParameters))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        output.acceptAll(creativeMenu);
    };

    private static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, GrappleMod.id("main"));

    private static final CreativeModeTab ITEM_GROUP = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.grapplinghook.main"))
            .icon(() -> new ItemStack(GRAPPLING_HOOK.get()))
            .displayItems(MOD_TAB_GENERATOR)
            .build();

    public static <I extends Item> ItemEntry<I> item(String id, Function<Item.Properties, I> item) {
        return item(id, item, null);
    }

    public static <I extends Item> ItemEntry<I> item(String id, Function<Item.Properties, I> item, TabBuilder tabProvider) {
        return item(id, item, tabProvider, false);
    }

    public static <I extends Item> ItemEntry<I> item(String id, Function<Item.Properties, I> item, TabBuilder tabProvider, boolean placeFirstInCreative) {
        Identifier qualId = GrappleMod.id(id);
        // 26.2 requires every item to carry its own registry key, handed in through Properties.
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, qualId);
        ItemEntry<I> entry = new ItemEntry<>(qualId, () -> item.apply(new Item.Properties().setId(key)), tabProvider);

        if(ModItems.items.containsKey(qualId))
            throw new IllegalStateException("Duplicate item registered");

        ModItems.items.put(qualId, entry);

        if(placeFirstInCreative) {
            ModItems.itemsInRegistryOrder.add(0, qualId);
        } else {
            ModItems.itemsInRegistryOrder.add(qualId);
        }

        return entry;
    }

    public static void registerAllItems() {
        for(Map.Entry<Identifier, ItemEntry<?>> def: items.entrySet()) {
            Identifier id = def.getKey();
            ItemEntry<?> data = def.getValue();
            Item it = data.getFactory().get();

            data.finalize(Registry.register(BuiltInRegistries.ITEM, id, it));
        }

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP_KEY, ITEM_GROUP);
    }



    public static class ItemEntry<I extends Item> extends AbstractRegistryReference<I> {

        protected TabBuilder tabProvider;

        protected ItemEntry(Identifier id, java.util.function.Supplier<I> factory, TabBuilder creativeTabProvider) {
            super(id, factory);

            this.tabProvider = creativeTabProvider == null
                    ? this.defaultInTab()
                    : creativeTabProvider;
        }

        public TabBuilder getTabProvider() {
            return this.tabProvider;
        }

        private TabBuilder defaultInTab() {
            return displayParams -> List.of(this.get().getDefaultInstance());
        }

        private static TabBuilder populateHookVariantsInTab() {
            return displayParams -> {
                ArrayList<ItemStack> grappleHookVariants = new ArrayList<>();
                grappleHookVariants.add(ModItems.GRAPPLING_HOOK.get().getDefaultInstance());

                HookTemplates.getTemplates().stream()
                        .filter(HookTemplates.Template::isEnabled)
                        .map(HookTemplates.Template::getAsStack)
                        .forEachOrdered(grappleHookVariants::add);

                return grappleHookVariants;
            };
        }
    }
}
