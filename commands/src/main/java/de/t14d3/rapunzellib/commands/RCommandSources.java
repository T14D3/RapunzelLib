package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Factory for creating {@link RCommandSource} instances with various configurations.
 * <p>
 * Provides a set of overloaded {@code of(...)} factory methods to construct command sources
 * from platform handles, audiences, players, reply channels, and permission checkers.
 * Also includes a convenience method for creating {@link ReplyChannels}.
 * </p>
 */
public final class RCommandSources {
    private RCommandSources() {
    }

    /**
     * A record holding three separate {@link Audience} instances for different reply channels.
     *
     * @param audience        the primary audience for standard messages
     * @param systemAudience  the audience for system messages
     * @param failureAudience the audience for failure/error messages
     */
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

    /**
     * Creates {@link ReplyChannels} where all three channels use the same audience.
     *
     * @param audience the audience for all reply channels
     * @return the reply channels
     */
    public static @NotNull ReplyChannels replyChannels(@NotNull Audience audience) {
        Objects.requireNonNull(audience, "audience");
        return new ReplyChannels(audience, audience, audience);
    }

    /**
     * Creates {@link ReplyChannels} with separate audiences for each channel.
     *
     * @param audience        the primary audience
     * @param systemAudience  the audience for system messages
     * @param failureAudience the audience for failure/error messages
     * @return the reply channels
     */
    public static @NotNull ReplyChannels replyChannels(
        @NotNull Audience audience,
        @NotNull Audience systemAudience,
        @NotNull Audience failureAudience
    ) {
        return new ReplyChannels(audience, systemAudience, failureAudience);
    }

    /**
     * Creates an {@link RCommandSource} from an {@link Audience} without a player.
     *
     * @param platformId the platform identifier
     * @param audience   the audience
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Audience audience) {
        return of(platformId, audience, replyChannels(audience), Optional.empty());
    }

    /**
     * Creates an {@link RCommandSource} from an {@link Audience} with a player.
     *
     * @param platformId the platform identifier
     * @param audience   the audience
     * @param player     the player
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Audience audience, RPlayer player) {
        return of(platformId, audience, replyChannels(audience), Optional.of(player));
    }

    /**
     * Creates an {@link RCommandSource} from an {@link Audience} with an optional player.
     *
     * @param platformId the platform identifier
     * @param audience   the audience
     * @param player     the optional player
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Audience audience, Optional<RPlayer> player) {
        return of(platformId, audience, replyChannels(audience), player);
    }

    /**
     * Creates an {@link RCommandSource} from a platform handle and audience without a player.
     *
     * @param platformId the platform identifier
     * @param handle     the native platform handle
     * @param audience   the audience
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience) {
        return of(platformId, handle, replyChannels(audience), Optional.empty());
    }

    /**
     * Creates an {@link RCommandSource} from a platform handle, audience, and player.
     *
     * @param platformId the platform identifier
     * @param handle     the native platform handle
     * @param audience   the audience
     * @param player     the player
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience, RPlayer player) {
        return of(platformId, handle, replyChannels(audience), Optional.of(player));
    }

    /**
     * Creates an {@link RCommandSource} from a platform handle, audience, and optional player.
     *
     * @param platformId the platform identifier
     * @param handle     the native platform handle
     * @param audience   the audience
     * @param player     the optional player
     * @return the command source
     */
    public static RCommandSource of(PlatformId platformId, Object handle, Audience audience, Optional<RPlayer> player) {
        return of(platformId, handle, replyChannels(audience), player, defaultPermissionChecker(player));
    }

    /**
     * Creates an {@link RCommandSource} from a platform handle, reply channels, and optional player
     * using the default permission checker.
     *
     * @param platformId    the platform identifier
     * @param handle        the native platform handle
     * @param replyChannels the reply channels
     * @param player        the optional player
     * @return the command source
     */
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
