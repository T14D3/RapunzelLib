package de.t14d3.rapunzellib.livetest;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.context.RapunzelContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A registry for live test instances.
 * <p>
 * <b>New code:</b> Use context-scoped registries obtained via
 * {@code LiveTestFeatures.registry(context)} or
 * {@code context.services().get(LiveTestRegistry.class)}.
 * </p>
 * <p>
 * <b>Legacy code:</b> The static methods on this class provide backward-compatible
 * access, delegating to the current context's registry or a global fallback.
 * </p>
 */
public final class LiveTestRegistry {

    private final Map<String, LiveTest> tests = new LinkedHashMap<>();

    /**
     * Creates a new empty registry instance.
     */
    public LiveTestRegistry() {
    }

    /**
     * Registers a live test.
     *
     * @param test the test to register
     * @throws NullPointerException if test is null
     */
    public void add(@NotNull LiveTest test) {
        Objects.requireNonNull(test, "test");
        tests.put(test.name(), test);
    }

    /**
     * Returns all registered live tests.
     *
     * @return an unmodifiable collection of registered tests
     */
    public @NotNull Collection<LiveTest> allTests() {
        return Collections.unmodifiableCollection(tests.values());
    }

    /**
     * Looks up a test by its name.
     *
     * @param name the test name
     * @return the test, or null if not found
     */
    public @Nullable LiveTest find(@NotNull String name) {
        return tests.get(name);
    }

    /**
     * Returns the number of registered tests.
     *
     * @return the test count
     */
    public int testCount() {
        return tests.size();
    }

    /**
     * Removes all registered tests.
     */
    public void removeAll() {
        tests.clear();
    }

    // ── Legacy static API (deprecated) ────────────────────────────────────

    private static @NotNull LiveTestRegistry resolve() {
        return Rapunzel.findContext()
                .flatMap(ctx -> ctx.services().find(LiveTestRegistry.class))
                .orElse(LegacyHolder.INSTANCE);
    }

    /**
     * Registers a live test using the current context's registry.
     *
     * @param test the test to register
     * @deprecated Use registry instance from {@code LiveTestFeatures.registry()}.
     */
    @Deprecated
    public static void register(@NotNull LiveTest test) {
        resolve().add(test);
    }

    /**
     * Returns all registered live tests.
     *
     * @deprecated Use registry instance from {@code LiveTestFeatures.registry()}.
     */
    @Deprecated
    public static @NotNull Collection<LiveTest> all() {
        return resolve().allTests();
    }

    /**
     * Looks up a test by name.
     *
     * @deprecated Use registry instance from {@code LiveTestFeatures.registry()}.
     */
    @Deprecated
    public static @Nullable LiveTest get(@Nullable String name) {
        return resolve().find(name);
    }

    /**
     * Returns the number of registered tests.
     *
     * @deprecated Use registry instance from {@code LiveTestFeatures.registry()}.
     */
    @Deprecated
    public static int size() {
        return resolve().testCount();
    }

    /**
     * Clears all registered tests from the current context.
     *
     * @deprecated Use registry instance from {@code LiveTestFeatures.registry()}.
     */
    @Deprecated
    public static void clear() {
        resolve().removeAll();
    }

    // Legacy fallback for when no context is available
    private static final class LegacyHolder {
        static final LiveTestRegistry INSTANCE = new LiveTestRegistry();
    }
}
