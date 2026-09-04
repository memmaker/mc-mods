package dev.explorercraft.grapplinghook.content.registry.helper;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public abstract class HoldingRegistryReference<B, T extends B> extends AbstractRegistryReference<T> {

    private final Registry<B> registry;

    private Holder<B> holder;

    protected HoldingRegistryReference(Identifier id, Supplier<T> factory, Registry<B> registry) {
        super(id, factory);
        this.registry = registry;
        this.holder = null;
    }

    public void register() {
        if(this.isRegistered())
            throw new IllegalStateException("Attempted to register an already registered entry");

        T mat = this.getFactory().get();

        this.holder = Registry.registerForHolder(this.registry, this.getIdentifier(), mat);
        this.finalize(mat);
    }

    public Holder<B> asHolder() {
        if(!this.isRegistered())
            throw new IllegalStateException("Attempted to reference unregistered entry");

        return this.holder;
    }

}
