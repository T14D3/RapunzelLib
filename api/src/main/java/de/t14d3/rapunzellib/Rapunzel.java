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
 * attachment support. Manages a single global {@link RapunzelContext} bootstrapped
 * by the platform plugin/mod. Consumer plugins acquire a borrower view of the shared
 * context via {@link #acquire}.</p>
 *
 * <p>This class is thread-safe. All public methods are safe to call from any thread
 * unless otherwise noted.</p>
 */
public final class Rapunzel {
    private static final Object DEFAULT_OWNER = Rapunzel.class;
    private static final Object LOCK = new Object();
    private static final RapunzelRuntime RUNTIME = RapunzelRuntime.getInstance();
    private static final Map<Object, BootstrapHandle> ownerHandles = new LinkedHashMap<>();
    private static final Map<Object, BootstrapHandle> borrowerHandles = new LinkedHashMap<>();
    private static volatile RapunzelContext context;
    private static volatile PlatformBootstrapHost registeredPlatformHost;

    private Rapunzel() {
    }

    /** Returns the shared {@link RapunzelRuntime} instance. */
    public static @NotNull RapunzelRuntime sharedRuntime() {
        return RUNTIME;
    }

    /**
     * Returns whether the single global context has been bootstrapped.
     *
     * @return true if the context is active
     */
    public static boolean isBootstrapped() {
        return context != null;
    }

    /**
     * Returns the single global {@link RapunzelContext}.
     *
     * @return the active context
     * @throws IllegalStateException if no context has been bootstrapped
     */
    public static @NotNull RapunzelContext context() {
        RapunzelContext current = context;
        if (current == null) {
            throw new IllegalStateException("RapunzelLib context not bootstrapped yet");
        }
        return current;
    }

    /**
     * Finds the single global context, if available.
     *
     * @return an {@link Optional} containing the context, or empty if not bootstrapped
     */
    public static @NotNull Optional<RapunzelContext> findContext() {
        return Optional.ofNullable(context);
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
     * Bootstraps the single global context for the given owner.
     *
     * @param owner      the plugin or module that will own the context
     * @param newContext the context to bootstrap
     * @return a {@link BootstrapHandle} representing the owner's lifecycle participation
     * @throws IllegalStateException if a context is already bootstrapped by a different owner
     */
    public static @NotNull BootstrapHandle bootstrap(@NotNull Object owner, @NotNull RapunzelContext newContext) {
        Objects.requireNonNull(newContext, "newContext");
        return bootstrap(owner, () -> newContext);
    }

    /**
     * Bootstraps the single global context for the given owner, created on demand via the supplier.
     *
     * <p>If the owner already has an open handle, the existing one is returned
     * and the supplier is not invoked.</p>
     *
     * @param owner          the plugin or module that will own the context
     * @param contextFactory a supplier that creates the context when needed
     * @return a {@link BootstrapHandle} representing the owner's lifecycle participation
     * @throws IllegalStateException if a context is already bootstrapped by a different owner
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
            if (context != null && !ownerHandles.containsKey(owner)) {
                throw new IllegalStateException(
                    "RapunzelLib context has already been bootstrapped by a different owner. "
                        + "Use Rapunzel.acquire() to obtain a consumer view.");
            }

            RapunzelContext created = Objects.requireNonNull(contextFactory.get(), "contextFactory.get()");
            BootstrapHandle handle = new BootstrapHandle(owner, created, BootstrapOwnerRole.OWNER, () -> Rapunzel.shutdown(owner));
            context = created;
            ownerHandles.put(owner, handle);
            return handle;
        }
    }

    /**
     * Acquires a borrower handle to the single global context.
     *
     * <p>Borrowers share the context without owning it. When the borrower's handle
     * is closed, only the borrower reference is released; the context itself is
     * not shut down unless the owner closes it.</p>
     *
     * @param owner the borrower object (plugin or module)
     * @return a {@link BootstrapHandle} for the borrowed context
     * @throws IllegalStateException if no context has been bootstrapped
     */
    public static @NotNull BootstrapHandle acquire(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");

        synchronized (LOCK) {
            BootstrapHandle owned = ownerHandles.get(owner);
            if (owned != null) {
                return owned;
            }
            if (context == null) {
                throw new IllegalStateException(
                    "No active RapunzelLib context. "
                        + "Ensure the platform plugin has been loaded and has bootstrapped.");
            }
            BootstrapHandle existing = borrowerHandles.get(owner);
            if (existing != null) {
                return existing;
            }
            BootstrapHandle borrowed = new BootstrapHandle(owner, context, BootstrapOwnerRole.BORROWER, () -> Rapunzel.shutdown(owner));
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
            if (context == null) {
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

    /** Shuts down the context for the default owner. */
    public static void shutdown() {
        shutdown(DEFAULT_OWNER);
    }

    /**
     * Shuts down the context owned or borrowed by the given owner.
     *
     * <p>If the owner is the bootstrap owner, the global context is closed and
     * the field is cleared. If the owner is a borrower, only the borrower
     * reference is removed.</p>
     *
     * @param owner the owner whose participation to shut down
     */
    public static void shutdown(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");
        RapunzelContext toClose = null;
        synchronized (LOCK) {
            borrowerHandles.remove(owner);
            BootstrapHandle removed = ownerHandles.remove(owner);
            if (removed != null) {
                toClose = context;
                context = null;
            }
        }
        closeContext(toClose);
    }

    /**
     * Shuts down the global context and the shared runtime.
     *
     * <p>Closes the context and then shuts down the shared {@link RapunzelRuntime}.
     * Errors from close operations are logged but do not prevent the runtime from
     * shutting down.</p>
     */
    public static void shutdownAll() {
        RapunzelContext toClose;
        synchronized (LOCK) {
            borrowerHandles.clear();
            ownerHandles.clear();
            toClose = context;
            context = null;
        }
        closeContext(toClose);
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
            Object singleOwner = ownerHandles.size() == 1 ? ownerHandles.keySet().iterator().next() : null;
            return new BootstrapState(context, singleOwner, borrowerHandles.size(), registeredPlatformHost);
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
