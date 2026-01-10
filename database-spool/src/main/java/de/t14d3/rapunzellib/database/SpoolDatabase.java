package de.t14d3.rapunzellib.database;

import de.t14d3.spool.core.EntityManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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

    private final Object lock = new Object();
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
        synchronized (lock) {
            runnable.run();
        }
    }

    public <T> T locked(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        synchronized (lock) {
            return supplier.get();
        }
    }

    public void flush() {
        runLocked(() -> {
            try {
                entityManager.flush();
            } catch (Exception e) {
                logger.error("DB flush failed", e);
            }
        });
    }

    public void flushAsync() {
        flushExecutor.execute(this::flush);
    }

    public void transactional(Runnable work) {
        runLocked(() -> entityManager.transactional(work));
    }

    public <T> T transactional(Supplier<T> work) {
        return locked(() -> entityManager.transactional(work));
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
            return new SpoolDatabase(em, flushExecutor, logger);
        }
    }
}

