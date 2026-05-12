package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A manifest describing which event types are supported on a given platform.
 *
 * <p>Records the platform ID and a map of event types to their {@link GameEventSupport}
 * entries. The manifest can be built incrementally using {@link Builder} and supports
 * overlay operations to merge support information from multiple sources.</p>
 *
 * @param platformId      the platform this manifest applies to
 * @param supportByEvent  map of event types to their support descriptors
 */
public record GameEventSupportManifest(
    @NotNull PlatformId platformId,
    @NotNull Map<Class<? extends GameEvent>, GameEventSupport> supportByEvent
) {
    public GameEventSupportManifest {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(supportByEvent, "supportByEvent");
        supportByEvent = Map.copyOf(supportByEvent);
    }

    /**
     * Creates an empty support manifest where all event types are unsupported.
     *
     * @param platformId the platform identifier
     * @return an empty manifest
     */
    public static @NotNull GameEventSupportManifest empty(@NotNull PlatformId platformId) {
        return builder(platformId).build();
    }

    /**
     * Creates a new {@link Builder} for the given platform ID.
     *
     * @param platformId the platform identifier
     * @return a new builder
     */
    public static @NotNull Builder builder(@NotNull PlatformId platformId) {
        return new Builder(platformId);
    }

    /**
     * Converts this manifest to a builder pre-populated with its data.
     *
     * @return a builder initialized with this manifest's entries
     */
    public @NotNull Builder toBuilder() {
        return builder(platformId).include(this);
    }

    /**
     * Overlays another manifest onto this one, merging supported event entries.
     *
     * @param manifest the manifest to overlay
     * @return a new manifest with merged support entries
     * @throws IllegalArgumentException if the platform IDs do not match
     */
    public @NotNull GameEventSupportManifest overlay(@NotNull GameEventSupportManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        if (platformId != manifest.platformId()) {
            throw new IllegalArgumentException(
                "Cannot overlay support manifest for " + manifest.platformId() + " onto " + platformId
            );
        }
        return toBuilder().include(manifest).build();
    }

    /**
     * Overlays another manifest, only adding entries for event types that are
     * currently unsupported in this manifest.
     *
     * @param manifest the manifest to overlay
     * @return a new manifest with merged support entries
     * @throws IllegalArgumentException if the platform IDs do not match
     */
    public @NotNull GameEventSupportManifest overlayUnsupported(@NotNull GameEventSupportManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        if (platformId != manifest.platformId()) {
            throw new IllegalArgumentException(
                "Cannot overlay support manifest for " + manifest.platformId() + " onto " + platformId
            );
        }
        return toBuilder().includeUnsupported(manifest).build();
    }

    /**
     * Returns the {@link GameEventSupport} for the given event type.
     *
     * @param eventType the event class
     * @return the support descriptor, or unsupported if not found
     */
    public @NotNull GameEventSupport support(@NotNull Class<? extends GameEvent> eventType) {
        Objects.requireNonNull(eventType, "eventType");
        return supportByEvent.getOrDefault(eventType, GameEventSupport.unsupported(eventType));
    }

    /**
     * Returns whether the given event type is supported.
     *
     * @param eventType the event class
     * @return true if supported
     */
    public boolean supports(@NotNull Class<? extends GameEvent> eventType) {
        return support(eventType).supported();
    }

    /**
     * Builder for constructing a {@link GameEventSupportManifest}.
     *
     * <p>Allows registering native, emulated, partial, and unsupported event types,
     * as well as including entries from existing manifests.</p>
     */
    public static final class Builder {
        private final PlatformId platformId;
        private final Map<Class<? extends GameEvent>, GameEventSupport> supportByEvent = new LinkedHashMap<>();

        private Builder(@NotNull PlatformId platformId) {
            this.platformId = Objects.requireNonNull(platformId, "platformId");
        }

        /**
         * Registers event types as natively supported.
         *
         * @param details    human-readable support details
         * @param eventTypes the event types to register
         * @return this builder
         */
        @SafeVarargs
        public final @NotNull Builder nativeSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.NATIVE, details, eventTypes);
        }

        /**
         * Registers event types as emulated supported.
         *
         * @param details    human-readable support details
         * @param eventTypes the event types to register
         * @return this builder
         */
        @SafeVarargs
        public final @NotNull Builder emulatedSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.EMULATED, details, eventTypes);
        }

        /**
         * Registers event types as partially supported.
         *
         * @param details    human-readable support details
         * @param eventTypes the event types to register
         * @return this builder
         */
        @SafeVarargs
        public final @NotNull Builder partialSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.PARTIAL, details, eventTypes);
        }

        /**
         * Registers event types with the given parity level.
         *
         * @param parity     the support parity level
         * @param details    human-readable support details
         * @param eventTypes the event types to register
         * @return this builder
         */
        @SafeVarargs
        public final @NotNull Builder support(
            @NotNull GameEventSupportParity parity,
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            Objects.requireNonNull(parity, "parity");
            Objects.requireNonNull(details, "details");
            Objects.requireNonNull(eventTypes, "eventTypes");
            for (Class<? extends GameEvent> eventType : eventTypes) {
                supportByEvent.put(eventType, new GameEventSupport(eventType, parity, details));
            }
            return this;
        }

        /**
         * Includes all supported entries from the given manifest.
         *
         * @param manifest the manifest to include entries from
         * @return this builder
         * @throws IllegalArgumentException if the platform IDs do not match
         */
        public @NotNull Builder include(@NotNull GameEventSupportManifest manifest) {
            Objects.requireNonNull(manifest, "manifest");
            if (platformId != manifest.platformId()) {
                throw new IllegalArgumentException(
                    "Cannot include support manifest for " + manifest.platformId() + " into " + platformId
                );
            }
            for (GameEventSupport support : manifest.supportByEvent.values()) {
                if (support.supported()) {
                    supportByEvent.put(support.eventType(), support);
                }
            }
            return this;
        }

        /**
         * Includes supported entries from the given manifest, but only for event types
         * that are not already supported in this builder.
         *
         * @param manifest the manifest to include entries from
         * @return this builder
         * @throws IllegalArgumentException if the platform IDs do not match
         */
        public @NotNull Builder includeUnsupported(@NotNull GameEventSupportManifest manifest) {
            Objects.requireNonNull(manifest, "manifest");
            if (platformId != manifest.platformId()) {
                throw new IllegalArgumentException(
                    "Cannot include support manifest for " + manifest.platformId() + " into " + platformId
                );
            }
            for (GameEventSupport support : manifest.supportByEvent.values()) {
                if (!support.supported()) {
                    continue;
                }
                GameEventSupport existing = supportByEvent.get(support.eventType());
                if (existing == null || !existing.supported()) {
                    supportByEvent.put(support.eventType(), support);
                }
            }
            return this;
        }

        /**
         * Builds the manifest, filling in any missing event types as unsupported.
         *
         * @return the built manifest
         */
        public @NotNull GameEventSupportManifest build() {
            Map<Class<? extends GameEvent>, GameEventSupport> resolved = new LinkedHashMap<>();
            for (Class<? extends GameEvent> eventType : GameEventCatalog.sharedEventTypes()) {
                resolved.put(eventType, supportByEvent.getOrDefault(eventType, GameEventSupport.unsupported(eventType)));
            }
            return new GameEventSupportManifest(platformId, resolved);
        }
    }
}
