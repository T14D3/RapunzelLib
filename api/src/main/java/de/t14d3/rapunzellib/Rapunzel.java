package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Rapunzel {
    private static final Object DEFAULT_OWNER = Rapunzel.class;
    private static final Object LOCK = new Object();
    private static volatile RapunzelContext context;
    private static final Map<Object, Lease> leases = new HashMap<>();

    private Rapunzel() {
    }

    public static boolean isBootstrapped() {
        return context != null;
    }

    public static RapunzelContext context() {
        RapunzelContext current = context;
        if (current == null) {
            throw new IllegalStateException("RapunzelLib context not bootstrapped yet");
        }
        return current;
    }

    public static Players players() {
        return context().players();
    }

    public static Worlds worlds() {
        return context().worlds();
    }

    public static Blocks blocks() {
        return context().blocks();
    }

    /**
     * Bootstraps the global {@link RapunzelContext} and associates it with an {@code owner}.
     *
     * <p>Multiple owners can share the same bootstrapped context. The context is only closed once the last owner calls
     * {@link #shutdown(Object)} (or closes the returned {@link Lease}).</p>
     *
     * <p>If a context is already bootstrapped, subsequent calls must pass the <em>same context instance</em> (otherwise an
     * exception is thrown). This prevents accidental replacement in shared classpaths.</p>
     *
     * @param owner a stable owner token (e.g. your plugin/mod instance)
     * @return a lease that releases this owner on {@link Lease#close()}
     */
    public static Lease bootstrap(Object owner, RapunzelContext newContext) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(newContext, "newContext");

        synchronized (LOCK) {
            RapunzelContext current = context;
            if (current == null) {
                context = newContext;
                current = newContext;
            } else if (current != newContext) {
                throw new IllegalStateException(
                    "RapunzelLib context already bootstrapped (existing=" + current.getClass().getName() +
                        ", attempted=" + newContext.getClass().getName() + ")"
                );
            }

            Lease existing = leases.get(owner);
            if (existing != null) return existing;

            Lease lease = new Lease(owner, current);
            leases.put(owner, lease);
            return lease;
        }
    }

    /**
     * Registers {@code owner} as a consumer of the already-bootstrapped context.
     *
     * <p>This does not create or replace the global context; it only prevents shutdown until the owner releases.</p>
     *
     * @throws IllegalStateException if no context is bootstrapped
     */
    public static Lease acquire(Object owner) {
        Objects.requireNonNull(owner, "owner");

        synchronized (LOCK) {
            RapunzelContext current = context;
            if (current == null) {
                throw new IllegalStateException("RapunzelLib context not bootstrapped yet");
            }

            Lease existing = leases.get(owner);
            if (existing != null) return existing;

            Lease lease = new Lease(owner, current);
            leases.put(owner, lease);
            return lease;
        }
    }

    /**
     * Bootstraps the global {@link RapunzelContext} using the default owner token.
     *
     * <p>Prefer {@link #bootstrap(Object, RapunzelContext)} in shared runtimes.</p>
     */
    public static void bootstrap(RapunzelContext newContext) {
        bootstrap(DEFAULT_OWNER, newContext);
    }

    /**
     * Releases {@code owner}. The global context is closed once no owners remain.
     */
    public static void shutdown(Object owner) {
        Objects.requireNonNull(owner, "owner");

        RapunzelContext toClose;
        synchronized (LOCK) {
            leases.remove(owner);
            if (!leases.isEmpty()) {
                return;
            }
            toClose = context;
            context = null;
        }

        if (toClose == null) return;
        try {
            toClose.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Releases the default owner token.
     *
     * <p>Prefer {@link #shutdown(Object)} in shared runtimes.</p>
     */
    public static void shutdown() {
        shutdown(DEFAULT_OWNER);
    }

    /**
     * Forcefully clears all owners and closes the global context.
     *
     * <p>This is primarily intended for tests or process shutdown.</p>
     */
    public static void shutdownAll() {
        RapunzelContext toClose;
        synchronized (LOCK) {
            leases.clear();
            toClose = context;
            context = null;
        }
        if (toClose == null) return;
        try {
            toClose.close();
        } catch (Exception ignored) {
        }
    }

    public static int ownerCount() {
        synchronized (LOCK) {
            return leases.size();
        }
    }

    public static final class Lease implements AutoCloseable {
        private final Object owner;
        private final RapunzelContext context;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Lease(Object owner, RapunzelContext context) {
            this.owner = owner;
            this.context = context;
        }

        public Object owner() {
            return owner;
        }

        public RapunzelContext context() {
            return context;
        }

        public boolean isClosed() {
            return closed.get();
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) return;
            Rapunzel.shutdown(owner);
        }
    }
}

