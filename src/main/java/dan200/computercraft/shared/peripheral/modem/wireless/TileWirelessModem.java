/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.modem.wireless;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileWirelessModem extends TileGeneric {
    private static class Peripheral extends WirelessModemPeripheral {
        private final TileWirelessModem entity;

        Peripheral(TileWirelessModem entity) {
            super(new ModemState(() -> TickScheduler.schedule(entity.tickToken)), entity.advanced);
            this.entity = entity;
        }

        @Nonnull
        @Override
        public Level getLevel() {
            return entity.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition() {
            return Vec3.atLowerCornerOf(entity.getBlockPos().relative(entity.getDirection()));
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other || (other instanceof Peripheral && entity == ((Peripheral) other).entity);
        }

        @Nonnull
        @Override
        public Object getTarget() {
            return entity;
        }
    }

    private final boolean advanced;

    private final ModemPeripheral modem;
    private boolean destroyed = false;
    private @Nullable Runnable modemChanged;
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);

    public TileWirelessModem(BlockEntityType<? extends TileWirelessModem> type, BlockPos pos, BlockState state, boolean advanced) {
        super(type, pos, state);
        this.advanced = advanced;
        modem = new Peripheral(this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // TODO: Replace with onLoad
        TickScheduler.schedule(tickToken);
    }

    @Override
    public void destroy() {
        if (!destroyed) {
            modem.destroy();
            destroyed = true;
        }
    }

    @Override
    @Deprecated
    public void setBlockState(@Nonnull BlockState state) {
        var direction = getDirection();
        super.setBlockState(state);
        if (getDirection() != direction && modemChanged != null) modemChanged.run();
    }

    @Override
    public void blockTick() {
        if (modem.getModemState().pollChanged()) updateBlockState();
    }

    @Nonnull
    private Direction getDirection() {
        return getBlockState().getValue(BlockWirelessModem.FACING);
    }

    private void updateBlockState() {
        var on = modem.getModemState().isOpen();
        var state = getBlockState();
        if (state.getValue(BlockWirelessModem.ON) != on) {
            getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(BlockWirelessModem.ON, on));
        }
    }

    @Nullable
    public IPeripheral getPeripheral(@Nullable Direction direction) {
        if (destroyed) return null;
        return direction == null || getDirection() == direction ? modem : null;
    }

    public void onModemChanged(Runnable callback) {
        modemChanged = callback;
    }
}
