// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.core;

import net.minecraft.util.StringRepresentable;

public enum ComputerFamily implements StringRepresentable {
    NORMAL("normal"),
    ADVANCED("advanced"),
    COMMAND("command");

    private final String name;

    ComputerFamily(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
