package de.t14d3.rapunzellib.network.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks statistics for cache operations.
 */
public class CacheStatistics {
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong invalidationsSent = new AtomicLong(0);
    private final AtomicLong invalidationsReceived = new AtomicLong(0);

    /**
     * Increments the hit counter.
     */
    public void incrementHits() {
        hits.incrementAndGet();
    }

    /**
     * Increments the miss counter.
     */
    public void incrementMisses() {
        misses.incrementAndGet();
    }

    /**
     * Increments the invalidations sent counter.
     */
    public void incrementInvalidationsSent() {
        invalidationsSent.incrementAndGet();
    }

    /**
     * Increments the invalidations received counter.
     */
    public void incrementInvalidationsReceived() {
        invalidationsReceived.incrementAndGet();
    }

    /**
     * Gets the current hit count.
     *
     * @return the hit count
     */
    public long getHits() {
        return hits.get();
    }

    /**
     * Gets the current miss count.
     *
     * @return the miss count
     */
    public long getMisses() {
        return misses.get();
    }

    /**
     * Gets the current invalidations sent count.
     *
     * @return the invalidations sent count
     */
    public long getInvalidationsSent() {
        return invalidationsSent.get();
    }

    /**
     * Gets the current invalidations received count.
     *
     * @return the invalidations received count
     */
    public long getInvalidationsReceived() {
        return invalidationsReceived.get();
    }
}
