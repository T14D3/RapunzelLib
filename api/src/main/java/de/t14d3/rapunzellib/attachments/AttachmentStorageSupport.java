package de.t14d3.rapunzellib.attachments;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    public boolean supportsTransient() {
        return supportsTransient;
    }

    public boolean supportsPersistent() {
        return persistentSupport.supported();
    }

    public boolean persistentGuaranteed() {
        return persistentSupport == PersistentSupport.GUARANTEED;
    }

    public boolean persistentConditional() {
        return persistentSupport == PersistentSupport.OPTIONAL;
    }

    public @NotNull PersistentSupport persistentSupport() {
        return persistentSupport;
    }

    public boolean supports(@NotNull RAttachmentScope scope) {
        Objects.requireNonNull(scope, "scope");
        return switch (scope) {
            case TRANSIENT -> supportsTransient;
            case PERSISTENT -> supportsPersistent();
        };
    }

    public @NotNull AttachmentStorageSupport intersect(@NotNull AttachmentStorageSupport other) {
        Objects.requireNonNull(other, "other");
        return of(
            supportsTransient && other.supportsTransient,
            persistentSupport.intersect(other.persistentSupport)
        );
    }

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

    public enum PersistentSupport {
        NONE,
        OPTIONAL,
        GUARANTEED;

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
