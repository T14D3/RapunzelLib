package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Describes the storage capabilities for attachments on a given target type.
 */
public enum AttachmentStorageSupport {
    UNSUPPORTED(false, false, PersistentSupport.NONE),
    TRANSIENT_ONLY(true, true, PersistentSupport.NONE),
    TRANSIENT_AND_OPTIONAL_PERSISTENT(true, true, PersistentSupport.OPTIONAL),
    TRANSIENT_AND_PERSISTENT(true, true, PersistentSupport.GUARANTEED),
    PERSISTENT_ONLY(true, false, PersistentSupport.GUARANTEED);

    private final boolean supported;
    private final boolean supportsTransient;
    private final PersistentSupport persistentSupport;

    AttachmentStorageSupport(
        boolean supported,
        boolean supportsTransient,
        @NotNull PersistentSupport persistentSupport
    ) {
        this.supported = supported;
        this.supportsTransient = supportsTransient;
        this.persistentSupport = Objects.requireNonNull(persistentSupport, "persistentSupport");
    }

    public boolean supported() {
        return supported;
    }

    /**
     * Checks whether transient (in-memory) storage is supported.
     *
     * @return true if transient storage is supported
     */
    public boolean supportsTransient() {
        return supportsTransient;
    }

    /**
     * Checks whether persistent (disk-backed) storage is supported.
     *
     * @return true if persistent storage is supported
     */
    public boolean supportsPersistent() {
        return persistentSupport.supported();
    }

    /**
     * Checks whether persistent storage is guaranteed.
     *
     * @return true if persistent storage is guaranteed
     */
    public boolean persistentGuaranteed() {
        return persistentSupport == PersistentSupport.GUARANTEED;
    }

    /**
     * Checks whether persistent storage is conditional (optional).
     *
     * @return true if persistent storage is optional
     */
    public boolean persistentConditional() {
        return persistentSupport == PersistentSupport.OPTIONAL;
    }

    /**
     * Returns the persistent support level.
     *
     * @return the persistent support
     */
    public @NotNull PersistentSupport persistentSupport() {
        return persistentSupport;
    }

    /**
     * Checks whether the given scope is supported.
     *
     * @param scope the scope to check
     * @return true if the scope is supported
     */
    public boolean supports(@NotNull RAttachmentScope scope) {
        Objects.requireNonNull(scope, "scope");
        return switch (scope) {
            case TRANSIENT -> supportsTransient;
            case PERSISTENT -> supportsPersistent();
        };
    }

    /**
     * Intersects this storage support with another, yielding the most restrictive combination.
     *
     * @param other the other storage support
     * @return the intersected result
     */
    public @NotNull AttachmentStorageSupport intersect(@NotNull AttachmentStorageSupport other) {
        Objects.requireNonNull(other, "other");
        return of(
            supportsTransient && other.supportsTransient,
            persistentSupport.intersect(other.persistentSupport)
        );
    }

    /**
     * Creates a storage support from boolean flags.
     *
     * @param supportsTransient    whether transient storage is supported
     * @param supportsPersistent   whether persistent storage is supported
     * @return the storage support
     */
    public static @NotNull AttachmentStorageSupport of(boolean supportsTransient, boolean supportsPersistent) {
        return of(supportsTransient, supportsPersistent ? PersistentSupport.GUARANTEED : PersistentSupport.NONE);
    }

    public static @NotNull AttachmentStorageSupport of(
        boolean supportsTransient,
        @NotNull PersistentSupport persistentSupport
    ) {
        Objects.requireNonNull(persistentSupport, "persistentSupport");
        if (supportsTransient) {
            return switch (persistentSupport) {
                case NONE -> TRANSIENT_ONLY;
                case OPTIONAL -> TRANSIENT_AND_OPTIONAL_PERSISTENT;
                case GUARANTEED -> TRANSIENT_AND_PERSISTENT;
            };
        }
        if (persistentSupport.supported()) {
            return PERSISTENT_ONLY;
        }
        return UNSUPPORTED;
    }

    /**
     * The level of persistent storage support.
     */
    public enum PersistentSupport {
        /** No persistent storage. */
        NONE,
        /** Persistent storage may be available. */
        OPTIONAL,
        /** Persistent storage is guaranteed. */
        GUARANTEED;

    /**
     * Checks whether any storage is supported.
     *
     * @return true if storage is supported
     */
    public boolean supported() {
            return this != NONE;
        }

        private @NotNull PersistentSupport intersect(@NotNull PersistentSupport other) {
            Objects.requireNonNull(other, "other");
            if (this == NONE || other == NONE) {
                return NONE;
            }
            if (this == GUARANTEED && other == GUARANTEED) {
                return GUARANTEED;
            }
            return OPTIONAL;
        }
    }
}
