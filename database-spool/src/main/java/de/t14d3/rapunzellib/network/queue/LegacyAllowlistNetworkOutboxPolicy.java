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

public final class LegacyAllowlistNetworkOutboxPolicy implements NetworkOutboxPolicy {
    public static final Set<String> DEFAULT_CHANNEL_ALLOWLIST =
        Set.of("rapunzellib:filesync:invalidate", DistributedCacheManager.CACHE_INVALIDATION_CHANNEL, "db.cache_event");

    private final Set<String> channelAllowlist;

    public LegacyAllowlistNetworkOutboxPolicy(Set<String> channelAllowlist) {
        this.channelAllowlist = normalize(channelAllowlist);
    }

    public static @NotNull LegacyAllowlistNetworkOutboxPolicy fromConfig(de.t14d3.rapunzellib.config.YamlConfig config) {
        Objects.requireNonNull(config, "config");

        Set<String> allowlist = normalize(config.getStringList("network.queue.allowlist", List.of()));
        if (allowlist.isEmpty()) {
            allowlist = DEFAULT_CHANNEL_ALLOWLIST;
        }
        return new LegacyAllowlistNetworkOutboxPolicy(allowlist);
    }

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

    public @NotNull Set<String> channelAllowlist() {
        return channelAllowlist;
    }

    private static @NotNull Set<String> normalize(List<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) {
            return Set.of();
        }
        return normalize(Set.copyOf(allowlist));
    }

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
