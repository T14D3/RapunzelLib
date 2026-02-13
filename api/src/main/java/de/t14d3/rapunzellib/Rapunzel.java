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
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class Rapunzel {
    private static final Object DEFAULT_OWNER = Rapunzel.class;
    private static final Object LOCK = new Object();
    private static volatile RapunzelContext context;
    private static volatile Object ownerToken;
    private static volatile PlatformBootstrapHost registeredPlatformHost;
    private static BootstrapHandle ownerHandle;
    private static final Map<Object, BootstrapHandle> borrowerHandles = new HashMap<>();

    private Rapunzel() {
    }

    public static boolean isBootstrapped() {
        return context != null;
    }

    public static @NotNull RapunzelContext context() {
        RapunzelContext current = context;
        if (current == null) {
            throw new IllegalStateException("RapunzelLib context not bootstrapped yet");
        }
        return current;
    }

    public static @NotNull Optional<RapunzelContext> findContext() {
        return Optional.ofNullable(context);
    }

    public static @NotNull ServiceRegistry services() {
        return context().services();
    }

    /**
     * Returns the registered service for {@code type} from the global service registry.
     */
    public static <T> @NotNull T service(@NotNull Class<T> type) {
        return services().get(Objects.requireNonNull(type, "type"));
    }

    /**
     * Returns the registered service for {@code type} when present.
     */
    public static <T> @NotNull Optional<T> findService(@NotNull Class<T> type) {
        return services().find(Objects.requireNonNull(type, "type"));
    }

    public static @NotNull Scheduler scheduler() {
        return context().scheduler();
    }

    public static @NotNull ConfigService configs() {
        return context().configs();
    }

    public static @NotNull MessageFormatService messages() {
        return context().messages();
    }

    public static @NotNull Logger logger() {
        return context().logger();
    }

    public static @NotNull Path dataDirectory() {
        return context().dataDirectory();
    }

    public static @NotNull PlatformRuntime runtime() {
        return context().runtime();
    }

    public static @NotNull PlatformId platformId() {
        return context().platformId();
    }

    public static @NotNull Optional<RNativeInterop> nativeInterop() {
        return findContext().flatMap(RapunzelContext::nativeInterop);
    }

    public static @NotNull RRegistryAccess registries() {
        return context().registries();
    }

    public static @NotNull Players players() {
        return context().players();
    }

    public static @NotNull Entities entities() {
        return context().entities();
    }

    public static @NotNull REntityTypeRegistry entityTypes() {
        return context().entityTypes();
    }

    public static @NotNull RItemTypeRegistry itemTypes() {
        return context().itemTypes();
    }

    public static @NotNull RBlockTypeRegistry blockTypes() {
        return context().blockTypes();
    }

    public static @NotNull Worlds worlds() {
        return context().worlds();
    }

    public static @NotNull Blocks blocks() {
        return context().blocks();
    }

    public static @NotNull AttachmentSupport attachments() {
        return AttachmentFeatures.install();
    }

    public static boolean supportsAttachments(@NotNull RNative target) {
        return AttachmentFeatures.supports(target);
    }

    public static <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return AttachmentFeatures.requireSupported(target);
    }

    public static @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return AttachmentFeatures.attachments(target);
    }

    /**
     * Bootstraps the global {@link RapunzelContext} for {@code owner}.
     *
     * <p>The first successful bootstrap becomes the sole owner. Later callers receive borrower handles for the existing
     * global context. If a canonical platform host is registered before bootstrap, it is given first chance to claim
     * ownership and create the context.</p>
     */
    public static @NotNull BootstrapHandle bootstrap(@NotNull Object owner, @NotNull RapunzelContext newContext) {
        Objects.requireNonNull(newContext, "newContext");
        return bootstrap(owner, () -> newContext);
    }

    /**
     * Bootstraps the global {@link RapunzelContext} for {@code owner} using a lazy context factory.
     */
    public static @NotNull BootstrapHandle bootstrap(
        @NotNull Object owner,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(contextFactory, "contextFactory");

        synchronized (LOCK) {
            RapunzelContext current = context;
            if (current != null) {
                return handleFor(owner, current);
            }

            BootstrapHandle hosted = tryBootstrapRegisteredHost(owner, contextFactory);
            if (hosted != null) {
                return hosted;
            }

            RapunzelContext created = Objects.requireNonNull(contextFactory.get(), "contextFactory.get()");
            return installOwnedContext(owner, owner, created);
        }
    }

    /**
     * Compatibility alias for {@link #bootstrap(Object, Supplier)}.
     */
    public static @NotNull BootstrapHandle bootstrapOrAcquire(
        @NotNull Object owner,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        return bootstrap(owner, contextFactory);
    }

    /**
     * Registers {@code owner} as a borrower of the already-bootstrapped context.
     *
     * @throws IllegalStateException if no context is bootstrapped
     */
    public static @NotNull BootstrapHandle acquire(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");

        synchronized (LOCK) {
            RapunzelContext current = context;
            if (current == null) {
                throw new IllegalStateException("RapunzelLib context not bootstrapped yet");
            }
            return handleFor(owner, current);
        }
    }

    /**
     * Bootstraps the global {@link RapunzelContext} using the default owner token.
     *
     * <p>Prefer {@link #bootstrap(Object, Supplier)} in shared runtimes.</p>
     */
    public static void bootstrap(@NotNull RapunzelContext newContext) {
        bootstrap(DEFAULT_OWNER, newContext);
    }

    /**
     * Releases {@code owner}. Borrowers only release their local handle. The current global context is torn down only
     * when the active owner releases.
     */
    public static void shutdown(@NotNull Object owner) {
        Objects.requireNonNull(owner, "owner");

        RapunzelContext toClose;
        synchronized (LOCK) {
            RapunzelContext current = context;
            if (current == null) {
                borrowerHandles.remove(owner);
                return;
            }

            if (!Objects.equals(ownerToken, owner)) {
                borrowerHandles.remove(owner);
                return;
            }

            borrowerHandles.clear();
            ownerHandle = null;
            ownerToken = null;
            toClose = current;
            context = null;
        }

        closeContext(toClose);
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
            borrowerHandles.clear();
            ownerHandle = null;
            ownerToken = null;
            toClose = context;
            context = null;
        }
        closeContext(toClose);
    }

    public static int ownerCount() {
        synchronized (LOCK) {
            return ownerToken == null ? 0 : 1;
        }
    }

    public static int borrowerCount() {
        synchronized (LOCK) {
            return borrowerHandles.size();
        }
    }

    public static @NotNull BootstrapState bootstrapState() {
        synchronized (LOCK) {
            return new BootstrapState(context, ownerToken, borrowerHandles.size(), registeredPlatformHost);
        }
    }

    public static void registerPlatformBootstrapHost(@NotNull PlatformBootstrapHost host) {
        Objects.requireNonNull(host, "host");

        synchronized (LOCK) {
            registeredPlatformHost = host;
        }
    }

    public static void clearRegisteredPlatformBootstrapHost() {
        synchronized (LOCK) {
            registeredPlatformHost = null;
        }
    }

    public static @NotNull Optional<PlatformBootstrapHost> registeredPlatformBootstrapHost() {
        return Optional.ofNullable(registeredPlatformHost);
    }

    private static BootstrapHandle tryBootstrapRegisteredHost(
        Object requester,
        Supplier<? extends RapunzelContext> contextFactory
    ) {
        PlatformBootstrapHost host = registeredPlatformHost;
        if (host == null) {
            return null;
        }

        Object canonicalOwner = Objects.requireNonNull(host.ownerToken(), "host.ownerToken()");
        Optional<? extends RapunzelContext> claimed = Objects.requireNonNull(
            host.tryCreateContext(requester, contextFactory),
            "host.tryCreateContext(...)"
        );
        if (claimed.isEmpty()) {
            return null;
        }

        RapunzelContext claimedContext = Objects.requireNonNull(claimed.get(), "host.tryCreateContext(...).get()");
        return installOwnedContext(canonicalOwner, requester, claimedContext);
    }

    private static BootstrapHandle installOwnedContext(Object owner, Object requester, RapunzelContext newContext) {
        context = newContext;
        ownerToken = owner;
        ownerHandle = new BootstrapHandle(owner, newContext, BootstrapOwnerRole.OWNER, () -> Rapunzel.shutdown(owner));
        if (Objects.equals(owner, requester)) {
            return ownerHandle;
        }
        return borrowerHandle(requester, newContext);
    }

    private static BootstrapHandle handleFor(Object requester, RapunzelContext current) {
        if (Objects.equals(ownerToken, requester)) {
            return ownerHandle;
        }
        return borrowerHandle(requester, current);
    }

    private static BootstrapHandle borrowerHandle(Object borrower, RapunzelContext current) {
        BootstrapHandle existing = borrowerHandles.get(borrower);
        if (existing != null) {
            return existing;
        }

        BootstrapHandle created = new BootstrapHandle(
            borrower,
            current,
            BootstrapOwnerRole.BORROWER,
            () -> Rapunzel.shutdown(borrower)
        );
        borrowerHandles.put(borrower, created);
        return created;
    }

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
