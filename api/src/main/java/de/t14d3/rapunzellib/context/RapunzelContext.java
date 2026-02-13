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
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public interface RapunzelContext extends AutoCloseable {
    @NotNull PlatformRuntime runtime();

    default @NotNull PlatformId platformId() {
        return runtime().platformId();
    }

    @NotNull Logger logger();

    @NotNull Path dataDirectory();

    @NotNull ResourceProvider resources();

    @NotNull Scheduler scheduler();

    @NotNull ServiceRegistry services();

    default @NotNull RRegistryAccess registries() {
        return services().get(RRegistryAccess.class);
    }

    default boolean supports(@NotNull RuntimeCapability capability) {
        return runtime().hasCapability(capability);
    }

    default void requireCapability(@NotNull RuntimeCapability capability) {
        runtime().requireCapability(capability);
    }

    default void requireCapability(@NotNull RuntimeCapability capability, @NotNull String useCase) {
        runtime().requireCapability(capability, useCase);
    }

    default @NotNull LifecycleOwner lifecycleOwner() {
        return owner();
    }

    default @NotNull LifecycleOwner owner() {
        return runtime().owner();
    }

    default <T> @NotNull Optional<T> lifecycleOwner(@NotNull Class<T> type) {
        return owner(type);
    }

    default <T> @NotNull Optional<T> owner(@NotNull Class<T> type) {
        return owner().as(type);
    }

    default <T> @NotNull T requireLifecycleOwner(@NotNull Class<T> type) {
        return requireOwner(type);
    }

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

    default <T> void registerAlias(@NotNull Class<T> aliasType, @NotNull Class<? extends T> targetType) {
        services().registerAlias(aliasType, targetType);
    }

    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        T registered = services().registerIfAbsent(type, instance);
        if (registered == instance && instance instanceof AutoCloseable closeable) {
            registerCloseable(closeable);
        }
        return registered;
    }

    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return getOrCreate(type, supplier);
    }

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

    default @NotNull ConfigService configs() {
        return services().get(ConfigService.class);
    }

    default @NotNull MessageFormatService messages() {
        return services().get(MessageFormatService.class);
    }

    default @NotNull Players players() {
        return services().get(Players.class);
    }

    default @NotNull Entities entities() {
        requireCapability(RuntimeCapability.ENTITIES, "entity access");
        return services().get(Entities.class);
    }

    default @NotNull REntityTypeRegistry entityTypes() {
        requireCapability(RuntimeCapability.ENTITIES, "entity type registry access");
        return services().find(REntityTypeRegistry.class).orElseGet(() -> REntityTypeRegistry.of(registries()));
    }

    default @NotNull RItemTypeRegistry itemTypes() {
        requireCapability(RuntimeCapability.INVENTORY, "item type registry access");
        return services().find(RItemTypeRegistry.class).orElseGet(() -> RItemTypeRegistry.of(registries()));
    }

    default @NotNull RBlockTypeRegistry blockTypes() {
        requireCapability(RuntimeCapability.BLOCKS, "block type registry access");
        return services().find(RBlockTypeRegistry.class).orElseGet(() -> RBlockTypeRegistry.of(registries()));
    }

    default @NotNull Worlds worlds() {
        requireCapability(RuntimeCapability.WORLDS, "world access");
        return services().get(Worlds.class);
    }

    default @NotNull Blocks blocks() {
        requireCapability(RuntimeCapability.BLOCKS, "block access");
        return services().get(Blocks.class);
    }

    default @NotNull AttachmentSupport attachments() {
        requireCapability(RuntimeCapability.ATTACHMENTS, "attachment access");
        return services().find(AttachmentSupport.class).orElseGet(AttachmentFeatures::install);
    }

    default boolean supportsAttachments(@NotNull RNative target) {
        return attachments().supports(target);
    }

    default <T extends RNative> @NotNull T requireAttachmentSupport(@NotNull T target) {
        return attachments().requireSupported(target);
    }

    default @NotNull RAttachmentContainer attachments(@NotNull RNative target) {
        return attachments().attachments(target);
    }

    default @NotNull Optional<RNativeInterop> nativeInterop() {
        return services().find(RNativeInterop.class);
    }

    @Override
    default void close() throws Exception {
        // no-op
    }
}
