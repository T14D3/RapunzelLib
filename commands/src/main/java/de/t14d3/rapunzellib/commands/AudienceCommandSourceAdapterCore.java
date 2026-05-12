package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.RPlayer;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Utility for wrapping platform command sources into {@link RCommandSource} instances
 * using {@link net.kyori.adventure.audience.Audience} extraction.
 * <p>
 * Provides factory methods that derive {@link RCommandSources.ReplyChannels} from an
 * {@code Audience} extractor, enabling consistent command source creation across
 * different platform implementations (Bukkit, Sponge, etc.).
 * </p>
 */
public final class AudienceCommandSourceAdapterCore {
    private AudienceCommandSourceAdapterCore() {
    }

    /**
     * Wraps a platform source into an {@link RCommandSource} using an audience extractor.
     *
     * @param <T>                the platform source type
     * @param platformId         the platform identifier
     * @param source             the platform source object
     * @param audienceExtractor  extracts an {@link Audience} from the platform source
     * @param permissionChecker  checks permissions for the platform source
     * @param playerExtractor    extracts an optional {@link RPlayer} from the platform source
     * @return the wrapped command source
     */
    public static <T> @NotNull RCommandSource wrap(
        @NotNull PlatformId platformId,
        @NotNull T source,
        @NotNull Function<? super T, ? extends Audience> audienceExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        return wrapReplyChannels(
            platformId,
            source,
            value -> RCommandSources.replyChannels(audienceExtractor.apply(value)),
            permissionChecker,
            playerExtractor
        );
    }

    /**
     * Wraps a platform source into an {@link RCommandSource} using a custom reply channels extractor.
     *
     * @param <T>                      the platform source type
     * @param platformId               the platform identifier
     * @param source                   the platform source object
     * @param replyChannelsExtractor   extracts {@link RCommandSources.ReplyChannels} from the platform source
     * @param permissionChecker        checks permissions for the platform source
     * @param playerExtractor          extracts an optional {@link RPlayer} from the platform source
     * @return the wrapped command source
     */
    public static <T> @NotNull RCommandSource wrapReplyChannels(
        @NotNull PlatformId platformId,
        @NotNull T source,
        @NotNull Function<? super T, ? extends RCommandSources.ReplyChannels> replyChannelsExtractor,
        @NotNull BiPredicate<? super T, ? super String> permissionChecker,
        @NotNull Function<? super T, Optional<RPlayer>> playerExtractor
    ) {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(replyChannelsExtractor, "replyChannelsExtractor");
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(playerExtractor, "playerExtractor");

        RCommandSources.ReplyChannels replyChannels = Objects.requireNonNull(replyChannelsExtractor.apply(source), "replyChannels");
        Optional<RPlayer> player = Objects.requireNonNull(playerExtractor.apply(source), "player");
        return RCommandSources.of(platformId, source, replyChannels, player, permission -> permissionChecker.test(source, permission));
    }
}
