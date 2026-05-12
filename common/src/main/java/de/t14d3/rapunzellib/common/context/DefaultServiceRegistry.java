package de.t14d3.rapunzellib.common.context;

import de.t14d3.rapunzellib.context.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ServiceRegistry}.
 * <p>
 * Thread-safe service registry supporting direct registration, linked (alias)
 * registrations, and lazy creation via suppliers. Uses a monitor lock on an
 * internal object for all mutation operations.
 * <p>
 * Alias chains are followed transitively during lookup and cycle-detected
 * to prevent infinite loops.
 */
public final class DefaultServiceRegistry implements ServiceRegistry {
    /** Synchronization lock */
    private final Object lock = new Object();
    /** Primary service registration map */
    private final LinkedHashMap<Class<?>, Object> services = new LinkedHashMap<>();
    /** Alias map: alias type -> primary type */
    private final LinkedHashMap<Class<?>, Class<?>> aliases = new LinkedHashMap<>();

    /**
     * Registers a service instance under its exact type.
     *
     * @param type     the service type class
     * @param instance the service instance
     * @param <T>      the service type
     * @throws IllegalStateException if the type is already registered as an alias
     */
    @Override
    public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        type.cast(instance);

        synchronized (lock) {
            ensureNotAliasedType(type);
            services.put(type, instance);
        }
    }

    /**
     * Registers a service under a primary type and creates aliases to it.
     *
     * @param primaryType the primary registration type
     * @param instance    the service instance
     * @param linkedTypes additional types to alias to the primary
     * @param <T>         the service type
     * @return the registered instance
     */
    @Override
    public <T> @NotNull T registerLinked(
        @NotNull Class<T> primaryType,
        @NotNull T instance,
        @NotNull Class<?>... linkedTypes
    ) {
        Objects.requireNonNull(primaryType, "primaryType");
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(linkedTypes, "linkedTypes");
        primaryType.cast(instance);

        synchronized (lock) {
            ensureNotAliasedType(primaryType);
            validateLinkedTypes(primaryType, linkedTypes);

            services.put(primaryType, instance);
            for (Class<?> linkedType : linkedTypes) {
                if (!Objects.equals(aliases.get(linkedType), primaryType)) {
                    aliases.put(linkedType, primaryType);
                }
            }
            return instance;
        }
    }

    /**
     * Creates an alias from one type to another.
     *
     * @param aliasType  the alias type
     * @param targetType the target type that implements the alias
     * @param <T>        the service type
     */
    @Override
    public <T> void registerAlias(@NotNull Class<T> aliasType, @NotNull Class<? extends T> targetType) {
        Objects.requireNonNull(aliasType, "aliasType");
        Objects.requireNonNull(targetType, "targetType");

        synchronized (lock) {
            validateAliasRegistration(aliasType, targetType);
            if (!Objects.equals(aliases.get(aliasType), targetType)) {
                aliases.put(aliasType, targetType);
            }
        }
    }

    /**
     * Registers a service instance if no registration exists for the type
     * (following aliases). Returns the existing instance if already registered.
     *
     * @param type     the service type
     * @param instance the service instance
     * @param <T>      the service type
     * @return the existing or newly registered instance
     */
    @Override
    public <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");

        synchronized (lock) {
            Class<?> resolvedType = resolveType(type);
            Object existing = services.get(resolvedType);
            if (existing != null) {
                return type.cast(existing);
            }
            if (!resolvedType.isInstance(instance)) {
                throw new IllegalArgumentException(
                    "Cannot register " + instance.getClass().getName()
                        + " for alias " + type.getName()
                        + " because it resolves to " + resolvedType.getName()
                );
            }
            services.put(resolvedType, instance);
            return type.cast(instance);
        }
    }

    /**
     * Registers a service via supplier if no registration exists for the type.
     *
     * @param type     the service type
     * @param supplier the factory for creating the instance
     * @param <T>      the service type
     * @return the existing or newly created instance
     */
    @Override
    public <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");

        synchronized (lock) {
            Class<?> resolvedType = resolveType(type);
            Object existing = services.get(resolvedType);
            if (existing != null) {
                return type.cast(existing);
            }
            T created = Objects.requireNonNull(supplier.get(), "supplier returned null for " + type.getName());
            if (!resolvedType.isInstance(created)) {
                throw new IllegalArgumentException(
                    "Cannot register " + created.getClass().getName()
                        + " for alias " + type.getName()
                        + " because it resolves to " + resolvedType.getName()
                );
            }
            services.put(resolvedType, created);
            return type.cast(created);
        }
    }

    /**
     * Gets an existing service or creates and registers it via the supplier.
     *
     * @param type     the service type
     * @param supplier the factory for creating the instance
     * @param <T>      the service type
     * @return the existing or newly created instance
     */
    @Override
    public <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return registerIfAbsent(type, supplier);
    }

    /**
     * Finds a registered service by type.
     *
     * @param type the service type
     * @param <T>  the service type
     * @return an optional containing the service, or empty if not found
     */
    @Override
    public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");

        synchronized (lock) {
            Object instance = services.get(resolveType(type));
            if (instance == null) {
                return Optional.empty();
            }
            return Optional.of(type.cast(instance));
        }
    }

    /**
     * Returns all registered service types including aliases.
     *
     * @return an immutable list of service types
     */
    @Override
    public @NotNull List<Class<?>> serviceTypes() {
        synchronized (lock) {
            LinkedHashSet<Class<?>> types = new LinkedHashSet<>(services.keySet());
            types.addAll(aliases.keySet());
            return List.copyOf(types);
        }
    }

    /**
     * Returns all unique service instances.
     *
     * @return an immutable list of service instances
     */
    @Override
    public @NotNull List<Object> services() {
        synchronized (lock) {
            ArrayList<Object> snapshot = new ArrayList<>();
            IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
            for (Object service : services.values()) {
                if (seen.put(service, Boolean.TRUE) == null) {
                    snapshot.add(service);
                }
            }
            return List.copyOf(snapshot);
        }
    }

    private void ensureNotAliasedType(Class<?> type) {
        Class<?> target = aliases.get(type);
        if (target != null) {
            throw new IllegalStateException(
                "Cannot register " + type.getName() + " directly because it aliases " + target.getName()
            );
        }
    }

    private void validateLinkedTypes(Class<?> primaryType, Class<?>[] linkedTypes) {
        LinkedHashSet<Class<?>> seen = new LinkedHashSet<>();
        for (Class<?> linkedType : linkedTypes) {
            Objects.requireNonNull(linkedType, "linkedType");
            if (!seen.add(linkedType)) {
                throw new IllegalStateException(
                    "Linked type " + linkedType.getName() + " is registered more than once for " + primaryType.getName()
                );
            }
            validateAliasRegistration(linkedType, primaryType);
        }
    }

    private void validateAliasRegistration(Class<?> aliasType, Class<?> targetType) {
        if (aliasType == targetType) {
            throw new IllegalStateException("Cannot alias " + aliasType.getName() + " to itself");
        }
        if (!aliasType.isAssignableFrom(targetType)) {
            throw new IllegalArgumentException(
                "Cannot alias " + aliasType.getName() + " to incompatible target " + targetType.getName()
            );
        }
        if (services.containsKey(aliasType)) {
            throw new IllegalStateException(
                "Cannot alias " + aliasType.getName() + " because a direct service is already registered"
            );
        }
        Class<?> existingTarget = aliases.get(aliasType);
        if (existingTarget != null && existingTarget != targetType) {
            throw new IllegalStateException(
                "Cannot alias " + aliasType.getName() + " to " + targetType.getName()
                    + " because it already aliases " + existingTarget.getName()
            );
        }
        assertNoAliasCycle(aliasType, targetType);
    }

    private void assertNoAliasCycle(Class<?> aliasType, Class<?> targetType) {
        LinkedHashSet<Class<?>> path = new LinkedHashSet<>();
        path.add(aliasType);

        Class<?> current = targetType;
        while (current != null) {
            if (!path.add(current)) {
                throw new IllegalStateException("Alias cycle detected: " + describePath(path, current));
            }
            current = aliases.get(current);
        }
    }

    private Class<?> resolveType(Class<?> type) {
        LinkedHashSet<Class<?>> path = new LinkedHashSet<>();
        Class<?> current = type;

        while (true) {
            if (!path.add(current)) {
                throw new IllegalStateException("Alias cycle detected: " + describePath(path, current));
            }
            Class<?> target = aliases.get(current);
            if (target == null) {
                return current;
            }
            current = target;
        }
    }

    private static String describePath(LinkedHashSet<Class<?>> path, Class<?> repeatedType) {
        StringBuilder description = new StringBuilder();
        boolean first = true;
        for (Class<?> type : path) {
            if (!first) {
                description.append(" -> ");
            }
            description.append(type.getName());
            first = false;
        }
        description.append(" -> ").append(repeatedType.getName());
        return description.toString();
    }
}
