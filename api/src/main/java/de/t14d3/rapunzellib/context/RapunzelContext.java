package de.t14d3.rapunzellib.context;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.attachments.AttachmentFeatures;
import de.t14d3.rapunzellib.attachments.AttachmentSupport;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
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
    /**
     * Returns the shared {@link de.t14d3.rapunzellib.runtime.RapunzelRuntime}.
     *
     * @return the shared runtime
     */
    default @NotNull RapunzelRuntime sharedRuntime() {
        return RapunzelRuntime.getInstance();
    }

    /**
     * Returns the platform runtime for this context.
     *
     * @return the platform runtime
     */
    @NotNull PlatformRuntime runtime();

    /**
     * Returns the platform identifier for this context.
     *
     * @return the platform ID
     */
    default @NotNull PlatformId platformId() {
        return runtime().platformId();
    }

    /**
     * Returns the logger for this context.
     *
     * @return the logger
     */
    @NotNull Logger logger();

    /**
     * Returns the data directory path for this context.
     *
     * @return the data directory path
     */
    @NotNull Path dataDirectory();

    /**
     * Returns the resource provider for this context.
     *
     * @return the resource provider
     */
    @NotNull ResourceProvider resources();

    /**
     * Returns the scheduler for this context.
     *
     * @return the scheduler
     */
    @NotNull Scheduler scheduler();

    /**
     * Returns the service registry for this context.
     *
     * @return the service registry
     */
    @NotNull ServiceRegistry services();

    /**
     * Returns the registry access for this context.
     *
     * @return the registry access
     */
    default @NotNull RRegistryAccess registries() {
        return services().get(RRegistryAccess.class);
    }

    /**
     * Checks whether the platform supports the given capability.
     *
     * @param capability the capability to check
     * @return true if supported
     */
    default boolean supports(@NotNull RuntimeCapability capability) {
        return runtime().hasCapability(capability);
    }

    /**
     * Requires that the platform supports the given capability, throwing if not.
     *
     * @param capability the required capability
     */
    default void requireCapability(@NotNull RuntimeCapability capability) {
        runtime().requireCapability(capability);
    }

    /**
     * Requires that the platform supports the given capability, throwing if not, with a use case description.
     *
     * @param capability the required capability
     * @param useCase    the use case description for error messages
     */
    default void requireCapability(@NotNull RuntimeCapability capability, @NotNull String useCase) {
        runtime().requireCapability(capability, useCase);
    }

    /**
     * Returns the lifecycle owner for this context.
     *
     * @return the lifecycle owner
     */
    default @NotNull LifecycleOwner lifecycleOwner() {
        return owner();
    }

    /**
     * Returns the lifecycle owner for this context.
     *
     * @return the lifecycle owner
     */
    default @NotNull LifecycleOwner owner() {
        return runtime().owner();
    }

    /**
     * Returns the lifecycle owner cast to the requested type, if applicable.
     *
     * @param type the target type class
     * @param <T>  the target type
     * @return an {@link Optional} containing the owner, or empty if not of that type
     */
    default <T> @NotNull Optional<T> lifecycleOwner(@NotNull Class<T> type) {
        return owner(type);
    }

    /**
     * Returns the owner cast to the requested type, if applicable.
     *
     * @param type the target type class
     * @param <T>  the target type
     * @return an {@link Optional} containing the owner, or empty if not of that type
     */
    default <T> @NotNull Optional<T> owner(@NotNull Class<T> type) {
        return owner().as(type);
    }

    /**
     * Requires the lifecycle owner cast to the requested type, throwing if not applicable.
     *
     * @param type the target type class
     * @param <T>  the target type
     * @return the owner
     */
    default <T> @NotNull T requireLifecycleOwner(@NotNull Class<T> type) {
        return requireOwner(type);
    }

    /**
     * Requires the owner cast to the requested type, throwing if not applicable.
     *
     * @param type the target type class
     * @param <T>  the target type
     * @return the owner
     */
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
     * @param <T>         the primary service type
     * @return the registered instance
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
     * @param aliasType  the alias type
     * @param targetType the target type to resolve
     * @param <T>        the alias type
     */
    default <T> void registerAlias(@NotNull Class<T> aliasType, @NotNull Class<? extends T> targetType) {
        services().registerAlias(aliasType, targetType);
    }

    /**
     * Registers a service if no instance is already registered for the given type.
     *
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     * @return the registered or existing instance
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
     * @param supplier the supplier to create the instance
     * @param <T>      the service type
     * @return the registered or existing instance
     */
    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return getOrCreate(type, supplier);
    }

    /**
     * Gets an existing service or creates and registers one from the supplier.
     *
     * @param type     the service type
     * @param supplier the supplier to create the instance
     * @param <T>      the service type
     * @return the existing or newly registered instance
     */
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

    /**
     * Returns the config service for this context.
     *
     * @return the config service
     */
    default @NotNull ConfigService configs() {
        return services().get(ConfigService.class);
    }

    /**
     * Returns the message format service for this context.
     *
     * @return the message format service
     */
    default @NotNull MessageFormatService messages() {
        return services().get(MessageFormatService.class);
    }

    /**
     * Returns the players access for this context.
     *
     * @return the players access
     */
    default @NotNull Players players() {
        return services().get(Players.class);
    }

    /**
     * Returns the entities access for this context.
     *
     * @return the entities access
     */
    default @NotNull Entities entities() {
        requireCapability(RuntimeCapability.ENTITIES, "entity access");
        return services().get(Entities.class);
    }

    /**
     * Returns the entity type registry for this context.
     *
     * @return the entity type registry
     */
    default @NotNull REntityTypeRegistry entityTypes() {
        requireCapability(RuntimeCapability.ENTITIES, "entity type registry access");
        return services().find(REntityTypeRegistry.class).orElseGet(() -> REntityTypeRegistry.of(registries()));
    }

    /**
     * Returns the item type registry for this context.
     *
     * @return the item type registry
     */
    default @NotNull RItemTypeRegistry itemTypes() {
        requireCapability(RuntimeCapability.INVENTORY, "item type registry access");
        return services().find(RItemTypeRegistry.class).orElseGet(() -> RItemTypeRegistry.of(registries()));
    }

    /**
     * Returns the block type registry for this context.
     *
     * @return the block type registry
     */
    default @NotNull RBlockTypeRegistry blockTypes() {
        requireCapability(RuntimeCapability.BLOCKS, "block type registry access");
        return services().find(RBlockTypeRegistry.class).orElseGet(() -> RBlockTypeRegistry.of(registries()));
    }

    /**
     * Returns the worlds access for this context.
     *
     * @return the worlds access
     */
    default @NotNull Worlds worlds() {
        requireCapability(RuntimeCapability.WORLDS, "world access");
        return services().get(Worlds.class);
    }

    /**
     * Returns the blocks access for this context.
     *
     * @return the blocks access
     */
    default @NotNull Blocks blocks() {
        requireCapability(RuntimeCapability.BLOCKS, "block access");
        return services().get(Blocks.class);
    }

    /**
     * Returns the attachment support for this context.
     *
     * @return the attachment support
     */
    default @NotNull AttachmentSupport attachments() {
        requireCapability(RuntimeCapability.ATTACHMENTS, "attachment access");
        return services().find(AttachmentSupport.class).orElseGet(AttachmentFeatures::install);
    }

    /**
     * Checks whether attachments are supported for the given target.
     *
     * @param target the native object
     * @return true if attachments are supported
     */
    default boolean supportsAttachments(@NotNull RNative target) {
        return attachments().supports(target);
    }

    /**
     * Requires that the given target supports attachments, throwing if not.
     *
     * @param target the native object
     * @param <T>    the native type
     * @return the same target
     */
    default <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return attachments().requireSupported(target);
    }

    /**
     * Returns the attachment container for the given native target.
     *
     * @param target the native object
     * @return the attachment container
     */
    default @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return attachments().attachments(target);
    }

    /**
     * Returns the native interop service, if available.
     *
     * @return an {@link Optional} containing the native interop, or empty if not supported
     */
    default @NotNull Optional<RNativeInterop> nativeInterop() {
        return services().find(RNativeInterop.class);
    }

    @Override
    default void close() throws Exception {
        // no-op
    }
}
