// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.peripheral.generic.ComponentLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.common.util.NonNullConsumer;

/**
 * A function which may be called when a capability (or some other object) has been invalidated.
 * <p>
 * This extends {@link NonNullConsumer} for use with {@link ServerLevel#registerCapabilityListener(BlockPos, ICapabilityInvalidationListener)}, and
 * {@link Runnable} for use with {@link ComponentLookup}.
 */
public interface InvalidateCallback extends Runnable, ICapabilityInvalidationListener {
    @Override
    default boolean onInvalidate() {
        run();
        return true;
    }
}
