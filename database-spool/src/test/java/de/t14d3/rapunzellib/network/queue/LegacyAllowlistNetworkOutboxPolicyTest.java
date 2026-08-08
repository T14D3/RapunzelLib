package de.t14d3.rapunzellib.network.queue;

import de.t14d3.rapunzellib.config.SnakeYamlConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import de.t14d3.rapunzellib.network.cache.DistributedCacheManager;
import de.t14d3.rapunzellib.network.outbox.NetworkDeliverySemantics;
import de.t14d3.rapunzellib.network.outbox.NetworkSendRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyAllowlistNetworkOutboxPolicyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyAllowlistNetworkOutboxPolicyTest.class);

    @Test
    void allowlistedChannelsUseStoreAndForward() {
        LegacyAllowlistNetworkOutboxPolicy policy = new LegacyAllowlistNetworkOutboxPolicy(Set.of("filesync"));

        assertEquals(
            NetworkDeliverySemantics.STORE_AND_FORWARD,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.SERVER, "backend-1", "filesync", "payload"))
        );
    }

    @Test
    void nonAllowlistedChannelsStayDirectOnly() {
        LegacyAllowlistNetworkOutboxPolicy policy = new LegacyAllowlistNetworkOutboxPolicy(Set.of("filesync"));

        assertEquals(
            NetworkDeliverySemantics.DIRECT_ONLY,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.PROXY, null, "rpc:invoke", "payload"))
        );
    }

    @Test
    void defaultAllowlistIncludesDistributedCacheInvalidation() {
        assertTrue(LegacyAllowlistNetworkOutboxPolicy.DEFAULT_CHANNEL_ALLOWLIST.contains(
            DistributedCacheManager.CACHE_INVALIDATION_CHANNEL
        ));
    }

    @Test
    void defaultAllowlistRoutesDistributedCacheInvalidationAsStoreAndForward(@TempDir Path dir) throws Exception {
        // An empty config leaves the allowlist empty -> fromConfig falls back to
        // the DEFAULT_CHANNEL_ALLOWLIST, which must route RLib's own cache
        // invalidation channel with store-and-forward semantics. The legacy
        // consumer-side "db_cache_event" channel is no longer used by RLib and
        // must NOT be allowlisted.
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "", StandardCharsets.UTF_8);
        ResourceProvider noResources = _path -> Optional.empty();
        YamlConfig cfg = new SnakeYamlConfigService(noResources, LOGGER).load(file);

        LegacyAllowlistNetworkOutboxPolicy policy = LegacyAllowlistNetworkOutboxPolicy.fromConfig(cfg);
        assertEquals(
            NetworkDeliverySemantics.STORE_AND_FORWARD,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.SERVER, "backend-1", "rapunzellib:cache:invalidate", "payload"))
        );
        assertEquals(
            NetworkDeliverySemantics.DIRECT_ONLY,
            policy.semantics(new NetworkSendRequest(NetworkEnvelope.Target.SERVER, "backend-1", "db_cache_event", "payload"))
        );
    }

    @Test
    void defaultAllowlistDoesNotContainLegacyDbCacheEventChannel() {
        assertFalse(LegacyAllowlistNetworkOutboxPolicy.DEFAULT_CHANNEL_ALLOWLIST.contains("db_cache_event"),
            "default allowlist must not contain the legacy consumer-side 'db_cache_event' channel");
    }
}
