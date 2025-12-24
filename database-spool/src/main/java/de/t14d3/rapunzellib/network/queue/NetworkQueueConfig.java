package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record NetworkQueueConfig(
    boolean enabled,
    Set<String> channelAllowlist,
    Duration flushPeriod,
    int maxBatchSize,
    Duration maxAge
) {
    public static final Set<String> DEFAULT_CHANNEL_ALLOWLIST =
        Set.of("rapunzellib:filesync:invalidate", "db.cache_event");

    public static NetworkQueueConfig read(YamlConfig config) {
        Objects.requireNonNull(config, "config");

        boolean enabled = config.getBoolean("network.queue.enabled", true);

        Set<String> allowlist = normalize(config.getStringList("network.queue.allowlist"));
        if (allowlist.isEmpty()) {
            allowlist = DEFAULT_CHANNEL_ALLOWLIST;
        }

        long flushSeconds = Math.max(1L, config.getLong("network.queue.flushPeriodSeconds", 2));
        int maxBatchSize = (int) Math.max(1L, config.getLong("network.queue.maxBatchSize", 200));
        long maxAgeSeconds = Math.max(0L, config.getLong("network.queue.maxAgeSeconds", 300));

        return new NetworkQueueConfig(
            enabled,
            allowlist,
            Duration.ofSeconds(flushSeconds),
            maxBatchSize,
            Duration.ofSeconds(maxAgeSeconds)
        );
    }

    public static String defaultOwnerId() {
        return Rapunzel.context().platformId().name() + ":" +
            Rapunzel.context().dataDirectory().toAbsolutePath().normalize();
    }

    private static Set<String> normalize(List<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) return Set.of();
        HashSet<String> out = new HashSet<>();
        for (String channel : allowlist) {
            if (channel == null) continue;
            String trimmed = channel.trim();
            if (!trimmed.isBlank()) out.add(trimmed);
        }
        return Set.copyOf(out);
    }
}

