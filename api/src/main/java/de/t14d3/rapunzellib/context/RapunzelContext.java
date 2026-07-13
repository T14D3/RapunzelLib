package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentFeatures;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.commands.ConsoleCommandDispatcher;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.objects.Entities;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.objects.block.Blocks;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.registry.RBlockTypeRegistry;
import de.t14d3.rapunzellib.registry.REntityTypeRegistry;
import de.t14d3.rapunzellib.registry.RItemTypeRegistry;
import de.t14d3.rapunzellib.registry.RRegistryAccess;
import de.t14d3.rapunzellib.objects.interop.RNativeInterop;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RapunzelRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A RapunzelLib context providing access to all platform services and subsystems.
 *
 * <p>Each context is associated with a single plugin or module owner and provides
 * tailored access to registries, configuration, messaging, scheduling, entities,
 * players, worlds, attachments, and more. Contexts are created via
 * {@link de.t14d3.rapunzellib.Rapunzel#bootstrap} and closed on shutdown.</p>
 */
public interface RapunzelContext extends AutoCloseable {
    /** Returns the shared {@link de.t14d3.rapunzellib.runtime.RapunzelRuntime}. */
    default @NotNull RapunzelRuntime sharedRuntime() {
        return RapunzelRuntime.getInstance();
    }

    /** Returns the platform runtime for this context. */
    @NotNull PlatformRuntime runtime();

    /** Returns the platform identifier for this context. */
    default @NotNull PlatformId platformId() {
        return runtime().platformId();
    }

    /** Returns the logger for this context. */
    @NotNull Logger logger();

    /** Returns the data directory path for this context. */
    @NotNull Path dataDirectory();

    /** Returns the resource provider for this context. */
    @NotNull ResourceProvider resources();

    /** Returns the scheduler for this context. */
    @NotNull Scheduler scheduler();

    /** Returns the service registry for this context. */
    @NotNull ServiceRegistry services();

    /** Returns the registry access for this context. */
    default @NotNull RRegistryAccess registries() {
        return services().get(RRegistryAccess.class);
    }

    /**
     * Checks whether the platform supports the given capability.
     *
     * @param capability the runtime capability to check
     * @return true if the capability is supported, false otherwise
     */
    default boolean supports(@NotNull RuntimeCapability capability) {
        return runtime().hasCapability(capability);
    }

    /**
     * Requires that the platform supports the given capability, throwing if not.
     *
     * @param capability the required runtime capability
     * @throws IllegalStateException if the capability is not supported
     */
    default void requireCapability(@NotNull RuntimeCapability capability) {
        runtime().requireCapability(capability);
    }

    /**
     * Requires that the platform supports the given capability, with a use case description.
     *
     * <p>The use case description is included in the error message for better diagnostics.</p>
     *
     * @param capability the required runtime capability
     * @param useCase    a human-readable description of what the capability is needed for
     * @throws IllegalStateException if the capability is not supported
     */
    default void requireCapability(@NotNull RuntimeCapability capability, @NotNull String useCase) {
        runtime().requireCapability(capability, useCase);
    }

    /** Returns the lifecycle owner for this context. */
    default @NotNull LifecycleOwner lifecycleOwner() {
        return owner();
    }

    /** Returns the lifecycle owner for this context. */
    default @NotNull LifecycleOwner owner() {
        return runtime().owner();
    }

    /** Returns the lifecycle owner cast to the requested type, if applicable. */
    default <T> @NotNull Optional<T> lifecycleOwner(@NotNull Class<T> type) {
        return owner(type);
    }

    /** Returns the owner cast to the requested type, if applicable. */
    default <T> @NotNull Optional<T> owner(@NotNull Class<T> type) {
        return owner().as(type);
    }

    /** Requires the lifecycle owner cast to the requested type, throwing if not applicable. */
    default <T> @NotNull T requireLifecycleOwner(@NotNull Class<T> type) {
        return requireOwner(type);
    }

    /** Requires the owner cast to the requested type, throwing if not applicable. */
    default <T> @NotNull T requireOwner(@NotNull Class<T> type) {
        return owner().require(type);
    }

    /**
     * Registers a service into this context.
     *
     * <p>Default implementation registers into {@link #services()} only. Context
     * implementations may override to provide additional lifecycle tracking (e.g. auto-closing).</p>
     */
    default <T> @NotNull T register(@NotNull Class<T> type, @NotNull T instance) {
        services().register(type, instance);
        if (instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return instance;
    }

    /**
     * Registers a service instance linked to multiple type aliases.
     *
     * @param primaryType the primary service type
     * @param instance    the service instance
     * @param linkedTypes additional type aliases
     */
    default <T> @NotNull T registerLinked(
        @NotNull Class<T> primaryType,
        @NotNull T instance,
        @NotNull Class<?>... linkedTypes
    ) {
        services().registerLinked(primaryType, instance, linkedTypes);
        if (instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return instance;
    }

    /**
     * Registers an alias mapping one service type to another.
     *
     * <p>After registration, looking up {@code aliasType} returns the same instance
     * registered under {@code targetType}. The target type must already be registered.</p>
     *
     * @param aliasType  the alias service type (the lookup key)
     * @param targetType the already-registered type to map the alias to
     * @param <T>        the common service type
     */
    default <T> void registerAlias(@NotNull Class<T> aliasType, @NotNull Class<? extends T> targetType) {
        services().registerAlias(aliasType, targetType);
    }

    /**
     * Registers a service if no instance is already registered for the given type.
     *
     * <p>If a service is already registered, returns the existing instance and
     * ignores the new one. If the supplied instance is {@link AutoCloseable} and
     * it gets registered, it is tracked for automatic cleanup on context close.</p>
     *
     * @param type     the service type
     * @param instance the service instance to register if absent
     * @param <T>      the service type
     * @return the newly registered instance, or the existing one if already registered
     */
    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        T registered = services().registerIfAbsent(type, instance);
        if (registered == instance && instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return registered;
    }

    /**
     * Registers a service from a supplier if no instance is already registered.
     *
     * @param type     the service type
     * @param supplier the supplier to create the instance if absent
     * @param <T>      the service type
     * @return the existing instance, or the newly created and registered one
     */
    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return getOrCreate(type, supplier);
    }

    /** Gets an existing service or creates and registers one from the supplier. */
    default <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        AtomicReference<T> created = new AtomicReference<>();
        T registered = services().getOrCreate(type, () -> {
            T instance = supplier.get();
            created.set(instance);
            return instance;
        });
        T instance = created.get();
        if (instance != null && instance == registered && instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return registered;
    }

    /**
     * Registers a closeable to be closed when the context shuts down.
     *
     * <p>Default implementation is a no-op. Implementations with lifecycle tracking
     * should override.</p>
     */
    default void registerCloseable(@NotNull AutoCloseable closeable) {
        // no-op by default
    }

    /** Returns the config service for this context. */
    default @NotNull ConfigService configs() {
        return services().get(ConfigService.class);
    }

    /** Returns the message format service for this context. */
    default @NotNull MessageFormatService messages() {
        return services().get(MessageFormatService.class);
    }

    /** Returns the players access for this context. */
    default @NotNull Players players() {
        return services().get(Players.class);
    }

    /**
     * Returns the entities access for this context.
     *
     * <p>Requires the {@link RuntimeCapability#ENTITIES} capability.</p>
     *
     * @return the typed entities access for looking up and querying entities
     */
    default @NotNull Entities entities() {
        requireCapability(RuntimeCapability.ENTITIES, "entity access");
        return services().get(Entities.class);
    }

    /**
     * Returns the entity type registry for this context.
     *
     * <p>Requires the {@link RuntimeCapability#ENTITIES} capability.
     * The registry is shared globally across all contexts - all
     * Minecraft type data is identical regardless of the caller.</p>
     *
     * @return the entity type registry for looking up entity types by key
     */
    default @NotNull REntityTypeRegistry entityTypes() {
        requireCapability(RuntimeCapability.ENTITIES, "entity type registry access");
        return sharedRuntime().get(REntityTypeRegistry.class);
    }

    /**
     * Returns the item type registry for this context.
     *
     * <p>Requires the {@link RuntimeCapability#INVENTORY} capability.
     * The registry is shared globally across all contexts.</p>
     *
     * @return the item type registry for looking up item types by key
     */
    default @NotNull RItemTypeRegistry itemTypes() {
        requireCapability(RuntimeCapability.INVENTORY, "item type registry access");
        return sharedRuntime().get(RItemTypeRegistry.class);
    }

    /**
     * Returns the block type registry for this context.
     *
     * <p>Requires the {@link RuntimeCapability#BLOCKS} capability.
     * The registry is shared globally across all contexts.</p>
     *
     * @return the block type registry for looking up block types by key
     */
    default @NotNull RBlockTypeRegistry blockTypes() {
        requireCapability(RuntimeCapability.BLOCKS, "block type registry access");
        return sharedRuntime().get(RBlockTypeRegistry.class);
    }

    /**
     * Returns the worlds access for this context.
     *
     * <p>Requires the {@link RuntimeCapability#WORLDS} capability.</p>
     *
     * @return the typed worlds access for looking up and querying worlds
     */
    default @NotNull Worlds worlds() {
        requireCapability(RuntimeCapability.WORLDS, "world access");
        return services().get(Worlds.class);
    }

    /**
     * Returns the blocks access for this context.
     *
     * <p>Requires the {@link RuntimeCapability#BLOCKS} capability.</p>
     *
     * @return the typed blocks access for looking up and querying blocks
     */
    default @NotNull Blocks blocks() {
        requireCapability(RuntimeCapability.BLOCKS, "block access");
        return services().get(Blocks.class);
    }

    /**
     * Returns the attachment support for this context.
     *
     * <p>Requires the {@link RuntimeCapability#ATTACHMENTS} capability.
     * Falls back to auto-installing attachment features if no dedicated
     * service is registered.</p>
     *
     * @return the attachment support instance for this platform
     */
    default @NotNull AttachmentSupport attachments() {
        requireCapability(RuntimeCapability.ATTACHMENTS, "attachment access");
        return services().find(AttachmentSupport.class).orElseGet(AttachmentFeatures::install);
    }

    /** Checks whether attachments are supported for the given target. */
    default boolean supportsAttachments(@NotNull RNative target) {
        return attachments().supports(target);
    }

    /** Requires that the given target supports attachments, throwing if not. */
    default <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return attachments().requireSupported(target);
    }

    /**
     * Returns the attachment container for the given native target.
     *
     * <p>The target must be supported by this platform's attachment system
     * (see {@link #supportsAttachments(RNative)}). Transient and persistent
     * attachments may be available depending on the container implementation.</p>
     *
     * @param target the native object (player, entity, world, block, etc.)
     * @return the attachment container for the target
     */
    default @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return attachments().attachments(target);
    }

    /** Returns the native interop service, if available. */
    default @NotNull Optional<RNativeInterop> nativeInterop() {
        return services().find(RNativeInterop.class);
    }

    /**
     * Dispatches a command via the registered {@link ConsoleCommandDispatcher}.
     * <p>
     * This is a convenience method equivalent to looking up the
     * {@code ConsoleCommandDispatcher} service and calling {@code dispatch} on it.
     * </p>
     *
     * @param command the command to execute (e.g. "/gamemode creative Tester")
     * @throws IllegalStateException if no {@code ConsoleCommandDispatcher} is registered
     */
    default void dispatchCommand(@NotNull String command) {
        scheduler().run(() -> services().get(ConsoleCommandDispatcher.class).dispatch(command));
    }

    /**
     * Closes this context and releases all associated resources.
     *
     * <p>Implementations should close all registered closeables in reverse
     * registration order, collecting exceptions and suppressing subsequent
     * errors under the first exception.</p>
     *
     * @throws Exception if an error occurs during shutdown; subsequent errors are suppressed
     */
    @Override
    default void close() throws Exception {
        // no-op
    }
}
