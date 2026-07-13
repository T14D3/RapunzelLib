package de.t14d3.rapunzellib.context;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A registry for typed service instances.
 *
 * <p>Services are registered by class type and can be looked up, aliased, and
 * linked. This is the backbone of RapunzelLib's service-oriented architecture.</p>
 */
public interface ServiceRegistry {
    /**
     * Registers a service instance for the given type.
     *
     * <p>If a service is already registered for the type, the implementation
     * should throw or replace it depending on the implementation contract.</p>
     *
     * @param type     the service class (used as lookup key)
     * @param instance the service instance
     * @param <T>      the service type
     */
    <T> void register(@NotNull Class<T> type, @NotNull T instance);

    /**
     * Registers a service instance linked to multiple type aliases.
     *
     * <p>The primary registration uses {@code primaryType}. Additional lookups
     * via the linked types will resolve to the same instance.</p>
     *
     * @param primaryType the primary service type
     * @param instance    the service instance
     * @param linkedTypes additional type aliases pointing to this service
     * @param <T>         the service type
     * @return the registered instance
     */
    default <T> @NotNull T registerLinked(
        @NotNull Class<T> primaryType,
        @NotNull T instance,
        @NotNull Class<?>... linkedTypes
    ) {
        Objects.requireNonNull(primaryType, "primaryType");
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(linkedTypes, "linkedTypes");

        register(primaryType, instance);
        for (Class<?> linkedType : linkedTypes) {
            registerLinkedAlias(primaryType, linkedType);
        }
        return instance;
    }

    /**
     * Registers an alias mapping one type to another already-registered service.
     *
     * <p>After registration, looking up {@code aliasType} returns the same instance
     * registered under {@code targetType}. The target type must already be registered.</p>
     *
     * @param aliasType  the alias service type (the lookup key)
     * @param targetType the already-registered type to map the alias to
     * @param <T>        the common service type
     */
    default <T> void registerAlias(@NotNull Class<T> aliasType, @NotNull Class<? extends T> targetType) {
        Objects.requireNonNull(aliasType, "aliasType");
        Objects.requireNonNull(targetType, "targetType");
        if (!aliasType.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                "Cannot alias " + aliasType.getName() + " to incompatible target " + targetType.getName()
            );
        }
        register(aliasType, get(targetType));
    }

    /**
     * Registers a service if no instance is already registered for the given type.
     *
     * <p>If a service is already registered, returns the existing instance and
     * ignores the new one.</p>
     *
     * @param type     the service type
     * @param instance the service instance to register if absent
     * @param <T>      the service type
     * @return the newly registered instance, or the existing one if already registered
     */
    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        return find(type).orElseGet(() -> {
            register(type, instance);
            return instance;
        });
    }

    /**
     * Registers a service from a supplier if no instance is already registered.
     *
     * <p>If a service is already registered, returns the existing instance without
     * invoking the supplier.</p>
     *
     * @param type     the service type
     * @param supplier the supplier to create the instance if absent
     * @param <T>      the service type
     * @return the existing instance, or the newly created and registered one
     */
    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");
        return find(type).orElseGet(() -> {
            T instance = Objects.requireNonNull(supplier.get(), "supplier returned null for " + type.getName());
            register(type, instance);
            return instance;
        });
    }

    /** Gets an existing service or creates and registers one from the supplier. */
    default <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return registerIfAbsent(type, supplier);
    }

    /**
     * Finds a registered service by type.
     *
     * @param type the service class to look up
     * @param <T>  the service type
     * @return an {@link Optional} containing the service, or empty if not registered
     */
    <T> @NotNull Optional<T> find(@NotNull Class<T> type);

    /**
     * Requires a registered service, throwing if not found.
     *
     * @param type the service class to look up
     * @param <T>  the service type
     * @return the registered service instance
     * @throws IllegalStateException if no service is registered for the given type
     */
    default <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Service not registered: " + type.getName()));
    }

    /** Returns all registered service types. */
    @NotNull List<Class<?>> serviceTypes();

    /** Returns all registered service instances. */
    @NotNull List<Object> services();

    @SuppressWarnings("unchecked")
    private <T> void registerLinkedAlias(@NotNull Class<T> primaryType, @NotNull Class<?> linkedType) {
        Objects.requireNonNull(linkedType, "linkedType");
        if (!linkedType.isAssignableFrom(primaryType)) {
            throw new IllegalArgumentException(
                "Cannot link " + linkedType.getName() + " to incompatible target " + primaryType.getName()
            );
        }
        registerAlias((Class<T>) linkedType, primaryType);
    }
}
