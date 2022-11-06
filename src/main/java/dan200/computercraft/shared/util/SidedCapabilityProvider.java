/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A {@link ICapabilityProvider} which provides a different single capability, with different instances for each
 * direction.
 * <p>
 * This is designed for use with {@link AttachCapabilitiesEvent}, to attach individual capabilities to a specific
 * block entity.
 *
 * @param <T> The capability to provide.
 * @see CapabilityProvider
 */
public final class SidedCapabilityProvider<T> implements ICapabilityProvider {
    private final Capability<T> cap;
    private final Provider<T> supplier;
    private @Nullable LazyOptional<T>[] instances;

    private SidedCapabilityProvider(Capability<T> cap, Provider<T> supplier) {
        this.cap = Objects.requireNonNull(cap, "Capability cannot be null");
        this.supplier = supplier;
    }

    public static <T> SidedCapabilityProvider<T> attach(AttachCapabilitiesEvent<?> event, ResourceLocation id, Capability<T> cap, Provider<T> supplier) {
        var provider = new SidedCapabilityProvider<>(cap, supplier);
        event.addCapability(id, provider);
        event.addListener(provider::invalidate);
        return provider;
    }

    public void invalidate() {
        CapabilityUtil.invalidate(instances);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <U> LazyOptional<U> getCapability(Capability<U> cap, @Nullable Direction side) {
        if (cap != this.cap) return LazyOptional.empty();

        var instances = this.instances;
        if (instances == null) instances = this.instances = new LazyOptional[6];

        var index = side == null ? 6 : side.ordinal();

        var instance = instances[index];
        if (instance == null) {
            var created = supplier.get(side);
            instance = instances[index] = created == null ? LazyOptional.empty() : LazyOptional.of(() -> created);
        }

        return instance.cast();
    }

    public interface Provider<T> {
        @Nullable
        T get(@Nullable Direction direction);
    }
}
