package de.t14d3.rapunzellib.network.bootstrap;

import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.network.CompositeMessenger;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkHealthMonitor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class TransportSelectionPlanner {
    private TransportSelectionPlanner() {
    }

    public static @NotNull Messenger select(
        MessengerTransportBootstrap.TransportPriority priority,
        @NotNull Logger logger,
        @NotNull ServiceRegistry services,
        @NotNull Messenger inMemory,
        Messenger plugin,
        Messenger redis,
        Messenger rpc
    ) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(inMemory, "inMemory");

        MessengerTransportBootstrap.TransportPriority effective =
            priority != null ? priority : MessengerTransportBootstrap.TransportPriority.PLUGIN_ONLY;

        return switch (effective) {
            case REDIS_ONLY -> firstNonNull(redis, inMemory);
            case PLUGIN_ONLY -> firstNonNull(plugin, inMemory);
            case RPC_SERVER_ONLY -> firstNonNull(rpc, inMemory);
            case REDIS_FIRST -> compositeOrFallback(
                logger,
                services,
                inMemory,
                redis,
                plugin,
                rpc
            );
            case PLUGIN_FIRST -> compositeOrFallback(
                logger,
                services,
                inMemory,
                plugin,
                redis,
                rpc
            );
            case RPC_SERVER_FIRST -> compositeOrFallback(
                logger,
                services,
                inMemory,
                rpc,
                redis,
                plugin
            );
        };
    }

    private static Messenger compositeOrFallback(
        Logger logger,
        ServiceRegistry services,
        Messenger inMemory,
        Messenger primary,
        Messenger... candidates
    ) {
        if (primary == null) {
            for (Messenger candidate : candidates) {
                if (candidate != null) {
                    return candidate;
                }
            }
            return inMemory;
        }

        List<Messenger> fallbacks = dedupeNonNull(candidates);
        fallbacks.remove(primary);
        if (fallbacks.isEmpty()) {
            return primary;
        }

        CompositeMessenger composite = new CompositeMessenger(
            primary,
            fallbacks,
            logger,
            new NetworkHealthMonitor()
        );
        services.register(NetworkHealthMonitor.class, composite.getHealthMonitor());
        return composite;
    }

    private static Messenger firstNonNull(Messenger... candidates) {
        for (Messenger candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("At least one messenger candidate is required");
    }

    private static List<Messenger> dedupeNonNull(Messenger... candidates) {
        LinkedHashSet<Messenger> ordered = new LinkedHashSet<>();
        for (Messenger candidate : candidates) {
            if (candidate != null) {
                ordered.add(candidate);
            }
        }
        return new ArrayList<>(ordered);
    }
}
