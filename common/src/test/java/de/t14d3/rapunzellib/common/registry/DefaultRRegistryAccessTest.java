package de.t14d3.rapunzellib.common.registry;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.registry.RRegistry;
import de.t14d3.rapunzellib.registry.RRegistryKey;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultRRegistryAccessTest {
    @Test
    void registersAndFindsGenericRegistriesByKeyAndRef() {
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        RRegistryKey<String> strings = RRegistryKey.of("rapunzellib:test_strings", String.class);
        Map<RKey, String> entries = new LinkedHashMap<>();
        entries.put(RKey.of("rapunzellib:alpha"), "alpha");
        entries.put(RKey.of("rapunzellib:beta"), "beta");
        MapRegistry<String> registry = new MapRegistry<>(strings, entries);

        registries.register(strings, registry);

        assertTrue(registries.containsRegistry(strings));
        assertSame(registry, registries.registry(strings));
        assertEquals(List.of(strings), registries.registryKeys());
        assertEquals(Optional.of("beta"), registries.find(strings.ref("rapunzellib:beta")));
        assertEquals(List.of(RKey.of("rapunzellib:alpha"), RKey.of("rapunzellib:beta")), registry.keys());
        assertEquals(List.of(
            RRegistryRef.of(strings, "rapunzellib:alpha"),
            RRegistryRef.of(strings, "rapunzellib:beta")
        ), registry.refs());
        assertFalse(registries.find(RRegistryKey.of("rapunzellib:missing", String.class), RKey.of("rapunzellib:alpha")).isPresent());
    }

    @Test
    void rejectsConflictingRegistryTypesForSameLocation() {
        DefaultRRegistryAccess registries = new DefaultRRegistryAccess();
        registries.register(
            RRegistryKey.of("rapunzellib:test", String.class),
            new MapRegistry<>(RRegistryKey.of("rapunzellib:test", String.class), Map.of(RKey.of("rapunzellib:value"), "value"))
        );

        assertThrows(IllegalStateException.class, () -> registries.register(
            RRegistryKey.of("rapunzellib:test", Integer.class),
            new MapRegistry<>(RRegistryKey.of("rapunzellib:test", Integer.class), Map.of(RKey.of("rapunzellib:value"), 1))
        ));
    }

    private static final class MapRegistry<T> implements RRegistry<T> {
        private final RRegistryKey<T> registryKey;
        private final Map<RKey, T> entries;

        private MapRegistry(@NotNull RRegistryKey<T> registryKey, @NotNull Map<RKey, T> entries) {
            this.registryKey = registryKey;
            this.entries = new LinkedHashMap<>(entries);
        }

        @Override
        public @NotNull RRegistryKey<T> registryKey() {
            return registryKey;
        }

        @Override
        public @NotNull Optional<T> find(@NotNull RKey key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public @NotNull List<T> entries() {
            return List.copyOf(entries.values());
        }

        @Override
        public @NotNull List<RKey> keys() {
            return List.copyOf(entries.keySet());
        }
    }
}
