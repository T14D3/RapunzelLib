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

    /**
     * Returns the shared {@link RapunzelRuntime} instance.
     *
     * @return the shared runtime instance
     */
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
     * @param context the context to scope to
     * @param action  the action to execute
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
     * @param context the context to scope to
     * @param action  the supplier to execute
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

    /**
     * Returns the {@link ServiceRegistry} from the current context.
     *
     * @return the service registry
     */
    public static @NotNull ServiceRegistry services() {
        return context().services();
    }

    /**
     * Returns a required service of the given type from the current context.
     *
     * @param type the service class
     * @param <T>  the service type
     * @return the service instance
     */
    public static <T> @NotNull T service(@NotNull Class<T> type) {
        return services().get(Objects.requireNonNull(type, "type"));
    }

    /**
     * Finds an optional service of the given type from the current context.
     *
     * @param type the service class
     * @param <T>  the service type
     * @return an {@link Optional} containing the service, or empty if not registered
     */
    public static <T> @NotNull Optional<T> findService(@NotNull Class<T> type) {
        return services().find(Objects.requireNonNull(type, "type"));
    }

    /**
     * Returns the {@link Scheduler} from the current context.
     *
     * @return the scheduler
     */
    public static @NotNull Scheduler scheduler() {
        return context().scheduler();
    }

    /**
     * Returns the {@link ConfigService} from the current context.
     *
     * @return the config service
     */
    public static @NotNull ConfigService configs() {
        return context().configs();
    }

    /**
     * Returns the {@link MessageFormatService} from the current context.
     *
     * @return the message format service
     */
    public static @NotNull MessageFormatService messages() {
        return context().messages();
    }

    /**
     * Returns the {@link Logger} from the current context.
     *
     * @return the logger
     */
    public static @NotNull Logger logger() {
        return context().logger();
    }

    /**
     * Returns the data directory path from the current context.
     *
     * @return the data directory path
     */
    public static @NotNull Path dataDirectory() {
        return context().dataDirectory();
    }

    /**
     * Returns the {@link PlatformRuntime} from the current context.
     *
     * @return the platform runtime
     */
    public static @NotNull PlatformRuntime runtime() {
        return context().runtime();
    }

    /**
     * Returns the {@link PlatformId} from the current context.
     *
     * @return the platform identifier
     */
    public static @NotNull PlatformId platformId() {
        return context().platformId();
    }

    /**
     * Returns the {@link RNativeInterop} from the current context, if available.
     *
     * @return an {@link Optional} containing the native interop, or empty if not supported
     */
    public static @NotNull Optional<RNativeInterop> nativeInterop() {
        return findContext().flatMap(RapunzelContext::nativeInterop);
    }

    /**
     * Returns the {@link RRegistryAccess} from the current context.
     *
     * @return the registry access
     */
    public static @NotNull RRegistryAccess registries() {
        return context().registries();
    }

    /**
     * Returns the {@link Players} access from the current context.
     *
     * @return the players access
     */
    public static @NotNull Players players() {
        return context().players();
    }

    /**
     * Returns the {@link Entities} access from the current context.
     *
     * @return the entities access
     */
    public static @NotNull Entities entities() {
        return context().entities();
    }

    /**
     * Returns the {@link REntityTypeRegistry} from the current context.
     *
     * @return the entity type registry
     */
    public static @NotNull REntityTypeRegistry entityTypes() {
        return context().entityTypes();
    }

    /**
     * Returns the {@link RItemTypeRegistry} from the current context.
     *
     * @return the item type registry
     */
    public static @NotNull RItemTypeRegistry itemTypes() {
        return context().itemTypes();
    }

    /**
     * Returns the {@link RBlockTypeRegistry} from the current context.
     *
     * @return the block type registry
     */
    public static @NotNull RBlockTypeRegistry blockTypes() {
        return context().blockTypes();
    }

    /**
     * Returns the {@link Worlds} access from the current context.
     *
     * @return the worlds access
     */
    public static @NotNull Worlds worlds() {
        return context().worlds();
    }

    /**
     * Returns the {@link Blocks} access from the current context.
     *
     * @return the blocks access
     */
    public static @NotNull Blocks blocks() {
        return context().blocks();
    }

    /**
     * Returns the {@link AttachmentSupport} instance for managing attachments.
     *
     * @return the attachment support
     */
    public static @NotNull AttachmentSupport attachments() {
        return AttachmentFeatures.install();
    }

    /**
     * Returns whether the given native object supports attachments.
     *
     * @param target the native object to check
     * @return true if attachments are supported for this target
     */
    public static boolean supportsAttachments(@NotNull RNative target) {
        return AttachmentFeatures.supports(target);
    }

    /**
     * Requires that the given native object supports attachments, or throws.
     *
     * @param target the native object
     * @param <T>    the native type
     * @return the same target if attachment support is available
     */
    public static <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return AttachmentFeatures.requireSupported(target);
    }

    /**
     * Returns the attachment container for the given native object.
     *
     * @param target the native object
     * @return the attachment container
     */
    public static @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return AttachmentFeatures.attachments(target);
    }

    /**
     * Bootstraps a new context for the given owner, using the provided context instance.
     *
     * @param owner      the owner object
     * @param newContext the context to bootstrap
     * @return a {@link BootstrapHandle} for managing the lifecycle
     */
    public static @NotNull BootstrapHandle bootstrap(@NotNull Object owner, @NotNull RapunzelContext newContext) {
        Objects.requireNonNull(newContext, "newContext");
        return bootstrap(owner, () -> newContext);
    }

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
     * @param owner the borrower object
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
     * Bootstraps a context for the default owner.
     *
     * @param newContext the context to bootstrap
     */
    public static void bootstrap(@NotNull RapunzelContext newContext) {
        bootstrap(DEFAULT_OWNER, newContext);
    }

    /**
     * Shuts down the context owned by the given owner.
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

    /**
     * Shuts down the context owned by the default owner.
     */
    public static void shutdown() {
        shutdown(DEFAULT_OWNER);
    }

    /**
     * Shuts down all active contexts and the shared runtime.
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

    /**
     * Returns the number of registered owners.
     *
     * @return the owner count
     */
    public static int ownerCount() {
        synchronized (LOCK) {
            return ownerHandles.size();
        }
    }

    /**
     * Returns the number of registered borrowers.
     *
     * @return the borrower count
     */
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
