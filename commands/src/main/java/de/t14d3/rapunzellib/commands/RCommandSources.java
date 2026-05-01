package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class RCommandSources {
    private RCommandSources() {
    }

    public record ReplyChannels(
        @NotNull Audience audience,
        @NotNull Audience systemAudience,
        @NotNull Audience failureAudience
    ) {
        public ReplyChannels {
            Objects.requireNonNull(audience, "audience");
            Objects.requireNonNull(systemAudience, "systemAudience");
            Objects.requireNonNull(failureAudience, "failureAudience");
        }
    }

    public static @NotNull ReplyChannels replyChannels(@NotNull Audience audience) {
        Objects.requireNonNull(audience, "audience");
        return new ReplyChannels(audience, audience, audience);
    }

    public static @NotNull ReplyChannels replyChannels(
        @NotNull Audience audience,
        @NotNull Audience systemAudience,
        @NotNull Audience failureAudience
    ) {
        return new ReplyChannels(audience, systemAudience, failureAudience);
    }

    public static RCommandSource of(PlatformId platformId, Audience audience) {
        return of(platformId, audience, replyChannels(audience), Optional.empty());
    }

    public static RCommandSource of(PlatformId platformId, Audience audience, RPlayer player) {
        return of(platformId, audience, replyChannels(audience), Optional.of(player));
    }

    public static RCommandSource of(PlatformId platformId, Audience audience, Optional<RPlayer> player) {
        return of(platformId, audience, replyChannels(audience), player);
    }

    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience) {
        return of(platformId, handle, replyChannels(audience), Optional.empty());
    }

    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience, RPlayer player) {
        return of(platformId, handle, replyChannels(audience), Optional.of(player));
    }

    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience, Optional<RPlayer> player) {
        return of(platformId, handle, replyChannels(audience), player, defaultPermissionChecker(player));
    }

    public static RCommandSource of(PlatformId platformId, Object handle, ReplyChannels replyChannels, Optional<RPlayer> player) {
        return of(platformId, handle, replyChannels, player, defaultPermissionChecker(player));
    }

    public static RCommandSource of(
        PlatformId platformId,
        Object handle,
        ReplyChannels replyChannels,
        Optional<RPlayer> player,
        Predicate<String> permissionChecker
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(replyChannels, "replyChannels");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        return new DefaultRCommandSource(platformId, handle, replyChannels, player, permissionChecker);
    }

    public static RCommandSource of(
        PlatformId platformId,
        Object handle,
        Audience audience,
        Optional<RPlayer> player,
        Predicate<String> permissionChecker
    ) {
        return of(platformId, handle, replyChannels(audience), player, permissionChecker);
    }

    private static Predicate<String> defaultPermissionChecker(Optional<RPlayer> player) {
        return permission -> player.map(value -> value.hasPermission(permission)).orElse(false);
    }

    private static final class DefaultRCommandSource extends RNativeHandle<Object> implements RCommandSource {
        private final ReplyChannels replyChannels;
        private final Optional<RPlayer> player;
        private final Predicate<String> permissionChecker;

        private DefaultRCommandSource(
            PlatformId platformId,
            Object handle,
            ReplyChannels replyChannels,
            Optional<RPlayer> player,
            Predicate<String> permissionChecker
        ) {
            super(platformId, Objects.requireNonNull(handle, "handle"));
            this.replyChannels = Objects.requireNonNull(replyChannels, "replyChannels");
            this.player = Objects.requireNonNull(player, "player");
            this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker");
        }

        @Override
        public @NotNull Audience audience() {
            return replyChannels.audience();
        }

        @Override
        public @NotNull Audience systemAudience() {
            return replyChannels.systemAudience();
        }

        @Override
        public @NotNull Audience failureAudience() {
            return replyChannels.failureAudience();
        }

        @Override
        public Optional<RPlayer> player() {
            return player;
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            Objects.requireNonNull(permission, "permission");
            if (permission.isBlank()) {
                return true;
            }
            return permissionChecker.test(permission);
        }
    }
}
