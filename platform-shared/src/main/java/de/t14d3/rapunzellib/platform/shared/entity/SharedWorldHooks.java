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

/** Interface providing world creation, reference resolution, and UUID derivation hooks. */
public interface SharedWorldHooks {
    @NotNull RWorld createWorld(@NotNull ServerLevel level);

    default @NotNull RWorldRef worldRef(@NotNull ServerLevel level) {
        return new RWorldRef(null, key(level));
    }

    default @NotNull Optional<UUID> worldUuid(@NotNull ServerLevel level) {
        return Optional.of(UUID.nameUUIDFromBytes(key(level).asString().getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Resolves a {@link RWorldRef} to a native {@link ServerLevel}, searching by key first, then by name.
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

    default boolean matchesName(@NotNull ServerLevel level, @NotNull String name) {
        return name.equalsIgnoreCase(key(level).asString());
    }

    static @NotNull SharedWorldHooks of(@NotNull Function<ServerLevel, ? extends RWorld> worldFactory) {
        Objects.requireNonNull(worldFactory, "worldFactory");
        return new SharedWorldHooks() {
            @Override
            public @NotNull RWorld createWorld(@NotNull ServerLevel level) {
                return worldFactory.apply(level);
            }
        };
    }

    static @NotNull SharedWorldHooks unsupported() {
        return of(level -> {
            throw new UnsupportedOperationException("world creation hook is not available");
        });
    }

    static @NotNull RKey key(@NotNull ServerLevel level) {
        Objects.requireNonNull(level, "level");
        // #if VERSION >= 1.21.11
        return RKey.of(level.dimension().identifier().toString());
        // #else
        return RKey.of(level.dimension().location().toString());
        // #endif
    }
}
