package de.t14d3.rapunzellib.network.cache;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Listener for cache invalidation messages from the messenger system.
 */
public class CacheInvalidationListener implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationListener.class);
    private static final Gson gson = JsonCodecs.gson();
    
    private final Map<String, Consumer<CacheInvalidationMessage>> invalidationListeners;
    private final CacheStatistics statistics;

    /**
     * Creates a new CacheInvalidationListener.
     */
    public CacheInvalidationListener() {
        this.invalidationListeners = new ConcurrentHashMap<>();
        this.statistics = new CacheStatistics();
    }

    /**
     * Creates a new CacheInvalidationListener with existing listeners and statistics.
     *
     * @param invalidationListeners the map of entity class names to listeners
     * @param statistics the cache statistics
     */
    public CacheInvalidationListener(@NotNull Map<String, Consumer<CacheInvalidationMessage>> invalidationListeners,
                                   @NotNull CacheStatistics statistics) {
        this.invalidationListeners = new ConcurrentHashMap<>(invalidationListeners);
        this.statistics = statistics;
    }

    @Override
    public void onMessage(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
        if (!DistributedCacheManager.CACHE_INVALIDATION_CHANNEL.equals(channel)) {
            return; // Not a cache invalidation message
        }

        try {
            CacheInvalidationMessage invalidationMessage = gson.fromJson(data, CacheInvalidationMessage.class);
            
            // Notify all listeners (including wildcard listeners)
            invalidationListeners.forEach((entityClassName, listener) -> {
                if (entityClassName.equals("*") || entityClassName.equals(invalidationMessage.entityClassName())) {
                    try {
                        listener.accept(invalidationMessage);
                    } catch (Exception e) {
                        logger.error("Error notifying listener for entity {}", entityClassName, e);
                    }
                }
            });

            statistics.incrementInvalidationsReceived();
            logger.debug("Received cache invalidation from server {}: {}#{} ({})",
                        sourceServer,
                        invalidationMessage.entityClassName(),
                        invalidationMessage.id(),
                        invalidationMessage.operation());
                        
        } catch (Exception e) {
            logger.warn("Failed to process cache invalidation message from server {}: {}", sourceServer, data, e);
        }
    }

    /**
     * Adds an invalidation listener for a specific entity class.
     *
     * @param entityClassName the entity class name to listen for, or "*" for all
     * @param listener the listener to notify
     */
    public void addInvalidationListener(@NotNull String entityClassName, @NotNull Consumer<CacheInvalidationMessage> listener) {
        invalidationListeners.put(entityClassName, listener);
    }

    /**
     * Removes an invalidation listener.
     *
     * @param entityClassName the entity class name
     */
    public void removeInvalidationListener(@NotNull String entityClassName) {
        invalidationListeners.remove(entityClassName);
    }
}
