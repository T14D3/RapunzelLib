package de.t14d3.rapunzellib.database;

import de.t14d3.spool.core.EntityManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Thread-safe database wrapper backed by the Spool ORM.
 * <p>
 * Relies on Spool's {@link EntityManager} having thread-safe, queue-based internals:
 * persist/remove are lock-free, find/flush synchronize only on the JDBC connection.
 * </p>
 */
public final class SpoolDatabase implements AutoCloseable {

    public static Builder builder() {
        return new Builder();
    }

    public static SpoolDatabase open(String jdbcUrl, Logger logger, Class<?>... entities) {
        return builder()
            .jdbcUrl(jdbcUrl)
            .logger(logger)
            .entities(entities)
            .build();
    }

    private final EntityManager entityManager;
    private final ExecutorService flushExecutor;
    private final Logger logger;

    private SpoolDatabase(EntityManager entityManager, ExecutorService flushExecutor, Logger logger) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.flushExecutor = Objects.requireNonNull(flushExecutor, "flushExecutor");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public void runLocked(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        runnable.run();
    }

    public <T> T locked(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier.get();
    }

    public void flush() {
        try {
            entityManager.flush();
        } catch (Exception e) {
            logger.error("DB flush failed", e);
        }
    }

    public CompletableFuture<Void> flushAsync() {
        return CompletableFuture.runAsync(this::flush, flushExecutor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        if (runnable == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(runnable, flushExecutor);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (supplier == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(supplier, flushExecutor);
    }

    public CompletableFuture<Void> runLockedAsync(Runnable runnable) {
        return runAsync(runnable);
    }

    public <T> CompletableFuture<T> lockedAsync(Supplier<T> supplier) {
        return supplyAsync(supplier);
    }

    public void transactional(Runnable work) {
        entityManager.transactional(work);
    }

    public <T> T transactional(Supplier<T> work) {
        return entityManager.transactional(work);
    }

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

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder entities(Class<?>... entities) {
            this.entities = (entities != null) ? entities : new Class<?>[0];
            return this;
        }

        public Builder updateSchema(boolean updateSchema) {
            this.updateSchema = updateSchema;
            return this;
        }

        public Builder validateSchema(boolean validateSchema) {
            this.validateSchema = validateSchema;
            return this;
        }

        public Builder flushThreadFactory(ThreadFactory flushThreadFactory) {
            this.flushThreadFactory = Objects.requireNonNull(flushThreadFactory, "flushThreadFactory");
            return this;
        }

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
            return new SpoolDatabase(em, new TrackingExecutor(flushExecutor, logger, 10_000L), logger);
        }
    }

    /**
     * Delegating {@link ExecutorService} that logs any task running longer than
     * the given threshold (with the stack trace at completion). A stuck task on
     * the single-thread flush executor silently stalls every queued DB
     * write/flush (e.g. homes never persisting), so visibility matters.
     */
    private static final class TrackingExecutor implements ExecutorService {
        private final ExecutorService delegate;
        private final Logger logger;
        private final long timeoutMillis;

        TrackingExecutor(ExecutorService delegate, Logger logger, long timeoutMillis) {
            this.delegate = delegate;
            this.logger = logger;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public void execute(Runnable command) {
            long started = System.currentTimeMillis();
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    long took = System.currentTimeMillis() - started;
                    if (took > timeoutMillis) {
                        logger.error("[DBFlush] Task took {}ms (>{}ms) on the flush executor - possible stall; stack:", took, timeoutMillis);
                        for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
                            logger.error("    at {}", el);
                        }
                    }
                }
            });
        }

        @Override
        public void shutdown() { delegate.shutdown(); }

        @Override
        public java.util.List<Runnable> shutdownNow() { return delegate.shutdownNow(); }

        @Override
        public boolean isShutdown() { return delegate.isShutdown(); }

        @Override
        public boolean isTerminated() { return delegate.isTerminated(); }

        @Override
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
            java.util.concurrent.CompletableFuture<T> cf = new java.util.concurrent.CompletableFuture<>();
            execute(() -> {
                try { cf.complete(task.call()); } catch (Throwable t) { cf.completeExceptionally(t); }
            });
            return cf;
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
            return submit(java.util.concurrent.Executors.callable(task, result));
        }

        @Override
        public java.util.concurrent.Future<?> submit(Runnable task) {
            return submit(java.util.concurrent.Executors.callable(task));
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) throws InterruptedException, java.util.concurrent.ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }
}
