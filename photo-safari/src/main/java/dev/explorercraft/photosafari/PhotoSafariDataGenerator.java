package dev.explorercraft.photosafari;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// Writes the advancement tree into src/main/generated. Run `./gradlew runDatagen` after
/// a Minecraft update so new mobs show up on the checklist.
public class PhotoSafariDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider(Advancements::new);
    }

    private static class Advancements extends FabricAdvancementProvider {
        Advancements(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer) {
            AdvancementHolder root = Advancement.Builder.advancement()
                    .display(BuiltInRegistries.ITEM.getValue(Identifier.parse("camerapture:camera")),
                            title("root"), description("root"),
                            Identifier.withDefaultNamespace("gui/advancements/backgrounds/adventure"),
                            AdvancementType.TASK, false, false, false)
                    .addCriterion("photographed", speciesCount(1))
                    .build(PhotoSafari.id("root"));
            consumer.accept(root);

            AdvancementHolder previous = root;
            previous = tier(consumer, previous, "first_shot", 1, 50, AdvancementType.TASK);
            previous = tier(consumer, previous, "collector", 10, 100, AdvancementType.TASK);
            previous = tier(consumer, previous, "naturalist", 25, 250, AdvancementType.GOAL);
            tier(consumer, previous, "zoologist", 50, 500, AdvancementType.CHALLENGE);

            // One entry per photographable species, so the advancement screen is the checklist.
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                Identifier species = EntityType.getKey(type);
                Item egg = spawnEgg(species);
                if (egg == null) {
                    continue;
                }

                sink(consumer, species).accept(Advancement.Builder.advancement()
                        .parent(root)
                        .display(egg, type.getDescription(),
                                Component.translatable("advancements.photosafari.species.description",
                                        type.getDescription()),
                                null, AdvancementType.TASK, true, false, false)
                        .rewards(AdvancementRewards.Builder.experience(10))
                        .addCriterion("photographed", species(species))
                        .build(PhotoSafari.id("species/" + species.getNamespace() + "/" + species.getPath())));
            }
        }

        /// Mobs from another mod only show up on the checklist when that mod is installed.
        /// ponytail: keyed on the entity namespace, which is the mod id for every mod worth
        /// generating for. A mod registering under a foreign namespace would need naming here.
        private Consumer<AdvancementHolder> sink(Consumer<AdvancementHolder> consumer, Identifier species) {
            if (species.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                return consumer;
            }

            return withConditions(consumer, ResourceConditions.allModsLoaded(species.getNamespace()));
        }

        private static AdvancementHolder tier(Consumer<AdvancementHolder> consumer, AdvancementHolder parent,
                                              String name, int minSpecies, int experience, AdvancementType type) {
            AdvancementRewards.Builder rewards = AdvancementRewards.Builder.experience(experience);
            if (minSpecies >= 50) {
                rewards = rewards.addLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                        PhotoSafari.id("rewards/zoologist")));
            }

            AdvancementHolder advancement = Advancement.Builder.advancement()
                    .parent(parent)
                    .display(BuiltInRegistries.ITEM.getValue(Identifier.parse("camerapture:picture")),
                            title(name), description(name), null, type, true, true, false)
                    .rewards(rewards)
                    .addCriterion("photographed", speciesCount(minSpecies))
                    .build(PhotoSafari.id(name));

            consumer.accept(advancement);
            return advancement;
        }

        /// Item data components are not bound during datagen, so SpawnEggItem.byId is out and
        /// we go by name instead: vanilla writes cow_spawn_egg, Alex's Mobs writes
        /// spawn_egg_grizzly_bear. A mod naming eggs some third way still counts towards the
        /// tiers in game, it just gets no checklist entry.
        private static Item spawnEgg(Identifier species) {
            for (String path : List.of(species.getPath() + "_spawn_egg", "spawn_egg_" + species.getPath())) {
                Identifier egg = Identifier.fromNamespaceAndPath(species.getNamespace(), path);
                if (BuiltInRegistries.ITEM.containsKey(egg)) {
                    return BuiltInRegistries.ITEM.getValue(egg);
                }
            }

            return null;
        }

        private static Criterion<SpeciesPhotographedTrigger.TriggerInstance> speciesCount(int minSpecies) {
            return new Criterion<>(PhotoSafari.photographedTrigger,
                    new SpeciesPhotographedTrigger.TriggerInstance(Optional.empty(), minSpecies, Optional.empty()));
        }

        private static Criterion<SpeciesPhotographedTrigger.TriggerInstance> species(Identifier species) {
            return new Criterion<>(PhotoSafari.photographedTrigger,
                    new SpeciesPhotographedTrigger.TriggerInstance(Optional.empty(), 1, Optional.of(species)));
        }

        private static Component title(String name) {
            return Component.translatable("advancements.photosafari." + name + ".title");
        }

        private static Component description(String name) {
            return Component.translatable("advancements.photosafari." + name + ".description");
        }
    }
}
