package de.t14d3.rapunzellib.platform.shared.entity;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import net.minecraft.resources.ResourceKey;
// #if VERSION >= 1.21.11
import net.minecraft.resources.Identifier;
// #else
import net.minecraft.resources.ResourceLocation;
// #endif
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Interface providing world creation, reference resolution, and UUID derivation hooks.
 * <p>
 * These hooks bridge the native {@link ServerLevel} world representation with
 * RapunzelLib's abstract {@link RWorld} interface, enabling cross-platform
 * world operations.
 * </p>
 */
public interface SharedWorldHooks {
    /**
     * Creates an {@link RWorld} wrapper for the given server level.
     *
     * @param level the server level to wrap
     * @return the wrapped world instance
     */
    @NotNull RWorld createWorld(@NotNull ServerLevel level);

    /**
     * Creates a {@link RWorldRef} for the given server level based on its dimension key.
     *
     * @param level the server level
     * @return a world reference with a key but no name
     */
    default @NotNull RWorldRef worldRef(@NotNull ServerLevel level) {
        return new RWorldRef(null, key(level));
    }

    /**
     * Derives a deterministic {@link UUID} for the given server level from its dimension key.
     *
     * @param level the server level
     * @return an Optional containing the UUID
     */
    default @NotNull Optional<UUID> worldUuid(@NotNull ServerLevel level) {
        return Optional.of(UUID.nameUUIDFromBytes(key(level).asString().getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Resolves a {@link RWorldRef} to a native {@link ServerLevel}, searching by key first, then by name.
     *
     * @param server   the Minecraft server instance
     * @param worldRef the world reference to resolve
     * @return an Optional containing the resolved ServerLevel, or empty if not found
     */
    default @NotNull Optional<ServerLevel> resolveWorld(@NotNull MinecraftServer server, @Nullable RWorldRef worldRef) {
        Objects.requireNonNull(server, "server");
        if (worldRef == null) {
            return Optional.empty();
        }

        RKey key = worldRef.key();
        if (key != null) {
            // #if VERSION >= 1.21.11
            Identifier resourceLocation = Identifier.tryParse(key.asString());
            // #else
            ResourceLocation resourceLocation = ResourceLocation.tryParse(key.asString());
            // #endif
            if (resourceLocation != null) {
                ServerLevel level = server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, resourceLocation));
                if (level != null) {
                    return Optional.of(level);
                }
            }
        }

        String name = worldRef.name();
        if (name != null && !name.isBlank()) {
            for (ServerLevel level : server.getAllLevels()) {
                if (matchesName(level, name)) {
                    return Optional.of(level);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether the given level's dimension key matches the specified name.
     *
     * @param level the server level
     * @param name  the name to match
     * @return {@code true} if the name matches
     */
    default boolean matchesName(@NotNull ServerLevel level, @NotNull String name) {
        return name.equalsIgnoreCase(key(level).asString());
    }

    /**
     * Creates a {@link SharedWorldHooks} instance backed by the given world factory function.
     *
     * @param worldFactory a function to create {@link RWorld} wrappers from {@link ServerLevel} instances
     * @return a new SharedWorldHooks instance
     */
    static @NotNull SharedWorldHooks of(@NotNull Function<ServerLevel, ? extends RWorld> worldFactory) {
        Objects.requireNonNull(worldFactory, "worldFactory");
        return new SharedWorldHooks() {
            @Override
            public @NotNull RWorld createWorld(@NotNull ServerLevel level) {
                return worldFactory.apply(level);
            }
        };
    }

    /**
     * Returns a {@link SharedWorldHooks} that throws {@link UnsupportedOperationException} on world creation.
     *
     * @return an unsupported hooks instance
     */
    static @NotNull SharedWorldHooks unsupported() {
        return of(level -> {
            throw new UnsupportedOperationException("world creation hook is not available");
        });
    }

    /**
     * Extracts the {@link RKey} for the given server level from its dimension.
     *
     * @param level the server level
     * @return the corresponding RKey
     */
    static @NotNull RKey key(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level");
        // #if VERSION >= 1.21.11
        return RKey.of(level.dimension().identifier().toString());
        // #else
        return RKey.of(level.dimension().location().toString());
        // #endif
    }
}
