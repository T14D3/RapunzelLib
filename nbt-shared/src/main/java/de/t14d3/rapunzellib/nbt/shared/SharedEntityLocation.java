package de.t14d3.rapunzellib.nbt.shared;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public interface SharedEntityLocation {
    @NotNull ServerLevel level();

    double x();

    double y();

    double z();

    float yaw();

    float pitch();
}
