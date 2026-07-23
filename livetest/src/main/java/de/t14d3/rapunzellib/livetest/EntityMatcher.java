package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Predicate-style matcher used by {@link Bot#awaitEntity(EntityMatcher, long)}
 * and {@link Bot#queryMatchingEntities(EntityMatcher, long)}.
 *
 * <p>Static factory methods cover the most common queries; tests with custom
 * logic can implement the interface directly.</p>
 */
@FunctionalInterface
public interface EntityMatcher extends Predicate<@NotNull BotEntity> {

    /** Matches when the entity's type is the given name (bare or namespaced). */
    static @NotNull EntityMatcher ofType(@NotNull String typeName) {
        Objects.requireNonNull(typeName, "typeName");
        return e -> e.hasType(typeName);
    }

    /** Matches when the entity's id equals the given value. */
    static @NotNull EntityMatcher withId(int entityId) {
        return e -> e.entityId() == entityId;
    }

    /** Matches when the entity is within the given radius of the target position. */
    static @NotNull EntityMatcher within(double x, double y, double z, double radius) {
        double r2 = radius * radius;
        return e -> {
            double dx = e.x() - x, dy = e.y() - y, dz = e.z() - z;
            return dx * dx + dy * dy + dz * dz <= r2;
        };
    }

    /** Matches when the entity is farther than the given radius from the target. */
    static @NotNull EntityMatcher fartherThan(double x, double y, double z, double radius) {
        double r2 = radius * radius;
        return e -> {
            double dx = e.x() - x, dy = e.y() - y, dz = e.z() - z;
            return dx * dx + dy * dy + dz * dz > r2;
        };
    }

    /** Matches always - useful for "wait for any entity of this type to land". */
    static @NotNull EntityMatcher any() {
        return e -> !e.isUnknown();
    }

    /** Combines this matcher with another; both must pass. */
    default @NotNull EntityMatcher and(@NotNull EntityMatcher other) {
        Objects.requireNonNull(other, "other");
        return e -> test(e) && other.test(e);
    }

    /** Combines this matcher with another; either may pass. */
    default @NotNull EntityMatcher or(@NotNull EntityMatcher other) {
        Objects.requireNonNull(other, "other");
        return e -> test(e) || other.test(e);
    }

    /** Negates this matcher. */
    default @NotNull EntityMatcher negate() {
        return e -> !test(e);
    }
}
