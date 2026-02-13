package de.t14d3.rapunzellib.nbt.paper;

import de.t14d3.rapunzellib.nbt.shared.SharedEntityLocation;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record PaperLocation(
    @NotNull ServerLevel level,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) implements SharedEntityLocation {
    public static @NotNull PaperLocation fromBukkit(@NotNull Location location) {
        Objects.requireNonNull(location, "location");
        World world = Objects.requireNonNull(location.getWorld(), "location.world");
        return new PaperLocation(
            ((CraftWorld) world).getHandle(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    public PaperLocation(@NotNull ServerLevel level, double x, double y, double z) {
        this(level, x, y, z, 0.0f, 0.0f);
    }

    public PaperLocation(@NotNull ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
        this(level, pos.x, pos.y, pos.z, 0.0f, 0.0f);
    }
}
