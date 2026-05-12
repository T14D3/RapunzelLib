package de.t14d3.rapunzellib.database;

import de.t14d3.spool.core.EntityManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Thread-safe database wrapper backed by the Spool ORM, providing locking,
 * asynchronous execution, and transaction support.
 * <p>
 * All database access is serialized through a {@code synchronized} lock.
 * Flush operations can be triggered synchronously or asynchronously via a
 * dedicated single-thread executor.
 * </p>
 */
public final class SpoolDatabase implements AutoCloseable {

    /**
     * Creates a new {@link Builder} for constructing a SpoolDatabase.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method to open a database with the given JDBC URL, logger, and entity classes.
     *
     * @param jdbcUrl  the JDBC connection URL
     * @param logger   the logger
     * @param entities the entity classes to register
     * @return a new SpoolDatabase instance
     */
    public static SpoolDatabase open(String jdbcUrl, Logger logger, Class<?>... entities) {
        return builder()
            .jdbcUrl(jdbcUrl)
            .logger(logger)
            .entities(entities)
            .build();
    }

    /** Lock object for serializing database access. */
    private final Object lock = new Object();
    private final EntityManager entityManager;
    private final ExecutorService flushExecutor;
    private final Logger logger;
    private final AtomicBoolean flushRequested = new AtomicBoolean(false);
    private final AtomicBoolean flushWorkerScheduled = new AtomicBoolean(false);

    /**
     * Constructs a new SpoolDatabase.
     *
     * @param entityManager the Spool entity manager
     * @param flushExecutor the executor for async flush operations
     * @param logger        the logger
     */
    private SpoolDatabase(EntityManager entityManager, ExecutorService flushExecutor, Logger logger) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.flushExecutor = Objects.requireNonNull(flushExecutor, "flushExecutor");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Gets the underlying entity manager.
     *
     * @return the entity manager
     */
    public EntityManager entityManager() {
        return entityManager;
    }

    /**
     * Executes a runnable while holding the database lock.
     *
     * @param runnable the work to execute
     */
    public void runLocked(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        synchronized (lock) {
            runnable.run();
        }
    }

    /**
     * Executes a supplier while holding the database lock and returns the result.
     *
     * @param <T>      the return type
     * @param supplier the work to execute
     * @return the result of the supplier
     */
    public <T> T locked(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        synchronized (lock) {
            return supplier.get();
        }
    }

    /**
     * Flushes all pending changes to the database synchronously.
     */
    public void flush() {
        runLocked(() -> {
            try {
                entityManager.flush();
            } catch (Exception e) {
                logger.error("DB flush failed", e);
            }
        });
    }

    /**
     * Requests an asynchronous flush. Multiple requests may be coalesced.
     */
    public void flushAsync() {
        flushRequested.set(true);
        scheduleFlushWorker();
    }

    /**
     * Runs a task asynchronously on the flush executor.
     *
     * @param runnable the task to run
     * @return a CompletableFuture that completes when the task finishes
     */
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        if (runnable == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(runnable, flushExecutor);
    }

    /**
     * Supplies a value asynchronously on the flush executor.
     *
     * @param <T>      the result type
     * @param supplier the supplier to execute
     * @return a CompletableFuture that completes with the supplied value
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(supplier, flushExecutor);
    }

    /**
     * Runs a task asynchronously under the database lock.
     *
     * @param runnable the task to run
     * @return a CompletableFuture that completes when the task finishes
     */
    public CompletableFuture<Void> runLockedAsync(Runnable runnable) {
        if (runnable == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> runLocked(runnable));
    }

    /**
     * Supplies a value asynchronously under the database lock.
     *
     * @param <T>      the result type
     * @param supplier the supplier to execute
     * @return a CompletableFuture that completes with the supplied value
     */
    public <T> CompletableFuture<T> lockedAsync(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        return supplyAsync(() -> locked(supplier));
    }

    /**
     * Executes work within a database transaction.
     *
     * @param work the transactional work to execute
     */
    public void transactional(Runnable work) {
        runLocked(() -> entityManager.transactional(work));
    }

    /**
     * Executes work within a database transaction and returns a result.
     *
     * @param <T>  the result type
     * @param work the transactional work to execute
     * @return the result of the work
     */
    public <T> T transactional(Supplier<T> work) {
        return locked(() -> entityManager.transactional(work));
    }

    /**
     * Closes the database connection and shuts down the flush executor.
     */
    @Override
    public void close() {
        flushExecutor.shutdown();
        try {
            Connection conn = entityManager.getExecutor().getConnection();
            if (conn != null) conn.close();
        } catch (Exception e) {
            logger.debug("Error closing DB connection", e);
        }
    }

    /**
     * Schedules a flush worker if one is not already scheduled.
     */
    private void scheduleFlushWorker() {
        if (!flushWorkerScheduled.compareAndSet(false, true)) {
            return;
        }

        flushExecutor.execute(() -> {
            try {
                do {
                    flushRequested.set(false);
                    flush();
                } while (flushRequested.get());
            } finally {
                flushWorkerScheduled.set(false);
                if (flushRequested.get()) {
                    scheduleFlushWorker();
                }
            }
        });
    }

    /**
     * Builder for constructing {@link SpoolDatabase} instances with fluent configuration.
     */
    public static final class Builder {
        private static final AtomicInteger DB_THREAD_IDS = new AtomicInteger();

        private String jdbcUrl;
        private Logger logger;
        private Class<?>[] entities = new Class<?>[0];
        private boolean updateSchema = true;
        private boolean validateSchema = true;
        private ThreadFactory flushThreadFactory = r -> {
            Thread t = new Thread(r, "RapunzelLib-DBFlush-" + DB_THREAD_IDS.incrementAndGet());
            t.setDaemon(true);
            return t;
        };

        private Builder() {
        }

        /**
         * Sets the JDBC connection URL.
         *
         * @param jdbcUrl the JDBC URL
         * @return this builder
         */
        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        /**
         * Sets the logger.
         *
         * @param logger the logger
         * @return this builder
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Sets the entity classes to register with the ORM.
         *
         * @param entities the entity classes
         * @return this builder
         */
        public Builder entities(Class<?>... entities) {
            this.entities = (entities != null) ? entities : new Class<?>[0];
            return this;
        }

        /**
         * Sets whether to automatically update the database schema on build.
         *
         * @param updateSchema {@code true} to apply schema migrations
         * @return this builder
         */
        public Builder updateSchema(boolean updateSchema) {
            this.updateSchema = updateSchema;
            return this;
        }

        /**
         * Sets whether to validate the database schema on build.
         *
         * @param validateSchema {@code true} to validate
         * @return this builder
         */
        public Builder validateSchema(boolean validateSchema) {
            this.validateSchema = validateSchema;
            return this;
        }

        /**
         * Sets a custom thread factory for the flush executor.
         *
         * @param flushThreadFactory the thread factory
         * @return this builder
         */
        public Builder flushThreadFactory(ThreadFactory flushThreadFactory) {
            this.flushThreadFactory = Objects.requireNonNull(flushThreadFactory, "flushThreadFactory");
            return this;
        }

        /**
         * Builds the {@link SpoolDatabase} instance.
         *
         * @return a new SpoolDatabase
         * @throws NullPointerException if jdbcUrl or logger is null
         * @throws RuntimeException     if schema migration or validation fails
         */
        public SpoolDatabase build() {
            Objects.requireNonNull(jdbcUrl, "jdbcUrl");
            Objects.requireNonNull(logger, "logger");

            EntityManager em = EntityManager.create(jdbcUrl);
            if (entities.length > 0) {
                em.registerEntities(entities);
            }

            if (updateSchema) {
                try {
                    int migrations = em.updateSchema();
                    logger.info("Applied {} DB migrations", migrations);
                } catch (Exception e) {
                    throw new RuntimeException("DB migrations failed", e);
                }
            }

            if (validateSchema) {
                try {
                    boolean valid = em.validateSchema();
                    logger.info("DB schema valid: {}", valid);
                } catch (Exception e) {
                    throw new RuntimeException("DB schema validation failed", e);
                }
            }

            ExecutorService flushExecutor = Executors.newSingleThreadExecutor(flushThreadFactory);
            return new SpoolDatabase(em, flushExecutor, logger);
        }
    }
}
