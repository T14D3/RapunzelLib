package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.network.Messenger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Binds the platform messenger's server name from a CONSUMER's own config at
 * {@code acquire()} time.
 *
 * <p>The platform bootstrap resolves the server name from the platform's own
 * {@code config.yml}, which is typically empty (the platform plugin has no
 * network configuration of its own). The consumer, however, usually configures
 * {@code network.serverName} in <i>its</i> data directory. Without this binding
 * the plugin messenger would emit envelopes with {@code sourceServer}
 * {@code "unknown"} until the async NetworkInfo RPC round-trip completes.</p>
 *
 * <p>The binding is strictly additive: it only fires when the messenger's
 * current name is blank or {@code "unknown"} and the consumer config actually
 * exists and declares a {@code network.serverName}. Consumers without a
 * configured name keep the platform-resolved name (the async NetworkInfo RPC
 * path still binds the correct name later).</p>
 */
public final class ConsumerServerNameBinder {
    private ConsumerServerNameBinder() {
    }

    /**
     * Binds the platform messenger's server name from the consumer config when
     * the currently resolved name is blank/unknown.
     *
     * @param platform         the shared platform context (services + logger)
     * @param consumerConfigs  the consumer-scoped config service (resolves defaults from the consumer jar)
     * @param consumerDataDir  the consumer's data directory containing {@code config.yml}
     * @param messengerType    the concrete platform messenger type registered in the platform services
     * @param binder           binds the resolved name onto the messenger
     * @param platformLabel    platform label for logging (e.g. {@code "paper"})
     * @param <M>              the platform messenger type
     */
    public static <M extends Messenger> void bindIfUnknown(
        @NotNull RapunzelContext platform,
        @NotNull ConfigService consumerConfigs,
        @NotNull Path consumerDataDir,
        @NotNull Class<M> messengerType,
        @NotNull BiConsumer<? super M, String> binder,
        @NotNull String platformLabel
    ) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(consumerConfigs, "consumerConfigs");
        Objects.requireNonNull(consumerDataDir, "consumerDataDir");
        Objects.requireNonNull(messengerType, "messengerType");
        Objects.requireNonNull(binder, "binder");
        Objects.requireNonNull(platformLabel, "platformLabel");

        M messenger = platform.services().find(messengerType).orElse(null);
        if (messenger == null) {
            return;
        }
        String current = messenger.getServerName();
        if (current != null && !current.isBlank() && !"unknown".equalsIgnoreCase(current)) {
            return;
        }

        Path configFile = consumerDataDir.resolve("config.yml");
        if (!Files.exists(configFile)) {
            // No consumer config - keep the platform-resolved name as-is; the
            // async NetworkInfo RPC path still binds the correct name later.
            return;
        }

        String configured;
        try {
            configured = consumerConfigs.load(configFile, "config.yml").getString("network.serverName", "");
        } catch (Exception e) {
            return;
        }
        if (configured == null || configured.isBlank()) {
            return;
        }

        String name = configured.trim();
        binder.accept(messenger, name);
        Logger logger = platform.logger();
        logger.info("[Network] Bound {} messenger server name to: {}", platformLabel, name);
    }
}
