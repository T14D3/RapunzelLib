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

    public static @NotNull RapunzelRuntime sharedRuntime() {
        return RUNTIME;
    }

    public static boolean isBootstrapped() {
        synchronized (LOCK) {
            return !contexts.isEmpty();
        }
    }

    public static @NotNull RapunzelContext context() {
        return findContext().orElseThrow(() -> new IllegalStateException(ambiguousContextMessage()));
    }

    public static @NotNull RapunzelContext currentContext() {
        return context();
    }

    public static @NotNull Optional<RapunzelContext> findContext() {
        RapunzelContext scoped = CURRENT_CONTEXT.get();
        if (scoped != null) {
            return Optional.of(scoped);
        }
        synchronized (LOCK) {
            return contexts.size() == 1 ? contexts.values().stream().findFirst() : Optional.empty();
        }
    }

    public static @NotNull Optional<RapunzelContext> findCurrentContext() {
        return findContext();
    }

    public static void withContext(@NotNull RapunzelContext context, @NotNull Runnable action) {
        Objects.requireNonNull(action, "action");
        withContext(context, () -> {
            action.run();
            return null;
        });
    }

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

    public static @NotNull ServiceRegistry services() {
        return context().services();
    }

    public static <T> @NotNull T service(@NotNull Class<T> type) {
        return services().get(Objects.requireNonNull(type, "type"));
    }

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

    public static @NotNull BootstrapHandle bootstrapOrAcquire(
        @NotNull Object owner,
        @NotNull Supplier<? extends RapunzelContext> contextFactory
    ) {
        return bootstrap(owner, contextFactory);
    }

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

    public static void bootstrap(@NotNull RapunzelContext newContext) {
        bootstrap(DEFAULT_OWNER, newContext);
    }

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

    public static void shutdown() {
        shutdown(DEFAULT_OWNER);
    }

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

    public static int ownerCount() {
        synchronized (LOCK) {
            return ownerHandles.size();
        }
    }

    public static int borrowerCount() {
        synchronized (LOCK) {
            return borrowerHandles.size();
        }
    }

    public static @NotNull BootstrapState bootstrapState() {
        synchronized (LOCK) {
            RapunzelContext singleContext = contexts.size() == 1 ? contexts.values().iterator().next() : null;
            Object singleOwner = contexts.size() == 1 ? contexts.keySet().iterator().next() : null;
            return new BootstrapState(singleContext, singleOwner, borrowerHandles.size(), registeredPlatformHost);
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

    private static @NotNull String ambiguousContextMessage() {
        synchronized (LOCK) {
            if (contexts.isEmpty()) {
                return "RapunzelLib context not bootstrapped yet";
            }
            return "RapunzelLib context is ambiguous because " + contexts.size()
                + " consumer contexts are active. Use your plugin-owned RapunzelContext or Rapunzel.withContext(...).";
        }
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
