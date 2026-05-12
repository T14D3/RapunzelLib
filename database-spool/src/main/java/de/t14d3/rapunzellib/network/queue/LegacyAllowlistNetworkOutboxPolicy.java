package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.network.cache.DistributedCacheManager;
import de.t14d3.rapunzellib.network.outbox.NetworkDeliverySemantics;
import de.t14d3.rapunzellib.network.outbox.NetworkOutboxPolicy;
import de.t14d3.rapunzellib.network.outbox.NetworkSendRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Legacy implementation of {@link NetworkOutboxPolicy} based on a static channel allowlist.
 * <p>
 * Channels in the allowlist use {@link NetworkDeliverySemantics#STORE_AND_FORWARD};
 * all other channels fall back to {@link NetworkDeliverySemantics#DIRECT_ONLY}.
 * Includes a default allowlist of known cache invalidation channels.
 * </p>
 */
public final class LegacyAllowlistNetworkOutboxPolicy implements NetworkOutboxPolicy {
    /**
     * Default set of channels eligible for store-and-forward queuing.
     */
    public static final Set<String> DEFAULT_CHANNEL_ALLOWLIST =
        Set.of("rapunzellib:filesync:invalidate", DistributedCacheManager.CACHE_INVALIDATION_CHANNEL, "db.cache_event");

    private final Set<String> channelAllowlist;

    /**
     * Constructs a new policy with the given channel allowlist.
     *
     * @param channelAllowlist the set of channels eligible for queuing (null-safe, duplicates removed)
     */
    public LegacyAllowlistNetworkOutboxPolicy(Set<String> channelAllowlist) {
        this.channelAllowlist = normalize(channelAllowlist);
    }

    /**
     * Creates a policy from a YAML config, using {@link #DEFAULT_CHANNEL_ALLOWLIST} if the config value is empty.
     *
     * @param config the YAML configuration
     * @return a new policy instance
     */
    public static @NotNull LegacyAllowlistNetworkOutboxPolicy fromConfig(de.t14d3.rapunzellib.config.YamlConfig config) {
        Objects.requireNonNull(config, "config");

        Set<String> allowlist = normalize(config.getStringList("network.queue.allowlist", List.of()));
        if (allowlist.isEmpty()) {
            allowlist = DEFAULT_CHANNEL_ALLOWLIST;
        }
        return new LegacyAllowlistNetworkOutboxPolicy(allowlist);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull NetworkDeliverySemantics semantics(@NotNull NetworkSendRequest request) {
        Objects.requireNonNull(request, "request");
        if (channelAllowlist.isEmpty()) {
            return NetworkDeliverySemantics.DIRECT_ONLY;
        }
        return channelAllowlist.contains(request.channel().trim())
            ? NetworkDeliverySemantics.STORE_AND_FORWARD
            : NetworkDeliverySemantics.DIRECT_ONLY;
    }

    /**
     * Returns the set of channels in the allowlist.
     *
     * @return an unmodifiable set of channel names
     */
    public @NotNull Set<String> channelAllowlist() {
        return channelAllowlist;
    }

    /**
     * Normalizes a list of channel strings into a cleaned, unmodifiable set.
     *
     * @param allowlist the raw list of channel names
     * @return a normalized set, or an empty set
     */
    private static @NotNull Set<String> normalize(List<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) {
            return Set.of();
        }
        return normalize(Set.copyOf(allowlist));
    }

    /**
     * Normalizes a set of channel strings by trimming whitespace and removing blanks.
     *
     * @param allowlist the raw set of channel names
     * @return a normalized, unmodifiable set
     */
    private static @NotNull Set<String> normalize(Set<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) {
            return Set.of();
        }
        HashSet<String> out = new HashSet<>();
        for (String channel : allowlist) {
            if (channel == null) {
                continue;
            }
            String trimmed = channel.trim();
            if (!trimmed.isBlank()) {
                out.add(trimmed);
            }
        }
        return Set.copyOf(out);
    }
}
