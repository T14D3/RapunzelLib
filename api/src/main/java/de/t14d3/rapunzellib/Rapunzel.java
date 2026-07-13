package de.t14d3.rapunzellib;

import de.t14d3.rapunzellib.attachments.AttachmentFeatures;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.bootstrap.BootstrapHandle;
import de.t14d3.rapunzellib.bootstrap.BootstrapOwnerRole;
import de.t14d3.rapunzellib.bootstrap.BootstrapState;
import de.t14d3.rapunzellib.bootstrap.PlatformBootstrapHost;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RapunzelRuntime;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Main entry point for the RapunzelLib API.
 *
 * <p>Provides static factory methods to access all RapunzelLib subsystems including
 * registries, entities, players, worlds, configuration, messaging, scheduling, and
 * attachment support. Also manages the lifecycle of {@link RapunzelContext} instances
 * through bootstrap and shutdown operations.</p>
 *
 * <p>This class is thread-safe. All public methods are safe to call from any thread
 * unless otherwise noted.</p>
 */
public final class Rapunzel {
    private static final Object DEFAULT_OWNER = Rapunzel.class;
    private static final Object LOCK = new Object();
    private static final ThreadLocal<RapunzelContext> CURRENT_CONTEXT = new ThreadLocal<>();
    private static final RapunzelRuntime RUNTIME = RapunzelRuntime.getInstance();
    private static final Map<Object, BootstrapHandle> ownerHandles = new LinkedHashMap<>();
    private static final Map<Object, BootstrapHandle> borrowerHandles = new LinkedHashMap<>();
    private static final Map<Object, RapunzelContext> contexts = new LinkedHashMap<>();
    private static volatile PlatformBootstrapHost registeredPlatformHost;

    private Rapunzel() {
    }

    /** Returns the shared {@link RapunzelRuntime} instance. */
    public static @NotNull RapunzelRuntime sharedRuntime() {
        return RUNTIME;
    }

    /**
     * Returns whether at least one context has been bootstrapped.
     *
     * @return true if any context is active
     */
    public static boolean isBootstrapped() {
        synchronized (LOCK) {
            return !contexts.isEmpty();
        }
    }

    /**
     * Returns the current or only active {@link RapunzelContext}.
     *
     * @return the current context
     * @throws IllegalStateException if no context is active or multiple contexts exist without scoping
     */
    public static @NotNull RapunzelContext context() {
        return findContext().orElseThrow(() -> new IllegalStateException(ambiguousContextMessage()));
    }

    /**
     * Finds the current or only active context, if available.
     *
     * @return an {@link Optional} containing the context, or empty if inactive or ambiguous
     */
    public static @NotNull Optional<RapunzelContext> findContext() {
        RapunzelContext scoped = CURRENT_CONTEXT.get();
        if (scoped != null) {
            return Optional.of(scoped);
        }
        synchronized (LOCK) {
            return contexts.size() == 1 ? contexts.values().stream().findFirst() : Optional.empty();
        }
    }

    /**
     * Executes the given action within the scope of the specified context.
     *
     * <p>Within the scoped block, {@link #context()} returns the given context,
     * enabling static accessor methods to resolve correctly when multiple
     * contexts are active. The previous context is restored afterward.</p>
     *
     * @param context the context to scope to
     * @param action  the action to execute within the scoped context
     */
    public static void withContext(@NotNull RapunzelContext context, @NotNull Runnable action) {
        Objects.requireNonNull(action, "action");
        withContext(context, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Executes the given supplier within the scope of the specified context and returns its result.
     *
     * <p>Within the scoped block, {@link #context()} returns the given context.
     * The previous context is restored in the finally block.</p>
     *
     * @param context the context to scope to
     * @param action  the supplier to execute within the scoped context
     * @param <T>     the return type of the supplier
     * @return the value returned by the supplier
     */
    public static <T> T withContext(@NotNull RapunzelContext context, @NotNull Supplier<T> action) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(action, "action");
        RapunzelContext previous = CURRENT_CONTEXT.get();
        CURRENT_CONTEXT.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT_CONTEXT.remove();
            } else {
                CURRENT_CONTEXT.set(previous);
            }
        }
    }

    /** Returns the {@link ServiceRegistry} from the current context. */
    public static @NotNull ServiceRegistry services() {
        return context().services();
    }

    /** Returns a required service of the given type from the current context. */
    public static <T> @NotNull T service(@NotNull Class<T> type) {
        return services().get(Objects.requireNonNull(type, "type"));
    }

    /** Finds an optional service of the given type from the current context. */
    public static <T> @NotNull Optional<T> findService(@NotNull Class<T> type) {
        return services().find(Objects.requireNonNull(type, "type"));
    }

    /** Returns the {@link Scheduler} from the current context. */
    public static @NotNull Scheduler scheduler() {
        return context().scheduler();
    }

    /** Returns the {@link ConfigService} from the current context. */
    public static @NotNull ConfigService configs() {
        return context().configs();
    }

    /** Returns the {@link MessageFormatService} from the current context. */
    public static @NotNull MessageFormatService messages() {
        return context().messages();
    }

    /** Returns the {@link Logger} from the current context. */
    public static @NotNull Logger logger() {
        return context().logger();
    }

    /** Returns the data directory path from the current context. */
    public static @NotNull Path dataDirectory() {
        return context().dataDirectory();
    }

    /** Returns the {@link PlatformRuntime} from the current context. */
    public static @NotNull PlatformRuntime runtime() {
        return context().runtime();
    }

    /** Returns the {@link PlatformId} from the current context. */
    public static @NotNull PlatformId platformId() {
        return context().platformId();
    }

    /** Returns the {@link RNativeInterop} from the current context, if available. */
    public static @NotNull Optional<RNativeInterop> nativeInterop() {
        return findContext().flatMap(RapunzelContext::nativeInterop);
    }

    /** Returns the {@link RRegistryAccess} from the current context. */
    public static @NotNull RRegistryAccess registries() {
        return context().registries();
    }

    /** Returns the {@link Players} access from the current context. */
    public static @NotNull Players players() {
        return context().players();
    }

    /** Returns the {@link Entities} access from the current context. */
    public static @NotNull Entities entities() {
        return context().entities();
    }

    /** Returns the {@link REntityTypeRegistry} from the current context. */
    public static @NotNull REntityTypeRegistry entityTypes() {
        return context().entityTypes();
    }

    /** Returns the {@link RItemTypeRegistry} from the current context. */
    public static @NotNull RItemTypeRegistry itemTypes() {
        return context().itemTypes();
    }

    /** Returns the {@link RBlockTypeRegistry} from the current context. */
    public static @NotNull RBlockTypeRegistry blockTypes() {
        return context().blockTypes();
    }

    /** Returns the {@link Worlds} access from the current context. */
    public static @NotNull Worlds worlds() {
        return context().worlds();
    }

    /** Returns the {@link Blocks} access from the current context. */
    public static @NotNull Blocks blocks() {
        return context().blocks();
    }

    /** Returns the {@link AttachmentSupport} instance for managing attachments. */
    public static @NotNull AttachmentSupport attachments() {
        return AttachmentFeatures.install();
    }

    /** Returns whether the given native object supports attachments. */
    public static boolean supportsAttachments(@NotNull RNative target) {
        return AttachmentFeatures.supports(target);
    }

    /** Requires that the given native object supports attachments, or throws. */
    public static <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return AttachmentFeatures.requireSupported(target);
    }

    /** Returns the attachment container for the given native object. */
    public static @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return AttachmentFeatures.attachments(target);
    }

    /**
     * Bootstraps a new context for the given owner.
     *
     * @param owner      the plugin or module that will own the context
     * @param newContext the context to bootstrap
     * @return a {@link BootstrapHandle} representing the owner's lifecycle participation
     */
    public static @NotNull BootstrapHandle bootstrap(@NotNull Object owner, @NotNull RapunzelContext newContext) {
        Objects.requireNonNull(newContext, "newContext");
        return bootstrap(owner, () -> newContext);
    }

    /**
     * Bootstraps a new context for the given owner, created on demand via the supplier.
     *
     * <p>If the owner already has an open handle, the existing one is returned
     * and the supplier is not invoked.</p>
     *
     * @param owner          the plugin or module that will own the context
     * @param contextFactory a supplier that creates the context when needed
     * @return a {@link BootstrapHandle} representing the owner's lifecycle participation
     */
    public static @NotNull BootstrapHandle bootstrap(
        @NotNull Object owner,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(contextFactory, "contextFactory");

        synchronized (LOCK) {
            BootstrapHandle existing = ownerHandles.get(owner);
            if (existing != null && !existing.isClosed()) {
                return existing;
            }

            RapunzelContext created = Objects.requireNonNull(contextFactory.get(), "contextFactory.get()");
            BootstrapHandle handle = new BootstrapHandle(owner, created, BootstrapOwnerRole.OWNER, () -> Rapunzel.shutdown(owner));
            contexts.put(owner, created);
            ownerHandles.put(owner, handle);
            return handle;
        }
    }

    /**
     * Bootstraps or acquires an existing handle for the given owner.
     *
     * @param owner          the owner object
     * @param contextFactory supplier that creates the context if no existing handle is found
     * @return a {@link BootstrapHandle} for managing the lifecycle
     */
    public static @NotNull BootstrapHandle bootstrapOrAcquire(
        @NotNull Object owner,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        return bootstrap(owner, contextFactory);
    }

    /**
     * Acquires a borrower handle to the single active context.
     *
     * <p>Borrowers share the context without owning it. When the borrower's handle
     * is closed, only the borrower reference is released; the context itself is
     * not shut down unless the owner closes it.</p>
     *
     * @param owner the borrower object (plugin or module)
     * @return a {@link BootstrapHandle} for the borrowed context
     * @throws IllegalStateException if no single active context exists
     */
    public static @NotNull BootstrapHandle acquire(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");

        synchronized (LOCK) {
            BootstrapHandle owned = ownerHandles.get(owner);
            if (owned != null) {
                return owned;
            }
            if (contexts.size() != 1) {
                throw new IllegalStateException(ambiguousContextMessage());
            }
            BootstrapHandle existing = borrowerHandles.get(owner);
            if (existing != null) {
                return existing;
            }
            RapunzelContext current = contexts.values().iterator().next();
            BootstrapHandle borrowed = new BootstrapHandle(owner, current, BootstrapOwnerRole.BORROWER, () -> Rapunzel.shutdown(owner));
            borrowerHandles.put(owner, borrowed);
            return borrowed;
        }
    }

    /**
     * Acquires a borrower handle backed by the given consumer-level context view.
     *
     * <p>This is the consumer equivalent of {@link #acquire(Object)} - instead of
     * returning the raw platform context, the caller provides a consumer-specific
     * view (such as a {@code ConsumerView}) that wraps the shared platform context
     * with the consumer's own logger, data directory, and resource provider.</p>
     *
     * @param owner   the borrower object (plugin or module)
     * @param view    the consumer-specific context view
     * @return a {@link BootstrapHandle} for the borrowed view
     * @throws IllegalStateException if no platform context has been registered
     */
    public static @NotNull BootstrapHandle acquire(
        @NotNull Object owner,
        @NotNull RapunzelContext view
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(view, "view");

        synchronized (LOCK) {
            BootstrapHandle owned = ownerHandles.get(owner);
            if (owned != null) {
                return owned;
            }
            if (contexts.isEmpty()) {
                throw new IllegalStateException(
                    "No active RapunzelLib context. "
                        + "Ensure the platform plugin has been loaded and has bootstrapped.");
            }
            BootstrapHandle existing = borrowerHandles.get(owner);
            if (existing != null) {
                return existing;
            }
            BootstrapHandle borrowed = new BootstrapHandle(
                owner, view, BootstrapOwnerRole.BORROWER,
                () -> Rapunzel.shutdown(owner)
            );
            borrowerHandles.put(owner, borrowed);
            return borrowed;
        }
    }

    /** Bootstraps a context for the default owner. */
    public static void bootstrap(@NotNull RapunzelContext newContext) {
        bootstrap(DEFAULT_OWNER, newContext);
    }

    /**
     * Shuts down the context owned by the given owner.
     *
     * <p>Removes the owner's handle and closes the associated context.
     * If the owner is a borrower, only the borrower reference is removed.</p>
     *
     * @param owner the owner whose context to shut down
     */
    public static void shutdown(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");
        RapunzelContext toClose;
        synchronized (LOCK) {
            borrowerHandles.remove(owner);
            ownerHandles.remove(owner);
            toClose = contexts.remove(owner);
        }
        closeContext(toClose);
    }

    /** Shuts down the context owned by the default owner. */
    public static void shutdown() {
        shutdown(DEFAULT_OWNER);
    }

    /**
     * Shuts down all active contexts and the shared runtime.
     *
     * <p>Closes every tracked context in order and then shuts down the
     * shared {@link RapunzelRuntime}. Errors from individual close operations
     * are logged but do not prevent remaining contexts from closing.</p>
     */
    public static void shutdownAll() {
        RapunzelContext[] toClose;
        synchronized (LOCK) {
            borrowerHandles.clear();
            ownerHandles.clear();
            toClose = contexts.values().toArray(RapunzelContext[]::new);
            contexts.clear();
        }
        for (RapunzelContext context : toClose) {
            closeContext(context);
        }
        try {
            RUNTIME.shutdown();
        } catch (Exception e) {
            System.err.println("Failed to close RapunzelLib runtime services: " + e);
            e.printStackTrace(System.err);
        }
    }

    /** Returns the number of registered owners. */
    public static int ownerCount() {
        synchronized (LOCK) {
            return ownerHandles.size();
        }
    }

    /** Returns the number of registered borrowers. */
    public static int borrowerCount() {
        synchronized (LOCK) {
            return borrowerHandles.size();
        }
    }

    /**
     * Returns the current bootstrap state including the active context, owner, borrower count, and registered host.
     *
     * @return the bootstrap state snapshot
     */
    public static @NotNull BootstrapState bootstrapState() {
        synchronized (LOCK) {
            RapunzelContext singleContext = contexts.size() == 1 ? contexts.values().iterator().next() : null;
            Object singleOwner = contexts.size() == 1 ? contexts.keySet().iterator().next() : null;
            return new BootstrapState(singleContext, singleOwner, borrowerHandles.size(), registeredPlatformHost);
        }
    }

    /**
     * Registers a platform bootstrap host for automatic lifecycle management.
     *
     * @param host the platform bootstrap host to register
     */
    public static void registerPlatformBootstrapHost(@NotNull PlatformBootstrapHost host) {
        Objects.requireNonNull(host, "host");
        synchronized (LOCK) {
            registeredPlatformHost = host;
        }
    }

    /**
     * Clears the registered platform bootstrap host.
     */
    public static void clearRegisteredPlatformBootstrapHost() {
        synchronized (LOCK) {
            registeredPlatformHost = null;
        }
    }

    /**
     * Returns the registered platform bootstrap host, if any.
     *
     * @return an {@link Optional} containing the host, or empty if none is registered
     */
    public static @NotNull Optional<PlatformBootstrapHost> registeredPlatformBootstrapHost() {
        return Optional.ofNullable(registeredPlatformHost);
    }

    /**
     * Builds an ambiguous-context error message based on the current state.
     *
     * @return a descriptive error message
     */
    private static @NotNull String ambiguousContextMessage() {
        synchronized (LOCK) {
            if (contexts.isEmpty()) {
                return "RapunzelLib context not bootstrapped yet";
            }
            return "RapunzelLib context is ambiguous because " + contexts.size()
                + " consumer contexts are active. Use your plugin-owned RapunzelContext or Rapunzel.withContext(...).";
        }
    }

    /**
     * Safely closes the given context, logging any errors.
     *
     * @param toClose the context to close, may be null
     */
    private static void closeContext(RapunzelContext toClose) {
        if (toClose == null) return;
        try {
            toClose.close();
        } catch (Exception e) {
            try {
                toClose.logger().warn("Failed to close RapunzelLib context", e);
            } catch (Exception logError) {
                System.err.println("Failed to close RapunzelLib context: " + e);
                e.printStackTrace(System.err);
                System.err.println("Additionally failed to log close failure: " + logError);
                logError.printStackTrace(System.err);
            }
        }
    }
}
