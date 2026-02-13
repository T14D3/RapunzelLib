package de.t14d3.rapunzellib.context;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public interface ServiceRegistry {
    <T> void register(@NotNull Class<T> type, @NotNull T instance);

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

    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        return find(type).orElseGet(() -> {
            register(type, instance);
            return instance;
        });
    }

    default <T> @NotNull T registerIfAbsent(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(supplier, "supplier");
        return find(type).orElseGet(() -> {
            T instance = Objects.requireNonNull(supplier.get(), "supplier returned null for " + type.getName());
            register(type, instance);
            return instance;
        });
    }

    default <T> @NotNull T getOrCreate(@NotNull Class<T> type, @NotNull Supplier<? extends T> supplier) {
        return registerIfAbsent(type, supplier);
    }

    <T> @NotNull Optional<T> find(@NotNull Class<T> type);

    default <T> @NotNull T get(@NotNull Class<T> type) {
        return find(type).orElseThrow(() -> new IllegalStateException("Service not registered: " + type.getName()));
    }

    @NotNull List<Class<?>> serviceTypes();

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
