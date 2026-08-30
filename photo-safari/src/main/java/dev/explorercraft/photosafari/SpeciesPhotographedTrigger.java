package dev.explorercraft.photosafari;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// Fires whenever a player photographs wildlife, carrying how many distinct species
/// they have on file. Advancements pick their threshold with `min_species`.
public class SpeciesPhotographedTrigger extends SimpleCriterionTrigger<SpeciesPhotographedTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /// `species` is the one that was just added, or null for a photo that added nothing new.
    public void trigger(ServerPlayer player, int speciesCount, @Nullable Identifier species) {
        trigger(player, instance -> instance.matches(speciesCount, species));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int minSpecies, Optional<Identifier> species)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.INT.optionalFieldOf("min_species", 1).forGetter(TriggerInstance::minSpecies),
                Identifier.CODEC.optionalFieldOf("species").forGetter(TriggerInstance::species)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(int speciesCount, @Nullable Identifier photographed) {
            if (this.species.isPresent() && !this.species.get().equals(photographed)) {
                return false;
            }

            return speciesCount >= this.minSpecies;
        }
    }
}
