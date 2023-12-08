// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.platform.RegistryEntry;
import dan200.computercraft.shared.util.BlockCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A subclass of {@link ComputerBlock} which implements {@link GameMasterBlock}, to prevent players breaking it without
 * permission.
 *
 * @param <T> The type of the computer block entity.
 * @see dan200.computercraft.shared.computer.items.CommandComputerItem
 */
public class CommandComputerBlock<T extends CommandComputerBlockEntity> extends ComputerBlock<T> implements GameMasterBlock {
    private static final MapCodec<CommandComputerBlock<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BlockCodecs.propertiesCodec(),
        StringRepresentable.fromEnum(ComputerFamily::values).fieldOf("family").forGetter(AbstractComputerBlock::getFamily),
        BlockCodecs.blockEntityCodec(x -> x.type)
    ).apply(instance, CommandComputerBlock::new));

    public CommandComputerBlock(Properties settings, ComputerFamily family, RegistryEntry<BlockEntityType<T>> type) {
        super(settings, family, type);
    }

    @Override
    protected MapCodec<? extends CommandComputerBlock<?>> codec() {
        return CODEC;
    }
}
