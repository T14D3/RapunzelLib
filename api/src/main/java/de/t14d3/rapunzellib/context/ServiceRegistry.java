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
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     */
    <T> void register(@NotNull Class<T> type, @NotNull T instance);

    /**
     * Registers a service instance linked to multiple type aliases.
     *
     * @param primaryType the primary service type
     * @param instance    the service instance
     * @param linkedTypes additional type aliases to register
     * @param <T>         the primary service type
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
     * @param aliasType  the alias type
     * @param targetType the existing registered type
     * @param <T>        the alias type
     * @throws IllegalArgumentException if the types are incompatible
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
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     * @return the registered or existing instance
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
     * @param type     the service type
     * @param supplier the supplier to create the instance
     * @param <T>      the service type
     * @return the registered or existing instance
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

    /**
     * Gets an existing service or creates and registers one from the supplier.
     *
     * @param type     the service type
     * @param supplier the supplier to create the instance
     * @param <T>      the service type
     * @return the existing or newly created instance
     */
    default <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return registerIfAbsent(type, supplier);
    }

    /**
     * Finds a registered service by type.
     *
     * @param type the service type
     * @param <T>  the service type
     * @return an {@link Optional} containing the instance, or empty if not registered
     */
    <T> @NotNull Optional<T> find(@NotNull Class<T> type);

    /**
     * Requires a registered service, throwing if not found.
     *
     * @param type the service type
     * @param <T>  the service type
     * @return the service instance
     * @throws IllegalStateException if the service is not registered
     */
    default <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Service not registered: " + type.getName()));
    }

    /**
     * Returns all registered service types.
     *
     * @return a list of service types
     */
    @NotNull List<Class<?>> serviceTypes();

    /**
     * Returns all registered service instances.
     *
     * @return a list of service instances
     */
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
