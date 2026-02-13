package de.t14d3.rapunzellib.platform.paper;

import de.t14d3.rapunzellib.objects.RKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PaperHandleBridge {
    private PaperHandleBridge() {
    }

    public static @NotNull MinecraftServer server(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return ((CraftServer) plugin.getServer()).getServer();
    }

    public static @NotNull ServerPlayer toNms(@NotNull Player player) {
        return ((CraftPlayer) Objects.requireNonNull(player, "player")).getHandle();
    }

    public static @NotNull Player toBukkit(@NotNull ServerPlayer player) {
        return player.getBukkitEntity();
    }

    public static @NotNull net.minecraft.world.entity.Entity toNms(@NotNull org.bukkit.entity.Entity entity) {
        return ((CraftEntity) Objects.requireNonNull(entity, "entity")).getHandle();
    }

    public static @NotNull org.bukkit.entity.Entity toBukkit(@NotNull net.minecraft.world.entity.Entity entity) {
        return entity.getBukkitEntity();
    }

    public static @NotNull ServerLevel toNms(@NotNull World world) {
        return ((CraftWorld) Objects.requireNonNull(world, "world")).getHandle();
    }

    public static @NotNull Optional<World> toBukkit(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level");
        NamespacedKey key = NamespacedKey.fromString(level.dimension().identifier().toString());
        if (key == null) return Optional.empty();
        return Optional.ofNullable(Bukkit.getWorld(key));
    }

    public static @NotNull Optional<ServerLevel> levelFromKey(@NotNull MinecraftServer server, @NotNull RKey keyValue) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(keyValue, "keyValue");
        Identifier location = Identifier.tryParse(keyValue.asString());
        if (location == null) return Optional.empty();
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
        return Optional.ofNullable(server.getLevel(key));
    }

    public static @NotNull Optional<ServerLevel> levelFromName(@NotNull MinecraftServer server, @NotNull String worldName) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(worldName, "worldName");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return Optional.empty();
        return Optional.of(toNms(world));
    }

    public static @NotNull BlockState toNms(@NotNull BlockData data) {
        return ((CraftBlockData) Objects.requireNonNull(data, "data")).getState();
    }

    public static @NotNull BlockData toBukkit(@NotNull BlockState state) {
        return CraftBlockData.fromData(Objects.requireNonNull(state, "state"));
    }

    public static @NotNull RKey blockTypeKey(@NotNull BlockState state) {
        return RKey.of(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    public static @NotNull RKey worldKey(@NotNull ServerLevel level) {
        return RKey.of(level.dimension().identifier().toString());
    }

    public static @NotNull UUID worldUuid(@NotNull ServerLevel level) {
        return toBukkit(level)
            .map(World::getUID)
            .orElseGet(() -> UUID.nameUUIDFromBytes(worldKey(level).asString().getBytes(StandardCharsets.UTF_8)));
    }

    public static @NotNull Location toBukkitLocation(
        @NotNull ServerLevel level,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        World world = toBukkit(level).orElseThrow(() -> new IllegalStateException("No Bukkit world for level " + worldKey(level).asString()));
        return new Location(world, x, y, z, yaw, pitch);
    }
}
