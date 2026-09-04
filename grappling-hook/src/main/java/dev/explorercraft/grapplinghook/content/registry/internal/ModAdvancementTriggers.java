package dev.explorercraft.grapplinghook.content.registry.internal;

import dev.explorercraft.grapplinghook.GrappleMod;
import dev.explorercraft.grapplinghook.content.advancement.trigger.PhysicsUpdateTrigger;
import dev.explorercraft.grapplinghook.content.registry.helper.AbstractRegistryReference;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.function.Supplier;

public class ModAdvancementTriggers {

    private static final LinkedHashSet<TriggerEntry<?>> advancementTriggers;

    static {
        advancementTriggers = new LinkedHashSet<>();
    }


    public static final TriggerEntry<PhysicsUpdateTrigger> PHYSICS_UPDATE_TRIGGER = trigger("grapple_physics_changed", PhysicsUpdateTrigger::new);


    public static <T extends CriterionTrigger<?>> TriggerEntry<T> trigger(String name, Supplier<T> trigger) {
        TriggerEntry<T> entry = new TriggerEntry<>(GrappleMod.id(name), trigger);
        advancementTriggers.add(entry);

        return entry;
    }


    public static void registerAllTriggers() {
        for(TriggerEntry<?> entry: advancementTriggers) {
            CriterionTrigger<?> it = entry.getFactory().get();
            entry.finalize(CriteriaTriggers.register(entry.getIdentifier().toString(), it));
        }
    }

    public static class TriggerEntry<T extends CriterionTrigger<?>> extends AbstractRegistryReference<T> {

        protected TriggerEntry(Identifier id, Supplier<T> factory) {
            super(id, factory);
        }

    }

}
